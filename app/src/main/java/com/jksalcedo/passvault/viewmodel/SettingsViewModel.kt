package com.jksalcedo.passvault.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jksalcedo.passvault.adapter.BackupAdapter
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.AppDatabase
import com.jksalcedo.passvault.data.BackupItem
import com.jksalcedo.passvault.data.Category
import com.jksalcedo.passvault.data.ImportRecord
import com.jksalcedo.passvault.data.ImportResult
import com.jksalcedo.passvault.data.enums.ImportType
import com.jksalcedo.passvault.importer.BitwardenImporter
import com.jksalcedo.passvault.importer.ChromeImporter
import com.jksalcedo.passvault.importer.KeePassImporter
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.settings.ExportUiState
import com.jksalcedo.passvault.ui.settings.ImportUiState
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.utils.Utility.toImportRecord
import com.jksalcedo.passvault.utils.Utility.toPasswordEntry
import com.jksalcedo.passvault.workers.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.Locale
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * ViewModel for settings.
 * @param application The application.
 * @param adapter The backup adapter.
 */
open class SettingsViewModel(
    application: Application,
    private val adapter: BackupAdapter? = null,
) :
    AndroidViewModel(application) {

    private val prefsRepository: PreferenceRepository = PreferenceRepository(application)

    private val workManager by lazy { WorkManager.getInstance(application.applicationContext) }
    private val passwordDao = AppDatabase.getDatabase(application).passwordDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()

    private val _exportUiState =
        MutableLiveData<com.jksalcedo.passvault.ui.settings.ExportUiState>(com.jksalcedo.passvault.ui.settings.ExportUiState.Idle)
    val exportUiState: LiveData<com.jksalcedo.passvault.ui.settings.ExportUiState> = _exportUiState

    private val _importResult = MutableLiveData<Result<Int>>()
    val importResult: LiveData<Result<Int>> = _importResult

    private val _importUiState = MutableLiveData<ImportUiState>(ImportUiState.Idle)
    val importUiState: LiveData<ImportUiState> = _importUiState

    private val _restartAppEvent = MutableLiveData<Unit?>()
    val restartAppEvent: LiveData<Unit?> = _restartAppEvent

    private val _keystoreValidationResult = MutableLiveData<Boolean?>()
    val keystoreValidationResult: LiveData<Boolean?> = _keystoreValidationResult


    companion object {
        private const val AUTO_BACKUP_WORK_TAG = "auto_backup_work"
    }

    /**
     * Resets the import UI state to Idle.
     */
    fun resetImportState() {
        _importUiState.value = ImportUiState.Idle
    }

    fun resetExportState() {
        _exportUiState.value = com.jksalcedo.passvault.ui.settings.ExportUiState.Idle
    }

    /**
     * Enables or disables automatic backups.
     * @param enabled True to enable, false to disable.
     */
    fun setAutoBackups(enabled: Boolean) {
        prefsRepository.setAutoBackups(enabled)
        if (enabled) {
            scheduleAutoBackup()
        } else {
            cancelAutoBackup()
        }
    }

    /**
     * Schedules a periodic work request for automatic backups.
     */
    private fun scheduleAutoBackup() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Offline
            .setRequiresBatteryNotLow(true)
            .build()

        // Create a request that runs once a day
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag(AUTO_BACKUP_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE, // Replace if existing
            backupRequest
        )
    }

    /**
     * Cancels the automatic backup work request.
     */
    private fun cancelAutoBackup() {
        workManager.cancelUniqueWork(AUTO_BACKUP_WORK_TAG)
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
                    passwordDao.getAllEntries()
                }

                if (exportFormat == "kdbx") {
                    val (kdbxBytes, exportResult) = com.jksalcedo.passvault.exporter.KeePassExporter.export(entries, password ?: "")
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
                val entries =
                    Utility.deserializeEntries(contentToParse, format)

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
     * Gets the internal backups.
     * @return The list of internal backups.
     */
    /**
     * Gets the list of backups from the configured location.
     * @return The list of backup items.
     */
    fun getBackups(): List<BackupItem> {
        val backupLocationUri = prefsRepository.getBackupLocation()
        val backupItems = mutableListOf<BackupItem>()

        val filenameFormat = prefsRepository.getBackupFileNameFormat()
        // Create a regex to match files generated by the pattern
        // Escape the pattern first, then replace the escaped {timestamp} with .*
        val patternRegex = Regex.escape(filenameFormat).replace("\\{timestamp\\}", ".*").toRegex()

        if (backupLocationUri != null) {
            // SAF Location
            try {
                val treeUri = backupLocationUri.toUri()
                val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                    getApplication(),
                    treeUri
                )

                pickedDir?.listFiles()?.forEach { file ->
                    if (file.name != null && patternRegex.matches(file.name!!)) {
                        backupItems.add(
                            BackupItem(
                                name = file.name ?: "Unknown",
                                uri = file.uri,
                                lastModified = file.lastModified(),
                                size = file.length(),
                                isSaf = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Internal Storage
            val backupsDir =
                File(getApplication<Application>().getExternalFilesDir(null), "backups")
            if (backupsDir.exists() && backupsDir.isDirectory) {
                backupsDir.listFiles()?.forEach { file ->
                    if (patternRegex.matches(file.name)) {
                        backupItems.add(
                            BackupItem(
                                name = file.name,
                                uri = file.toUri(),
                                lastModified = file.lastModified(),
                                size = file.length(),
                                isSaf = false
                            )
                        )
                    }
                }
            }
        }
        return backupItems.sortedByDescending { it.lastModified }
    }

    /**
     * Copies a backup file to a user-selected path.
     * @param backupItem The backup item to copy.
     * @param targetUri The target URI to copy to.
     */
    fun copyBackupToUri(backupItem: BackupItem, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream =
                    getApplication<Application>().contentResolver.openInputStream(backupItem.uri)
                val outputStream =
                    getApplication<Application>().contentResolver.openOutputStream(targetUri)

                if (inputStream != null && outputStream != null) {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Deletes a backup file.
     * @param backupItem The backup item to delete.
     * @return True if the backup file was deleted successfully, false otherwise.
     */
    fun deleteBackup(backupItem: BackupItem): Boolean {
        return try {
            if (backupItem.isSaf) {
                val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(
                    getApplication(),
                    backupItem.uri
                )
                val deleted = documentFile?.delete() ?: false
                if (deleted) {
                    adapter?.deleteBackup(backupItem)
                }
                deleted
            } else {
                val file = File(
                    getApplication<Application>().getExternalFilesDir(null),
                    "backups/${backupItem.name}"
                )
                if (file.exists() && file.delete()) {
                    adapter?.deleteBackup(backupItem)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restores a backup from the given item.
     * @param backupItem The backup item to restore.
     * @param password The password if encrypted.
     */
    fun restoreBackup(backupItem: BackupItem, password: String) {
        startImport(backupItem.uri, ImportType.PASSVAULT_JSON, password)
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
            val existingCategories = categoryDao.getAllCategoriesSync().map { it.name }.toSet()
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
                categoryDao.insertAll(newCategories)
            }

            // Insert password entries
            entries.forEachIndexed { index, importRecord ->
                onProgress?.invoke(index + 1, entries.size)
                try {
                    val entry = importRecord.toPasswordEntry()
                    passwordDao.insert(entry)
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
                    ImportType.CHROME_CSV -> ChromeImporter()
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