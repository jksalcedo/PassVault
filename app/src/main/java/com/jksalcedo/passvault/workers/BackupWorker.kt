package com.jksalcedo.passvault.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jksalcedo.passvault.repositories.PasswordRepository
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.utils.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val passwordRepository: PasswordRepository,
    private val preferenceRepository: PreferenceRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "BackupWorker"
    }

    @Suppress("unused")
    constructor(appContext: Context, workerParams: WorkerParameters) : this(
        appContext,
        workerParams,
        PasswordRepository(appContext),
        PreferenceRepository(appContext)
    )

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Auto backup started.")

                //Get all password entries
                val entries = passwordRepository.getAllEntries()
                if (entries.isEmpty()) {
                    Log.d(TAG, "No data to back up. Skipping.")
                    return@withContext Result.success()
                }

                val format = preferenceRepository.getExportFormat()
                // val encrypt = preferenceRepository.getEncryptBackups()

                // Serialize data
                val data = when (format.uppercase()) {
                    "JSON" -> Utility.serializeEntries(entries, format = format)
                    "CSV" -> Utility.serializeEntries(entries, format = format)
                    else -> Utility.serializeEntries(entries, "json") // Default to JSON
                }

                // Create the backup file
                val backupsDir = File(applicationContext.getExternalFilesDir(null), "backups")
                if (!backupsDir.exists()) {
                    backupsDir.mkdirs()
                }
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "passvault_backup_$timestamp.${format.lowercase()}"
                val backupFile = File(backupsDir, fileName)

                // Write data to the file
                backupFile.writeText(data)

                preferenceRepository.updateLastBackupTime()

                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure()
        }
    }
}