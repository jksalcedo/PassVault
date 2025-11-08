package com.jksalcedo.passvault.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.jksalcedo.passvault.data.PasswordEntry
import kotlinx.serialization.json.Json
import java.io.File

object Utility {
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun serializeEntries(list: List<PasswordEntry>): String {
        return Json.encodeToString(list)
    }

    fun deserializeEntries(serializedString: String): List<PasswordEntry> {
        return Json.decodeFromString(serializedString)
    }

    fun getDatabaseSize(context: Context, dbName: String): Long {
        val dbFile: File = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            return 0L
        }
        return dbFile.length()
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
