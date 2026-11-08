package com.jksalcedo.passvault.core.backup

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.jksalcedo.passvault.data.model.BackupItem
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import java.io.File

/**
 * Shared access to the configured backup location.
 *
 * Both the automatic backup worker and the Manage Backups screen use this so that file
 * listing, retention and deletion always agree on what counts as a backup.
 */
object BackupStorage {

    private const val TAG = "BackupStorage"

    /**
     * Returns the default internal backup directory, without creating it.
     */
    fun backupDirectory(context: Context): File? {
        val base = context.getExternalFilesDir(null) ?: return null
        return File(base, "backups")
    }

    /**
     * Returns the default internal backup directory, creating it if necessary.
     */
    fun ensureBackupDirectory(context: Context): File? {
        val directory = backupDirectory(context) ?: return null
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "Unable to create backup directory: ${directory.absolutePath}")
            return null
        }
        return directory
    }

    /**
     * Lists backups in the configured location, newest first.
     *
     * All filename patterns ever used are considered so that changing the pattern does
     * not hide older backups.
     */
    fun listBackups(context: Context, preferences: PreferenceRepository): List<BackupItem> {
        val regexes = BackupFileMatcher.buildRegexes(preferences.getBackupFileNameFormats())
        if (regexes.isEmpty()) return emptyList()

        val items = mutableListOf<BackupItem>()
        val location = preferences.getBackupLocation()

        if (location != null) {
            try {
                val directory = DocumentFile.fromTreeUri(context, location.toUri())
                directory?.listFiles()?.forEach { file ->
                    val name = file.name ?: return@forEach
                    if (regexes.any { it.matches(name) }) {
                        items.add(
                            BackupItem(
                                name = name,
                                uri = file.uri,
                                lastModified = file.lastModified(),
                                size = file.length(),
                                isSaf = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to list backups at custom location", e)
            }
        } else {
            backupDirectory(context)?.let { directory ->
                if (directory.exists() && directory.isDirectory) {
                    directory.listFiles()?.forEach { file ->
                        if (regexes.any { it.matches(file.name) }) {
                            items.add(
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
        }

        return items.sortedByDescending { it.lastModified }
    }

    /**
     * Deletes a single backup file.
     */
    fun deleteBackup(context: Context, item: BackupItem): Boolean {
        return try {
            if (item.isSaf) {
                DocumentFile.fromSingleUri(context, item.uri)?.delete() ?: false
            } else {
                val file = item.uri.path?.let { File(it) }
                    ?: backupDirectory(context)?.let { File(it, item.name) }
                file != null && file.exists() && file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to delete backup ${item.name}", e)
            false
        }
    }
}
