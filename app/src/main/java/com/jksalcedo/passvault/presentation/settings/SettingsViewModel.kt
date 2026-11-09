package com.jksalcedo.passvault.presentation.settings

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jksalcedo.passvault.core.backup.BackupScheduler
import com.jksalcedo.passvault.core.backup.BackupStorage
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.core.util.Utility.toImportRecord
import com.jksalcedo.passvault.core.util.Utility.toPasswordEntry
import com.jksalcedo.passvault.data.enums.ImportType
import com.jksalcedo.passvault.data.importer.BitwardenImporter
import com.jksalcedo.passvault.data.importer.ChromeImporter
import com.jksalcedo.passvault.data.importer.KeePassImporter
import com.jksalcedo.passvault.data.local.entity.Category
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.BackupItem
import com.jksalcedo.passvault.data.model.ImportRecord
import com.jksalcedo.passvault.data.model.ImportResult
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Locale
import com.jksalcedo.passvault.PassVaultApplication as PassVaultApp

/**
 * ViewModel for settings.
 * @param application The application.
 */
open class SettingsViewModel(
    application: Application,
) :
    AndroidViewModel(application) {

    private val container = (application as PassVaultApp).container
    private val prefsRepository: PreferenceRepository = container.preferenceRepository
    private val passwordRepository = container.passwordRepository
    private val categoryRepository = container.categoryRepository

    private val _exportUiState =
        MutableLiveData<ExportUiState>(ExportUiState.Idle)
    val exportUiState: LiveData<ExportUiState> = _exportUiState

    private val _importResult = MutableLiveData<Result<Int>>()
    val importResult: LiveData<Result<Int>> = _importResult

    private val _importUiState = MutableLiveData<ImportUiState>(ImportUiState.Idle)
    val importUiState: LiveData<ImportUiState> = _importUiState

    private val _restartAppEvent = MutableLiveData<Unit?>()
    val restartAppEvent: LiveData<Unit?> = _restartAppEvent

    private val _keystoreValidationResult = MutableLiveData<Boolean?>()
    val keystoreValidationResult: LiveData<Boolean?> = _keystoreValidationResult

    private val _backups = MutableLiveData<List<BackupItem>>(emptyList())
    val backups: LiveData<List<BackupItem>> = _backups

    private val _copyResult = MutableLiveData<Boolean?>()
    val copyResult: LiveData<Boolean?> = _copyResult

    private val _deleteResult = MutableLiveData<Boolean?>()
    val deleteResult: LiveData<Boolean?> = _deleteResult

    /**
     * The backup selected for "Save and Copy". Kept in the ViewModel so it survives
     * fragment re-creation while the document picker is open.
     */
    var pendingCopyItem: BackupItem? = null


    /**
     * Resets the import UI state to Idle.
     */
    fun resetImportState() {
        _importUiState.value = ImportUiState.Idle
    }

    fun resetExportState() {
        _exportUiState.value = ExportUiState.Idle
    }

    /**
     * Enables or disables automatic backups.
     * @param enabled True to enable, false to disable.
     */
    fun setAutoBackups(enabled: Boolean) {
        prefsRepository.setAutoBackups(enabled)
        if (enabled) {
            BackupScheduler.reschedule(getApplication(), prefsRepository)
        } else {
            BackupScheduler.cancel(getApplication())
        }
    }

    /**
     * Gets the app version name.
     * @return The app version name.
     */
    fun getAppVersion(): String {
        return try {
            val packageName = getApplication<Application>().packageName
            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getApplication<Application>().packageManager.getPackageInfo(packageName, 0)
                } else {
                    @Suppress("DEPRECATION")
                    getApplication<Application>().packageManager.getPackageInfo(packageName, 0)
                }
            "PassVault ${packageInfo.versionName ?: "Unknown"}"
        } catch (_: Exception) {
            "N/A"
        }
    }

    /**
     * Gets the storage info.
     * @return A pair of longs representing the database size and preferences size.
     */
    fun getStorageInfo(): Pair<Long, Long> {
        val dbSize = Utility.getDatabaseSize(getApplication(), "passvault_db")
        val prefsSize = getSharedPreferencesSize()
        return Pair(dbSize, prefsSize)
    }

    /**
     * Clears all app data.
     */
    fun clearAllData() {
        BackupScheduler.cancel(getApplication())
        viewModelScope.launch(Dispatchers.IO) {
            val dbFile = getApplication<Application>().getDatabasePath("passvault_db")
            if (dbFile.exists()) {
                dbFile.delete()
            }
            prefsRepository.clear()

            val prefsDir =
                File(getApplication<Application>().applicationInfo.dataDir, "shared_prefs")
            prefsDir.listFiles()?.forEach { it.delete() }
            // Restart to reflect changes
            _restartAppEvent.postValue(Unit)
            _restartAppEvent.value = null
        }
    }

    /**
     * Validates the Android Keystore before export.
     * @return True if the keystore is valid, false otherwise.
     */
    fun validateKeystoreBeforeExport(): Boolean {
        val isValid = Encryption.isKeystoreValid()
        _keystoreValidationResult.postValue(isValid)
        return isValid
    }

    /**
     * Resets the keystore validation result.
     */
    fun resetKeystoreValidation() {
        _keystoreValidationResult.value = null
    }

    /**
     * Retrieves all non-deleted entries for printing.
     */
    suspend fun getAllEntriesForPrint(): List<PasswordEntry> {
        return withContext(Dispatchers.IO) {
            passwordRepository.getAllEntries()
        }
    }

    /**
     * Exports entries to a file.
     * @param uri The URI of the file to export to.
     * @param password The password to encrypt the file with.
     */
    fun exportEntries(uri: Uri, password: String?) {
        val exportFormat = prefsRepository.getExportFormat().lowercase(Locale.ROOT)
        val isEncryptionEnabled = prefsRepository.getEncryptBackups()

        if (password.isNullOrEmpty() && (isEncryptionEnabled || exportFormat == "kdbx")) {
            _exportUiState.postValue(
                ExportUiState.Error(
                    Exception("Password not found.")
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _exportUiState.postValue(
                ExportUiState.Loading(
                    0,
                    0
                )
            )
            try {
                // Validate keystore before export
                if (!Encryption.isKeystoreValid()) {
                    _exportUiState.postValue(
                        ExportUiState.Error(
                            Exception("Android Keystore key is invalid. Some or all passwords cannot be decrypted.")
                        )
                    )
                    return@launch
                }

                // Fetch entries from the database
                val entries = withContext(Dispatchers.IO) {
                    passwordRepository.getAllEntries()
                }

                if (exportFormat == "kdbx") {
                    val (kdbxBytes, exportResult) = com.jksalcedo.passvault.data.exporter.KeePassExporter.export(
                        entries,
                        password ?: ""
                    )
                    saveBytesToFile(kdbxBytes, uri)
                    _exportUiState.postValue(
                        ExportUiState.Success(
                            exportResult
                        )
                    )
                } else {
                    val exportResult =
                        Utility.serializeEntries(
                            entries,
                            exportFormat
                        ) { progress, total ->
                            _exportUiState.postValue(
                                ExportUiState.Loading(
                                    progress,
                                    total
                                )
                            )
                        }

                    val contentToWrite = if (isEncryptionEnabled) {
                        Encryption.encryptFileContentArgon(
                            exportResult.serializedData,
                            password = password!!.toByteArray()
                        )
                    } else {
                        exportResult.serializedData
                    }

                    saveToFile(contentToWrite, uri)
                    _exportUiState.postValue(
                        ExportUiState.Success(
                            exportResult
                        )
                    )
                }
            } catch (e: Exception) {
                _exportUiState.postValue(ExportUiState.Error(e))
            }
        }
    }

    /**
     * Imports entries from a file.
     * @param uri The URI of the file to import from.
     * @param password The password to decrypt the file with.
     * @param formatOverride The format of the file to import from.
     */
    fun importEntries(uri: Uri, password: String, formatOverride: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _importUiState.postValue(ImportUiState.Loading(0, 0))

                // Read the raw file content
                val fileContent = readFromFile(uri)

                // Determine if we need to decrypt or use raw content
                val contentToParse = if (password.isNotEmpty()) {
                    try {
                        attemptDecryption(fileContent, password)
                    } catch (e: Exception) {
                        throw Exception(
                            "Decryption failed. Wrong password or not an encrypted file.",
                            e
                        )
                    }
                } else {
                    fileContent
                }

                if (contentToParse.isBlank()) {
                    throw Exception("No entries found or invalid file format.")
                }

                //Deserialize
                val format = formatOverride ?: prefsRepository.getExportFormat()
                val entries = if (format.equals("csv", ignoreCase = true)) {
                    parseCsvWithFallback(contentToParse)
                } else {
                    Utility.deserializeEntries(contentToParse, format)
                }

                if (entries.isEmpty()) {
                    throw Exception("No entries found or invalid file format.")
                }

                // Insert all into Database
                @Suppress("DEPRECATION") val results = importVault(entries.map { entry ->
                    entry.toImportRecord()
                }) { progress, total ->
                    _importUiState.postValue(ImportUiState.Loading(progress, total))
                }

                _importUiState.postValue(
                    ImportUiState.Success(
                        results.count { it.isSuccess },
                        results
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _importUiState.postValue(ImportUiState.Error(e))
            }
        }
    }

    /**
     * Parses a CSV backup. PassVault CSV is tried first, then Chrome CSV as a fallback
     * so CSV backups created by either flow can be restored.
     */
    private suspend fun parseCsvWithFallback(content: String): List<PasswordEntry> {
        val passVaultEntries = runCatching {
            Utility.deserializeEntries(content, "csv")
        }.getOrDefault(emptyList())

        if (passVaultEntries.isNotEmpty()) return passVaultEntries

        return ChromeImporter().parse(content).map { it.toPasswordEntry() }
    }

    /**
     * Attempts to decrypt the given content with the given password.
     * @param content The content to decrypt.
     * @param password The password to decrypt with.
     * @return The decrypted content.
     */
    private fun attemptDecryption(content: String, password: String): String {
        // Try the new method first (Argon2)
        try {
            return Encryption.decryptFileContentArgon(content, password.toByteArray())
        } catch (_: Exception) {
            // This is an old file
            // Ignore
        }

        // Old method
        try {
            @Suppress("DEPRECATION")
            return Encryption.decryptFileContent(content, password)
        } catch (_: Exception) {
            // Both failed, the password is wrong or the file is corrupt
            throw Exception("Decryption failed. Invalid password or unknown format.")
        }
    }

    /**
     * Saves the given content to the given URI.
     * @param content The content to save.
     * @param uri The URI to save to.
     */
    private suspend fun saveToFile(content: String, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    stream.write(content.toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private suspend fun saveBytesToFile(bytes: ByteArray, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    stream.write(bytes)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * Reads the content of the given URI.
     * @param uri The URI to read from.
     * @return The content of the URI.
     */
    suspend fun readFromFile(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: ""
        } catch (e: Exception) {
            throw Exception("Failed to read file.", e)
        }
    }

    /**
     * Gets the size of the shared preferences.
     * @return The size of the shared preferences.
     */
    private fun getSharedPreferencesSize(): Long {
        val prefsDir = File(getApplication<Application>().applicationInfo.dataDir, "shared_prefs")
        if (!prefsDir.exists()) return 0L

        return prefsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Refreshes the list of backups from the configured location.
     */
    fun refreshBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            _backups.postValue(getBackups())
        }
    }

    /**
     * Gets the list of backups from the configured location.
     * @return The list of backup items.
     */
    private fun getBackups(): List<BackupItem> {
        return BackupStorage.listBackups(getApplication(), prefsRepository)
    }

    /**
     * Copies a backup file to a user-selected path.
     * @param backupItem The backup item to copy.
     * @param targetUri The target URI to copy to.
     */
    fun copyBackupToUri(backupItem: BackupItem, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = try {
                val resolver = getApplication<Application>().contentResolver
                val inputStream = resolver.openInputStream(backupItem.uri)
                val outputStream = resolver.openOutputStream(targetUri)

                if (inputStream == null || outputStream == null) {
                    inputStream?.close()
                    outputStream?.close()
                    false
                } else {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to copy backup ${backupItem.name}", e)
                false
            }
            _copyResult.postValue(success)
        }
    }

    /**
     * Copies the backup selected before the document picker was opened.
     */
    fun copyPendingBackupToUri(targetUri: Uri) {
        val item = pendingCopyItem
        pendingCopyItem = null
        if (item == null) {
            _copyResult.postValue(false)
            return
        }
        copyBackupToUri(item, targetUri)
    }

    fun resetCopyResult() {
        _copyResult.value = null
    }

    /**
     * Deletes a backup file in the background and refreshes the list on success.
     * @param backupItem The backup item to delete.
     */
    fun deleteBackup(backupItem: BackupItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = BackupStorage.deleteBackup(getApplication(), backupItem)
            if (success) {
                _backups.postValue(getBackups())
            }
            _deleteResult.postValue(success)
        }
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }

    /**
     * Restores a backup from the given item.
     *
     * The import type is inferred from the file extension: automatic backups are always
     * JSON, but legacy CSV, Chrome CSV and KDBX files remain restorable.
     * @param backupItem The backup item to restore.
     * @param password The password if encrypted.
     */
    fun restoreBackup(backupItem: BackupItem, password: String) {
        when (backupItem.name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "kdbx" -> startImport(backupItem.uri, ImportType.KEEPASS_KDBX, password)
            "chrome_csv" -> importEntries(backupItem.uri, password, formatOverride = "chrome_csv")
            "csv" -> importEntries(backupItem.uri, password, formatOverride = "csv")
            else -> importEntries(backupItem.uri, password, formatOverride = "json")
        }
    }

    /**
     * Imports a vault.
     * @param entries The entries to import.
     * @return A list of [ImportResult].
     */
    suspend fun importVault(
        entries: List<ImportRecord>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<ImportResult> {
        val results = mutableListOf<ImportResult>()
        withContext(Dispatchers.IO) {
            // Check for and create missing categories first
            val existingCategories =
                categoryRepository.getAllCategoriesSync().map { it.name }.toSet()
            val importedCategories =
                entries.mapNotNull { it.category }.filter { it.isNotBlank() }.toSet()

            val colors = listOf(
                "#F44336", "#E91E63", "#9C27B0", "#673AB7",
                "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
                "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
                "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
                "#795548", "#9E9E9E", "#607D8B"
            )

            val missingCategories = importedCategories - existingCategories
            if (missingCategories.isNotEmpty()) {
                val newCategories = missingCategories.map { categoryName ->
                    Category(
                        name = categoryName,
                        colorHex = colors.random()
                    )
                }
                categoryRepository.insertAll(newCategories)
            }

            // Insert password entries
            entries.forEachIndexed { index, importRecord ->
                onProgress?.invoke(index + 1, entries.size)
                try {
                    val entry = importRecord.toPasswordEntry()
                    passwordRepository.insert(entry)
                    results.add(
                        ImportResult(
                            title = importRecord.title,
                            isSuccess = true,
                        )
                    )
                } catch (e: Exception) {
                    results.add(
                        ImportResult(
                            title = importRecord.title,
                            isSuccess = false,
                            errorMessage = e.message
                        )
                    )
                }
            }
        }
        return results
    }

    /**
     * Starts the import process.
     * @param uri The URI of the file to import.
     * @param type The type of import.
     * @param password The password for the import.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun startImport(uri: Uri, type: ImportType, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importUiState.postValue(ImportUiState.Loading(0, 0))
            try {
                if (type == ImportType.PASSVAULT_JSON) {
                    importEntries(uri, password, formatOverride = "json")
                    return@launch
                }

                if (type == ImportType.PASSVAULT_CSV) {
                    importEntries(uri, password, formatOverride = "csv")
                    return@launch
                }

                if (type == ImportType.CHROME_CSV) {
                    importEntries(uri, password, formatOverride = "chrome_csv")
                    return@launch
                }

                val importer = when (type) {
                    ImportType.KEEPASS_CSV -> KeePassImporter(
                        type = type,
                        password = password,
                        filePath = uri,
                        context = application
                    )

                    ImportType.KEEPASS_KDBX -> KeePassImporter(
                        type = type,
                        password = password,
                        filePath = uri,
                        context = application
                    )

                    ImportType.BITWARDEN_JSON -> BitwardenImporter()
                    else -> throw IllegalArgumentException("Unsupported import type")
                }
                val content = readFromFile(uri)
                val entries = importer.parse(content)
                val results = importVault(entries) { progress, total ->
                    _importUiState.postValue(ImportUiState.Loading(progress, total))
                }

                _importUiState.postValue(
                    ImportUiState.Success(
                        results.count { it.isSuccess },
                        results
                    )
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Import failed", e)
                _importUiState.postValue(ImportUiState.Error(Exception(getFriendlyErrorMessage(e))))
            }
        }
    }

    /**
     * Gets a friendly error message for the given throwable.
     * @param e The throwable.
     * @return The friendly error message.
     */
    private fun getFriendlyErrorMessage(e: Throwable): String {
        return when (e) {
            is FileNotFoundException -> "File not found or inaccessible."
            is IllegalArgumentException -> {
                if (e.message?.contains("Unsupported import type") == true) {
                    "Unsupported import type selected."
                } else {
                    "An error occurred with import settings."
                }
            }

            is org.json.JSONException -> {
                "Invalid file format. Please check if the file is correctly formatted (JSON/CSV)."
            }

            is Exception -> {
                when {
                    e.message?.contains("Decryption failed. Wrong password or not an encrypted file.") == true -> "Decryption failed. Incorrect password or not an encrypted file."
                    e.message?.contains("Invalid KeePass file or incorrect password.") == true -> "Invalid KeePass file or incorrect password."
                    e.message?.contains("No entries found or invalid file format.") == true -> "No entries found in the file or invalid file format."
                    else -> "An unexpected error occurred during import: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                }
            }

            else -> "An unexpected error occurred: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        }
    }
}

/**
 * Factory for creating [SettingsViewModel] instances.
 * @param application The application.
 */
@Suppress("UNCHECKED_CAST")
class SettingsModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return SettingsViewModel(application) as T
    }
}