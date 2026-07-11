package com.jksalcedo.passvault.importer

import com.jksalcedo.passvault.data.ChromeRecord
import com.jksalcedo.passvault.data.ImportRecord
import com.jksalcedo.passvault.utils.Utility.toPasswordEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalSerializationApi::class)
class ChromeImporter(
    private val csv: Csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
        ignoreEmptyLines = true
    }
) : VaultImporter {

    override suspend fun parse(raw: String): List<ImportRecord> {
        return try {
            val parsedRows = csv.decodeFromString<List<ChromeRecord>>(raw)
            parsedRows.mapNotNull { row ->
                if (row.password.isEmpty() && row.name.isBlank()) return@mapNotNull null

                ImportRecord(
                    title = row.name.ifBlank { row.url }.ifBlank { "Untitled" },
                    username = row.username,
                    password = row.password,
                    url = row.url,
                    notes = row.note,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun mapToPasswordEntries(records: List<ImportRecord>) =
        records.map { it.toPasswordEntry() }
}
