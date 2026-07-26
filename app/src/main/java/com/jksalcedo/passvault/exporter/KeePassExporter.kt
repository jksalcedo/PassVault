package com.jksalcedo.passvault.exporter

import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.encode
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.EntryFields
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Group
import app.keemobile.kotpass.models.Meta
import app.keemobile.kotpass.models.TimeData
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.CustomField
import com.jksalcedo.passvault.data.CustomFieldsPayload
import com.jksalcedo.passvault.data.ExportResult
import com.jksalcedo.passvault.data.PasswordEntry
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

object KeePassExporter {

    fun export(entries: List<PasswordEntry>, password: String): Pair<ByteArray, ExportResult> {
        val credentials = Credentials.from(EncryptedValue.fromString(password))
        val meta = Meta(generator = "PassVault", databaseName = "PassVault")
        val baseDb = KeePassDatabase.Ver4x.create("PassVault", meta, credentials)

        val activeEntries = entries.filter { !it.isDeleted }
        val successfulCount = mutableListOf<String>()
        val failedEntries = mutableListOf<String>()

        val groupToEntriesMap = mutableMapOf<String, MutableList<Entry>>()

        activeEntries.forEach { entry ->
            try {
                val plainPassword = if (entry.passwordCipher.isNotEmpty()) {
                    Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
                } else {
                    ""
                }

                val customFields = getCustomFields(entry)

                val fieldsMap = mutableMapOf<String, EntryValue>()
                fieldsMap[BasicField.Title.key] = EntryValue.Plain(entry.title)
                if (!entry.username.isNullOrBlank()) {
                    fieldsMap[BasicField.UserName.key] = EntryValue.Plain(entry.username)
                }
                if (plainPassword.isNotEmpty()) {
                    fieldsMap[BasicField.Password.key] = EntryValue.Encrypted(EncryptedValue.fromString(plainPassword))
                }
                if (!entry.url.isNullOrBlank()) {
                    fieldsMap[BasicField.Url.key] = EntryValue.Plain(entry.url)
                }
                if (!entry.notes.isNullOrBlank()) {
                    fieldsMap[BasicField.Notes.key] = EntryValue.Plain(entry.notes)
                }

                customFields.forEach { cf ->
                    fieldsMap[cf.name] = if (cf.isSecret) {
                        EntryValue.Encrypted(EncryptedValue.fromString(cf.value))
                    } else {
                        EntryValue.Plain(cf.value)
                    }
                }

                val times = TimeData(
                    creationTime = Instant.ofEpochMilli(entry.createdAt),
                    lastAccessTime = Instant.ofEpochMilli(entry.updatedAt),
                    lastModificationTime = Instant.ofEpochMilli(entry.updatedAt),
                    locationChanged = Instant.ofEpochMilli(entry.updatedAt),
                    expiryTime = null
                )

                val kotpassEntry = Entry(
                    uuid = UUID.randomUUID(),
                    fields = EntryFields(fieldsMap),
                    times = times
                )

                val category = entry.category?.takeIf { it.isNotBlank() } ?: "General"
                groupToEntriesMap.getOrPut(category) { mutableListOf() }.add(kotpassEntry)
                successfulCount.add(entry.title)
            } catch (e: Exception) {
                e.printStackTrace()
                failedEntries.add(entry.title)
            }
        }

        val rootEntries = groupToEntriesMap.remove("General") ?: mutableListOf()
        val subGroups = groupToEntriesMap.map { (categoryName, categoryEntries) ->
            Group(
                uuid = UUID.randomUUID(),
                name = categoryName,
                entries = categoryEntries
            )
        }

        val rootGroup = baseDb.content.group.copy(
            entries = rootEntries,
            groups = subGroups
        )

        val finalDb = baseDb.copy(
            content = baseDb.content.copy(group = rootGroup)
        )

        val outputStream = ByteArrayOutputStream()
        finalDb.encode(outputStream)

        val exportResult = ExportResult(
            serializedData = "",
            successCount = successfulCount.size,
            failedEntries = failedEntries,
            totalCount = activeEntries.size
        )

        return Pair(outputStream.toByteArray(), exportResult)
    }

    private fun getCustomFields(entry: PasswordEntry): List<CustomField> {
        if (entry.customFieldsCipher == null || entry.customFieldsIv == null) {
            return emptyList()
        }
        return try {
            val json = Encryption.decrypt(entry.customFieldsCipher, entry.customFieldsIv)
            Json.decodeFromString<CustomFieldsPayload>(json).fields
        } catch (_: Exception) {
            emptyList()
        }
    }
}
