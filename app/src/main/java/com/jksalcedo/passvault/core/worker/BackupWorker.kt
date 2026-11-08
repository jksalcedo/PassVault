package com.jksalcedo.passvault.core.worker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A worker for creating backups.
 * @param appContext The application context.
 * @param workerParams The worker parameters.
 * @param passwordRepository The password repository.
 * @param preferenceRepository The preference repository.
 */
class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val passwordRepository: PasswordRepository,
    private val preferenceRepository: PreferenceRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "BackupWorker"
    }

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
                val encryptionEnabled = preferenceRepository.getEncryptBackups()
                val password = preferenceRepository.getPasswordForAutoBackups()

                // Serialize data
                val exportResult = when (format.uppercase()) {
                    "JSON" -> Utility.serializeEntries(entries, format = format)
                    "CSV" -> Utility.serializeEntries(entries, format = format)
                    else -> Utility.serializeEntries(entries, "json") // Default to JSON
                }

                // Log if there were any failures during serialization
                if (exportResult.hasFailures) {
                    Log.w(
                        TAG,
                        "Backup: ${exportResult.failedEntries.size} entries failed to export: ${exportResult.failedEntries.joinToString()}"
                    )
                }

                val contentToWrite = if (encryptionEnabled) {
                    if (password.isNullOrEmpty()) {
                        Log.e(
                            TAG,
                            "Encryption is enabled but password is missing. Skipping backup."
                        )
                        // Return failure
                        return@withContext Result.failure()
                    }
                    Encryption.encryptFileContentArgon(
                        exportResult.serializedData,
                        password.toByteArray()
                    )
                } else {
                    exportResult.serializedData
                }

                // Create the backup file(s)
                val timestampFormat = preferenceRepository.getBackupTimestampFormat()
                val filenameFormat = preferenceRepository.getBackupFileNameFormat()

                val timestamp =
                    SimpleDateFormat(timestampFormat, Locale.getDefault()).format(Date())
                val copiesToCreate = preferenceRepository.getBackupCopies()
                val backupLocationUri = preferenceRepository.getBackupLocation()

                var successCount = 0
                for (copyNum in 1..copiesToCreate) {
                    val baseFileName = filenameFormat.replace("{timestamp}", timestamp)
                    val fileName = if (copiesToCreate == 1) {
                        "$baseFileName.${format.lowercase()}"
                    } else {
                        "${baseFileName}_copy$copyNum.${format.lowercase()}"
                    }

                    val success = if (backupLocationUri != null) {
                        // Use custom location (SAF)
                        try {
                            val treeUri = backupLocationUri.toUri()
                            val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                                applicationContext,
                                treeUri
                            )

                            if (pickedDir != null && pickedDir.canWrite()) {
                                val newFile =
                                    pickedDir.createFile("application/octet-stream", fileName)
                                if (newFile != null) {
                                    applicationContext.contentResolver.openOutputStream(newFile.uri)
                                        ?.use { outputStream ->
                                            outputStream.write(contentToWrite.toByteArray())
                                        }
                                    Log.d(
                                        TAG,
                                        "Auto backup copy $copyNum successful: ${newFile.uri}"
                                    )
                                    true
                                } else {
                                    Log.e(TAG, "Failed to create file copy $copyNum")
                                    false
                                }
                            } else {
                                Log.e(TAG, "Custom backup location is not accessible or writable")
                                false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error writing backup copy $copyNum", e)
                            false
                        }
                    } else {
                        // Use default internal storage
                        val backupsDir =
                            File(applicationContext.getExternalFilesDir(null), "backups")
                        if (!backupsDir.exists()) {
                            backupsDir.mkdirs()
                        }
                        val backupFile = File(backupsDir, fileName)
                        backupFile.writeText(contentToWrite)
                        Log.d(TAG, "Auto backup copy $copyNum successful: $fileName")
                        true
                    }

                    if (success) successCount++
                }

                if (successCount > 0) {
                    Log.d(TAG, "Created $successCount/$copiesToCreate backup copies")
                    preferenceRepository.updateLastBackupTime()

                    // Handle retention
                    handleRetention(backupLocationUri)

                    Result.success()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure()
        }
    }

    private fun handleRetention(backupLocationUri: String?) {
        val maxBackups = preferenceRepository.getBackupRetention()
        if (maxBackups == -1) return // Unlimited

        val filenameFormat = preferenceRepository.getBackupFileNameFormat()
        // Create a regex to match files generated by the pattern
        // Escape the pattern first, then replace the escaped {timestamp} with .*
        val patternRegex = Regex.escape(filenameFormat).replace("\\{timestamp\\}", ".*").toRegex()

        try {
            if (backupLocationUri != null) {
                // Custom location retention
                val treeUri = backupLocationUri.toUri()
                val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                    applicationContext,
                    treeUri
                )

                if (pickedDir != null) {
                    val files = pickedDir.listFiles()
                        .filter { it.name != null && patternRegex.matches(it.name!!) }
                        .sortedByDescending { it.lastModified() } // Newest first

                    if (files.size > maxBackups) {
                        val filesToDelete = files.drop(maxBackups)
                        for (file in filesToDelete) {
                            file.delete()
                            Log.d(TAG, "Deleted old backup (retention): ${file.name}")
                        }
                    }
                }
            } else {
                // Default location retention
                val backupsDir = File(applicationContext.getExternalFilesDir(null), "backups")
                if (backupsDir.exists()) {
                    val files = backupsDir.listFiles()
                        ?.filter { patternRegex.matches(it.name) }
                        ?.sortedByDescending { it.lastModified() }

                    if (files != null && files.size > maxBackups) {
                        val filesToDelete = files.drop(maxBackups)
                        for (file in filesToDelete) {
                            file.delete()
                            Log.d(TAG, "Deleted old backup (retention): ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling backup retention", e)
        }
    }
}
