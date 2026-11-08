package com.jksalcedo.passvault.core.worker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jksalcedo.passvault.core.backup.BackupFileMatcher
import com.jksalcedo.passvault.core.backup.BackupFormat
import com.jksalcedo.passvault.core.backup.BackupStorage
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A worker for creating backups.
 *
 * Automatic backups always use the JSON format for reliability, independent of the
 * manual export format preference. A backup is only counted as successful when at least
 * one entry was serialized and every requested copy was written.
 *
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Auto backup started.")

            val entries = passwordRepository.getAllEntries()
            if (entries.isEmpty()) {
                Log.d(TAG, "No data to back up. Skipping.")
                return@withContext Result.success()
            }

            if (!Encryption.isKeystoreValid()) {
                Log.e(TAG, "Android Keystore key is invalid. Skipping backup.")
                return@withContext Result.failure()
            }

            // Automatic backups always use JSON so they can be restored reliably.
            val format = BackupFormat.JSON
            val exportResult = Utility.serializeEntries(entries, format.prefValue)

            if (exportResult.hasFailures) {
                Log.w(
                    TAG,
                    "Backup: ${exportResult.failedEntries.size} entries failed to export: " +
                            exportResult.failedEntries.joinToString()
                )
            }

            if (exportResult.successCount == 0) {
                Log.e(TAG, "No entries could be exported. Skipping backup.")
                return@withContext Result.failure()
            }

            val encryptionEnabled = preferenceRepository.getEncryptBackups()
            val password = preferenceRepository.getPasswordForAutoBackups()
            val contentBytes = if (encryptionEnabled) {
                if (password.isNullOrEmpty()) {
                    Log.e(
                        TAG,
                        "Encryption is enabled but the auto backup password is missing. Skipping backup."
                    )
                    return@withContext Result.failure()
                }
                Encryption.encryptFileContentArgon(
                    exportResult.serializedData,
                    password.toByteArray()
                ).toByteArray(Charsets.UTF_8)
            } else {
                exportResult.serializedData.toByteArray(Charsets.UTF_8)
            }

            val timestampFormat = preferenceRepository.getBackupTimestampFormat()
            val filenameFormat = preferenceRepository.getBackupFileNameFormat()
            val timestamp = SimpleDateFormat(timestampFormat, Locale.getDefault()).format(Date())
            val baseFileName = filenameFormat.replace("{timestamp}", timestamp)

            val copiesToCreate = preferenceRepository.getBackupCopies().coerceAtLeast(1)
            val backupLocationUri = preferenceRepository.getBackupLocation()
            val existingNames = existingFileNames(backupLocationUri)
            val fileNames = BackupFileMatcher.uniqueFileNames(
                baseName = baseFileName,
                extension = format.extension,
                count = copiesToCreate,
                existingNames = existingNames
            )

            var successCount = 0
            for (fileName in fileNames) {
                val success = if (backupLocationUri != null) {
                    writeToSaf(backupLocationUri, fileName, format.mimeType, contentBytes)
                } else {
                    writeToInternal(fileName, contentBytes)
                }
                if (success) successCount++
            }

            if (successCount == 0) {
                Log.e(TAG, "Failed to write any backup copies")
                return@withContext Result.failure()
            }

            Log.d(TAG, "Created $successCount/${fileNames.size} backup copies")
            preferenceRepository.updateLastBackupTime()

            // Only prune old backups when the new backup contains every entry.
            if (!exportResult.hasFailures) {
                handleRetention()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure()
        }
    }

    /**
     * Names currently present in the configured location. Used to avoid overwriting an
     * existing backup when a date-only timestamp is used.
     */
    private fun existingFileNames(backupLocationUri: String?): Set<String> {
        return try {
            if (backupLocationUri != null) {
                val directory = DocumentFile.fromTreeUri(
                    applicationContext,
                    backupLocationUri.toUri()
                )
                directory?.listFiles()
                    ?.mapNotNull { it.name }
                    ?.toSet()
                    ?: emptySet()
            } else {
                BackupStorage.backupDirectory(applicationContext)
                    ?.listFiles()
                    ?.map { it.name }
                    ?.toSet()
                    ?: emptySet()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to inspect existing backup files", e)
            emptySet()
        }
    }

    private fun writeToInternal(fileName: String, content: ByteArray): Boolean {
        val directory = BackupStorage.ensureBackupDirectory(applicationContext) ?: return false
        return try {
            val backupFile = File(directory, fileName)
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(content)
                outputStream.flush()
            }
            val written = backupFile.length() > 0
            if (written) {
                Log.d(TAG, "Auto backup successful: ${backupFile.absolutePath}")
            } else {
                Log.e(TAG, "Backup file is empty: ${backupFile.absolutePath}")
            }
            written
        } catch (e: Exception) {
            Log.e(TAG, "Error writing backup $fileName", e)
            false
        }
    }

    private fun writeToSaf(
        backupLocationUri: String,
        fileName: String,
        mimeType: String,
        content: ByteArray
    ): Boolean {
        return try {
            val directory = DocumentFile.fromTreeUri(
                applicationContext,
                backupLocationUri.toUri()
            )
            if (directory == null || !directory.canWrite()) {
                Log.e(TAG, "Custom backup location is not accessible or writable")
                return false
            }

            // Avoid DocumentFile's automatic " (1)" duplicate naming, which would not
            // match the backup filename pattern.
            if (directory.findFile(fileName) != null) {
                Log.e(TAG, "A backup named $fileName already exists")
                return false
            }

            val newFile = directory.createFile(mimeType, fileName)
            if (newFile == null) {
                Log.e(TAG, "Failed to create backup file $fileName")
                return false
            }

            val outputStream = applicationContext.contentResolver
                .openOutputStream(newFile.uri)
            if (outputStream == null) {
                Log.e(TAG, "Unable to open output stream for $fileName")
                return false
            }

            outputStream.use {
                it.write(content)
                it.flush()
            }
            Log.d(TAG, "Auto backup successful: ${newFile.uri}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing backup $fileName", e)
            false
        }
    }

    private fun handleRetention() {
        val maxBackups = preferenceRepository.getBackupRetention()
        if (maxBackups <= 0) return // Unlimited (or an invalid value)

        val backups = BackupStorage.listBackups(applicationContext, preferenceRepository)
        if (backups.size <= maxBackups) return

        backups.drop(maxBackups).forEach { backup ->
            if (BackupStorage.deleteBackup(applicationContext, backup)) {
                Log.d(TAG, "Deleted old backup (retention): ${backup.name}")
            }
        }
    }
}
