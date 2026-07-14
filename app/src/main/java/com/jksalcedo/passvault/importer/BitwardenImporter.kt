package com.jksalcedo.passvault.importer

import com.jksalcedo.passvault.data.BitwardenExport
import com.jksalcedo.passvault.data.ImportRecord
import com.jksalcedo.passvault.utils.Utility.toEpochMillis
import com.jksalcedo.passvault.utils.Utility.toPasswordEntry
import kotlinx.serialization.json.Json


/**
 * An importer for Bitwarden.
 * This class is responsible for parsing a JSON export from Bitwarden
 * and converting it into a list of [ImportRecord] objects.
 */
class BitwardenImporter(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : VaultImporter {

    override suspend fun parse(raw: String): List<ImportRecord> {
        try {
            val cleanRaw = raw.trimStart('\uFEFF', '\u200B')
            
            // Check for encrypted Bitwarden JSON without fully parsing since it has a different structure
            if (cleanRaw.contains("\"encrypted\": true") || cleanRaw.contains("\"encrypted\":true")) {
                throw Exception("Bitwarden encrypted JSON is not supported. Please export as unencrypted JSON.")
            }
            
            val export = json.decodeFromString<BitwardenExport>(cleanRaw)
            return export.items
                .filter { (it.type == 1 || it.type == 2) && (it.name.isNotBlank() || it.login?.password?.isNotBlank() == true || it.notes?.isNotBlank() == true) }
                .map { item ->
                    val entryType = if (item.type == 2) "NOTE" else "PASSWORD"
                    ImportRecord(
                        title = item.name,
                        username = item.login?.username,
                        password = item.login?.password.orEmpty(),
                        email = null,
                        url = item.login?.uris?.firstOrNull()?.uri,
                        category = null,
                        notes = item.notes,
                        createdAt = item.creationDate?.toEpochMillis(),
                        updatedAt = item.revisionDate?.toEpochMillis(),
                        type = entryType
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