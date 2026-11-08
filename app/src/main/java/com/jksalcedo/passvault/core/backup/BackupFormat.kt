package com.jksalcedo.passvault.core.backup

import com.jksalcedo.passvault.data.enums.ImportType
import java.util.Locale

/**
 * Maps the user-facing export format preference to the file extension, MIME type and
 * import handler used across manual export, automatic backups and restore.
 */
enum class BackupFormat(
    val prefValue: String,
    val extension: String,
    val mimeType: String,
    val importType: ImportType?
) {
    JSON("json", "json", "application/json", ImportType.PASSVAULT_JSON),
    CSV("csv", "csv", "text/csv", ImportType.PASSVAULT_CSV),
    CHROME_CSV("chrome_csv", "csv", "text/csv", ImportType.CHROME_CSV),
    KDBX("kdbx", "kdbx", "application/x-keepass2", ImportType.KEEPASS_KDBX);

    companion object {
        /**
         * Resolves a format from the persisted `export_format` preference value.
         */
        fun fromPref(value: String?): BackupFormat {
            return entries.firstOrNull {
                it.prefValue.equals(value, ignoreCase = true)
            } ?: JSON
        }

        /**
         * Resolves a format from a file name. Returns null for unknown extensions.
         */
        fun fromFileName(name: String): BackupFormat? {
            val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when (extension) {
                "json" -> JSON
                "csv" -> CSV
                "chrome_csv" -> CHROME_CSV
                "kdbx" -> KDBX
                else -> null
            }
        }
    }
}
