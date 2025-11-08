package com.jksalcedo.passvault.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.AppDatabase
import com.jksalcedo.passvault.ui.auth.UnlockActivity
import com.jksalcedo.passvault.utils.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.crypto.BadPaddingException

class SettingsViewModel(application: Application, private val context: Activity) :
    AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val passwordDao = AppDatabase.getDatabase(application).passwordDao()

    private val _exportResult = MutableLiveData<Result<Unit>>()
    val exportResult: LiveData<Result<Unit>> = _exportResult

    private val _importResult = MutableLiveData<Result<Int>>()
    val importResult: LiveData<Result<Int>> = _importResult

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

    fun setExportFormat(format: String) {
        prefs.edit { putString("export_format", format) }
    }

    fun getExportFormat(): String {
        return prefs.getString("export_format", "json") ?: "json"
    }

    fun setAutoBackups(enabled: Boolean) {
        prefs.edit { putBoolean("auto_backups", enabled) }
        if (enabled) {
            updateLastBackupTime()
        }
    }

    fun getAutoBackups(): Boolean {
        return prefs.getBoolean("auto_backups", false)
    }

    fun updateLastBackupTime() {
        prefs.edit { putLong("last_backup_time", System.currentTimeMillis()) }
    }

    fun getLastBackupTime(): Long {
        return prefs.getLong("last_backup_time", 0L)
    }

    fun getStorageInfo(): Pair<Long, Long> {
        val dbSize = Utility.getDatabaseSize(getApplication(), "passvault_db")
        val prefsSize = getSharedPreferencesSize()
        return Pair(dbSize, prefsSize)
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbFile = getApplication<Application>().getDatabasePath("passvault_db")
            if (dbFile.exists()) {
                dbFile.delete()
            }
            prefs.edit { clear() }

            val prefsDir =
                File(getApplication<Application>().applicationInfo.dataDir, "shared_prefs")
            prefsDir.listFiles()?.forEach { it.delete() }
            // Restart to reflect changes
            triggerRestart(context = context)
        }
    }

    fun triggerRestart(context: Activity) {
        val intent = Intent(context, UnlockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        context.finish()
        kotlin.system.exitProcess(0)
    }

    fun setRequireAuthForExport(enabled: Boolean) {
        prefs.edit { putBoolean("require_auth_export", enabled) }
    }

    fun getRequireAuthForExport(): Boolean {
        return prefs.getBoolean("require_auth_export", true)
    }

    fun setEncryptBackups(enabled: Boolean) {
        prefs.edit { putBoolean("encrypt_backups", enabled) }
    }

    fun getEncryptBackups(): Boolean {
        return prefs.getBoolean("encrypt_backups", true)
    }

    fun exportEntries(uri: Uri) {
        val passkey = prefs.getString("passkey", null)
        if (passkey.isNullOrEmpty()) {
            _exportResult.postValue(Result.failure(Exception("Passkey not found.")))
            return
        }

        viewModelScope.launch {
            try {
                // Fetch entries from the database
                val entries = withContext(Dispatchers.IO) {
                    passwordDao.getAllEntries()
                }

                val serializedData = Utility.serializeEntries(entries, getExportFormat())
                val isEncryptionEnabled = getEncryptBackups()

                val contentToWrite = if (isEncryptionEnabled) {
                    Encryption.encryptFileContent(serializedData, passkey)
                } else {
                    serializedData
                }

                saveToFile(contentToWrite, uri)
                _exportResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _exportResult.postValue(Result.failure(e))
            }
        }
    }

    fun importEntries(uri: Uri) {
        val passkey = prefs.getString("passkey", null)
        if (passkey.isNullOrEmpty()) {
            _importResult.postValue(Result.failure(Exception("Passkey not found.")))
            return
        }

        viewModelScope.launch {
            try {
                val fileContent = readFromFile(uri)
                val isEncryptionEnabled = getEncryptBackups()

                val decryptedJson = if (isEncryptionEnabled) {
                    try {
                        Encryption.decryptFileContent(fileContent, passkey)
                    } catch (e: BadPaddingException) {
                        throw Exception("Invalid passkey or corrupt file.", e)
                    }
                } else {
                    fileContent
                }

                val entries = Utility.deserializeEntries(decryptedJson, getExportFormat())
                // Insert on IO thread
                withContext(Dispatchers.IO) {
                    entries.forEach { passwordDao.insert(it) }
                }
                _importResult.postValue(Result.success(entries.size))
            } catch (e: Exception) {
                _importResult.postValue(Result.failure(e))
            }
        }
    }

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

    private suspend fun readFromFile(uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to read file.", e)
        }
        stringBuilder.toString()
    }

    private fun getSharedPreferencesSize(): Long {
        val prefsDir = File(getApplication<Application>().applicationInfo.dataDir, "shared_prefs")
        if (!prefsDir.exists()) return 0L

        return prefsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}

@Suppress("UNCHECKED_CAST")
class SettingsModelFactory(
    private val application: Application,
    private val activity: Activity
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return SettingsViewModel(application, activity) as T
    }
}