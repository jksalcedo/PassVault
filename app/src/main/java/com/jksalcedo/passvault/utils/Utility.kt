package com.jksalcedo.passvault.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.CustomField
import com.jksalcedo.passvault.data.CustomFieldsPayload
import com.jksalcedo.passvault.data.ExportResult
import com.jksalcedo.passvault.data.ImportRecord
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
import java.time.format.DateTimeParseException
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
    fun serializeEntries(
        list: List<PasswordEntry>,
        format: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): ExportResult {
        val normalized = format.lowercase(Locale.ROOT)
        val json = Json { prettyPrint = true }

        val successfulRecords = mutableListOf<ImportRecord>()
        val failedEntries = mutableListOf<String>()

        // Convert entries, collecting failures
        list.forEachIndexed { index, entry ->
            onProgress?.invoke(index + 1, list.size)
            when (val result = entry.toImportRecordResult()) {
                is Result.Success -> successfulRecords.add(result.value)
                is Result.Failure -> failedEntries.add(entry.title)
            }
        }

        val serializedData = if (normalized == "json") {
            json.encodeToString(successfulRecords)
        } else {
            Csv.encodeToString(successfulRecords)
        }

        return ExportResult(
            serializedData = serializedData,
            successCount = successfulRecords.size,
            failedEntries = failedEntries,
            totalCount = list.size
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun deserializeEntries(serializedString: String, format: String): List<PasswordEntry> {
        val normalized = format.lowercase(Locale.ROOT)
        return try {
            val importRecords: List<ImportRecord> = if (normalized == "json") {
                Json.decodeFromString(serializedString)
            } else {
                Csv.decodeFromString(serializedString)
            }
            importRecords.map { it.toPasswordEntry() }
        } catch (e: Exception) {
            // Fallback for old format
            if (normalized == "json") {
                try {
                    Json.decodeFromString<List<PasswordEntry>>(serializedString)
                } catch (e2: Exception) {
                    throw e2
                }
            } else {
                throw e
            }
        }
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
        val formatter = DateTimeFormatter.ofPattern("MMM dd yyyy h:mm a", Locale.getDefault())
        return Instant.ofEpochMilli(it).atZone(zoneId).format(formatter)
    }

    fun String?.toEpochMillis(): Long = this.let {
        if (this.isNullOrBlank()) {
            return System.currentTimeMillis()
        }
        return try {
            Instant.parse(this).toEpochMilli()
        } catch (e: DateTimeParseException) {
            android.util.Log.w("Utility", "Could not parse timestamp: '$this'. $e")
            System.currentTimeMillis()
        }
    }

    fun ImportRecord.toPasswordEntry(): PasswordEntry = this.let {
        val (cipher, iv) = Encryption.encrypt(it.password)

        // Encrypt custom fields
        var customFieldsCipher: String? = null
        var customFieldsIv: String? = null
        if (it.customFields.isNotEmpty()) {
            val payload = CustomFieldsPayload(fields = it.customFields)
            val json = Json.encodeToString(payload)
            val (cfCipher, cfIv) = Encryption.encrypt(json)
            customFieldsCipher = cfCipher
            customFieldsIv = cfIv
        }

        return PasswordEntry(
            title = title,
            username = username,
            passwordCipher = cipher,
            passwordIv = iv,
            email = email,
            url = url,
            category = category ?: "General",
            notes = notes,
            createdAt = createdAt ?: System.currentTimeMillis(),
            updatedAt = updatedAt ?: System.currentTimeMillis(),
            customFieldsCipher = customFieldsCipher,
            customFieldsIv = customFieldsIv
        )
    }

    /**
     * Converts a PasswordEntry to an ImportRecord, returning a Result to handle decryption failures.
     */
    private fun PasswordEntry.toImportRecordResult(): Result<ImportRecord> {
        return try {
            val password = Encryption.decrypt(this.passwordCipher, this.passwordIv)
            Result.Success(
                ImportRecord(
                    title = this.title,
                    username = this.username,
                    password = password,
                    email = this.email,
                    url = this.url,
                    category = this.category,
                    notes = this.notes,
                    createdAt = this.createdAt,
                    updatedAt = this.updatedAt,
                    customFields = this.getCustomFields()
                )
            )
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    /**
     * Legacy function for backward compatibility. Throws exception on failure.
     */
    @Deprecated("Old converter")
    fun PasswordEntry.toImportRecord(): ImportRecord {
        val password = try {
            Encryption.decrypt(this.passwordCipher, this.passwordIv)
        } catch (e: Exception) {
            throw e
        }
        return ImportRecord(
            title = this.title,
            username = this.username,
            password = password,
            email = this.email,
            url = this.url,
            category = this.category,
            notes = this.notes,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            customFields = this.getCustomFields()
        )
    }

    /**
     * Simple Result sealed class for internal use.
     */
    private sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val exception: Exception) : Result<Nothing>()
    }

    fun getCategoryColor(context: Context, category: String?, colorHex: String? = null): Int {
        // If colorHex is provided, parse it first
        if (!colorHex.isNullOrBlank()) {
            try {
                return colorHex.toColorInt()
            } catch (_: IllegalArgumentException) {
                // default color mapping
            }
        }

        val colorRes = when (category) {
            "General" -> R.color.category_general
            "Social" -> R.color.category_social
            "Work" -> R.color.category_work
            "Personal" -> R.color.category_personal
            "Finance" -> R.color.category_finance
            "Entertainment" -> R.color.category_entertainment
            else -> R.color.category_general
        }
        return context.getColor(colorRes)
    }

    fun zipFiles(files: List<File>, zipFile: File): Boolean {
        return try {
            java.util.zip.ZipOutputStream(
                java.io.BufferedOutputStream(
                    java.io.FileOutputStream(
                        zipFile
                    )
                )
            )
                .use { out ->
                    val data = ByteArray(1024)
                    for (file in files) {
                        java.io.FileInputStream(file).use { fi ->
                            java.io.BufferedInputStream(fi, 1024).use { origin ->
                                val entry = java.util.zip.ZipEntry(file.name)
                                out.putNextEntry(entry)
                                var count: Int
                                while (origin.read(data, 0, 1024).also { count = it } != -1) {
                                    out.write(data, 0, count)
                                }
                            }
                        }
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun PasswordEntry.getCustomFields(): List<CustomField> {
        if (this.customFieldsCipher == null || this.customFieldsIv == null) {
            return emptyList()
        }
        return try {
            val json = Encryption.decrypt(this.customFieldsCipher, this.customFieldsIv)
            Json.decodeFromString<CustomFieldsPayload>(json).fields
        } catch (_: Exception) {
            emptyList()
        }
    }
}
