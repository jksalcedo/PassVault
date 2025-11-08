package com.jksalcedo.passvault.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.jksalcedo.passvault.data.PasswordEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Utility {
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun serializeEntries(list: List<PasswordEntry>, format: String): String {
        return if (format == "json") Json.encodeToString(list) else Csv.encodeToString(list)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun deserializeEntries(serializedString: String, format: String): List<PasswordEntry> {
        return if (format == "json") Json.decodeFromString(serializedString) else Csv.decodeFromString(
            serializedString
        )
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
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
            else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun Long.formatTime(zoneId: ZoneId = ZoneId.systemDefault()): String = this.let {
        val formatter = DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH)
        return Instant.ofEpochMilli(it).atZone(zoneId).format(formatter)
    }
}
