package com.jksalcedo.passvault.data.model

import android.net.Uri

/**
 * Represents a backup file, which could be a local File or a DocumentFile (SAF).
 */
data class BackupItem(
    val name: String,
    val uri: Uri,
    val lastModified: Long,
    val size: Long,
    val isSaf: Boolean // True if this is from Storage Access Framework
)
