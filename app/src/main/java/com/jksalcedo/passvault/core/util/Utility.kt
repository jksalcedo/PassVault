package com.jksalcedo.passvault.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.data.local.entity.CustomField
import com.jksalcedo.passvault.data.local.entity.CustomFieldsPayload
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.ChromeRecord
import com.jksalcedo.passvault.data.model.ExportResult
import com.jksalcedo.passvault.data.model.ImportRecord
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
import java.util.Date
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

        val serializedData = when (normalized) {
            "json" -> json.encodeToString(successfulRecords)
            "chrome_csv" -> {
                val chromeRecords = successfulRecords.map {
                    ChromeRecord(
                        name = it.title,
                        url = it.url ?: "",
                        username = it.username ?: "",
                        password = it.password,
                        note = it.notes ?: ""
                    )
                }
                Csv.encodeToString(chromeRecords)
            }

            else -> Csv.encodeToString(successfulRecords)
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
            val importRecords: List<ImportRecord> = when (normalized) {
                "json" -> Json.decodeFromString(serializedString)
                "chrome_csv" -> {
                    val chromeRecords: List<ChromeRecord> = Csv.decodeFromString(serializedString)
                    chromeRecords.map {
                        ImportRecord(
                            title = it.name.ifBlank { it.url }.ifBlank { "Untitled" },
                            username = it.username,
                            password = it.password,
                            url = it.url,
                            notes = it.note,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }

                else -> Csv.decodeFromString(serializedString)
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
            customFieldsIv = customFieldsIv,
            type = try {
                it.type?.let { t -> EntryType.valueOf(t) }
                    ?: if (it.password.isEmpty() && it.username.isNullOrEmpty() && it.url.isNullOrEmpty()) EntryType.NOTE else EntryType.PASSWORD
            } catch (e: Exception) {
                EntryType.PASSWORD
            }
        )
    }

    /**
     * Converts a PasswordEntry to an ImportRecord, returning a Result to handle decryption failures.
     */
    private fun PasswordEntry.toImportRecordResult(): Result<ImportRecord> {
        return try {
            val password = if (this.passwordCipher.isNotEmpty()) {
                Encryption.decrypt(this.passwordCipher, this.passwordIv)
            } else {
                ""
            }
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
                    customFields = this.getCustomFields(),
                    type = this.type.name
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
            if (this.passwordCipher.isNotEmpty()) {
                Encryption.decrypt(this.passwordCipher, this.passwordIv)
            } else {
                ""
            }
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
            customFields = this.getCustomFields(),
            type = this.type.name
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

    fun printVault(context: Context, entries: List<PasswordEntry>, timestampFormat: String) {
        if (entries.isEmpty()) {
            Toast.makeText(context, "No entries available to print", Toast.LENGTH_SHORT).show()
            return
        }
        val formatter = java.text.SimpleDateFormat(timestampFormat, Locale.getDefault())
        val htmlContent = buildPrintHtml(context, entries)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager =
                    context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                val jobName =
                    "${context.getString(R.string.app_name)}_passwords-${formatter.format(Date())}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    android.print.PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun buildPrintHtml(context: Context, entries: List<PasswordEntry>): String {
        val dateFormat = java.text.SimpleDateFormat.getDateTimeInstance()
        val formattedDate = dateFormat.format(Date())

        val rows = StringBuilder()
        for ((_, title1, username1, passwordCipher, passwordIv, notes1, email, url1, category1) in entries) {
            val title = android.text.TextUtils.htmlEncode(title1)
            val category = android.text.TextUtils.htmlEncode(category1 ?: "")
            val username = android.text.TextUtils.htmlEncode(username1 ?: email ?: "")
            val decryptedPassword = try {
                if (passwordCipher.isNotEmpty()) {
                    Encryption.decrypt(passwordCipher, passwordIv)
                } else ""
            } catch (_: Exception) {
                ""
            }
            val password = android.text.TextUtils.htmlEncode(decryptedPassword)
            val url = android.text.TextUtils.htmlEncode(url1 ?: "")
            val notes = android.text.TextUtils.htmlEncode(notes1 ?: "")

            rows.append(
                """
                <tr>
                    <td>$title</td>
                    <td>$category</td>
                    <td>$username</td>
                    <td>$password</td>
                    <td>$url</td>
                    <td>$notes</td>
                </tr>
                """.trimIndent()
            )
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="author" content="${context.getString(R.string.app_name)}">
                <title>PassVault Passwords</title>
                <style>
                    body { font-family: sans-serif; margin: 20px; color: #111; }
                    h1 { text-align: center; font-size: 20px; margin-bottom: 4px; }
                    .meta { text-align: center; font-size: 12px; color: #666; margin-bottom: 16px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                    th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; font-size: 11px; word-break: break-word; }
                    th { background-color: #f2f2f2; font-weight: bold; }
                    tr:nth-child(even) { background-color: #fafafa; }
                    @media print {
                        tr { page-break-inside: avoid; }
                    }
                </style>
            </head>
            <body>
                <h1>PassVault Passwords</h1>
                <div class="meta">Exported on: $formattedDate | Total Entries: ${entries.size}</div>
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Category</th>
                            <th>Username / Email</th>
                            <th>Password</th>
                            <th>URL</th>
                            <th>Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()
    }
}
