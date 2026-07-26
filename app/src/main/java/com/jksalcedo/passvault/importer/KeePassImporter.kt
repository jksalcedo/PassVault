package com.jksalcedo.passvault.importer

import android.content.Context
import android.net.Uri
import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.decode
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.Group
import com.jksalcedo.passvault.data.CustomField
import com.jksalcedo.passvault.data.ImportRecord
import com.jksalcedo.passvault.data.KeepassRecord
import com.jksalcedo.passvault.data.enums.ImportType
import com.jksalcedo.passvault.utils.Utility.toEpochMillis
import com.jksalcedo.passvault.utils.Utility.toPasswordEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.decodeFromString
import java.util.UUID

/**
 * Imports passwords from KeePass CSV or KDBX files.
 *
 * @param csv The Csv instance to use for parsing.
 * @param filePath The path to the KDBX file.
 * @param password The password for the KDBX file.
 * @param type The type of import.
 * @param context The context to use for content resolving.
 */
@OptIn(ExperimentalSerializationApi::class)
class KeePassImporter(
    private val csv: Csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
        ignoreEmptyLines = true
    },
    private val filePath: Uri? = null,
    private val password: String? = null,
    private val type: ImportType = ImportType.KEEPASS_CSV,
    private val context: Context? = null
) : VaultImporter {

    /** Standard KeePass field keys — anything else is imported as a custom field. */
    private val standardFieldKeys = setOf(
        BasicField.Title(),
        BasicField.UserName(),
        BasicField.Password(),
        BasicField.Url(),
        BasicField.Notes()
    )

    override suspend fun parse(raw: String): List<ImportRecord> = when (type) {
        ImportType.KEEPASS_CSV -> parseCsv(raw)
        ImportType.KEEPASS_KDBX -> parseKdbx()
        else -> emptyList()
    }

    private fun parseCsv(raw: String): List<ImportRecord> {
        return try {
            val parsedRows = csv.decodeFromString<List<KeepassRecord>>(raw)
            parsedRows.mapNotNull { row ->
                val pw = row.password.trim()
                if (pw.isEmpty() && row.title.isBlank()) return@mapNotNull null
                ImportRecord(
                    title = row.title.trim(),
                    username = row.username.trim(),
                    password = pw,
                    url = row.url,
                    notes = row.notes.trim(),
                    createdAt = row.creationTime.toEpochMillis(),
                    updatedAt = row.lastModificationTime.toEpochMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun parseKdbx(): List<ImportRecord> {
        if (filePath == null || password == null) return emptyList()
        val ctx = context
            ?: throw IllegalStateException("Context is required to import KeePass KDBX files")

        return try {
            val db = ctx.contentResolver.openInputStream(filePath)?.use { stream ->
                // IMPORTANT: Credentials.from(ByteArray) is the KEY-FILE overload.
                // For a password passphrase we must use Credentials.from(EncryptedValue).
                val credentials = Credentials.from(EncryptedValue.fromString(password))
                KeePassDatabase.decode(stream, credentials)
            } ?: return emptyList()

            val recycleBinUuid: UUID? = db.content.meta.recycleBinUuid

            // Collect UUIDs of all entries inside the Recycle Bin (if it exists),
            // so we can skip them during the main traversal.
            val recycleBinEntryUuids = mutableSetOf<UUID>()
            if (recycleBinUuid != null) {
                db.content.group
                    .findChildGroup(predicate = { it.uuid == recycleBinUuid })
                    ?.second
                    ?.traverse { element ->
                        if (element is Entry) recycleBinEntryUuids += element.uuid
                    }
            }

            val results = mutableListOf<ImportRecord>()

            // traverse() visits every Entry in the full group tree (breadth-first via stack).
            // We pair each entry with its parent group name for use as a category hint.
            traverseWithGroupName(db.content.group) { entry, groupName ->
                if (entry.uuid in recycleBinEntryUuids) return@traverseWithGroupName

                val fields = entry.fields
                val title = fields.title?.content
                val entryPassword = fields.password?.content

                // Skip entirely empty entries
                if (title.isNullOrBlank() && entryPassword.isNullOrEmpty()) return@traverseWithGroupName

                // Any field key not in the 5 standard ones becomes a PassVault custom field
                val customFields = fields
                    .filterKeys { it !in standardFieldKeys }
                    .entries
                    .mapIndexed { index, (key, value) ->
                        CustomField(
                            id = key,
                            name = key,
                            value = value.content,
                            isSecret = false,
                            order = index
                        )
                    }

                results += ImportRecord(
                    title = title.orEmpty().ifBlank { "Untitled" },
                    username = fields.userName?.content?.takeIf { it.isNotBlank() },
                    password = entryPassword.orEmpty(),
                    url = fields.url?.content?.takeIf { it.isNotBlank() },
                    notes = fields.notes?.content?.takeIf { it.isNotBlank() },
                    createdAt = entry.times?.creationTime?.toEpochMilli(),
                    updatedAt = entry.times?.lastModificationTime?.toEpochMilli(),
                    customFields = customFields,
                    // Use the KeePass group name as the PassVault category so vault
                    // structure is preserved. Falls back to "General" for the root group.
                    category = groupName.takeIf { it.isNotBlank() && it != db.content.group.name }
                )
            }

            results
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Walks [root] recursively, calling [block] for every [Entry] together with
     * the name of the immediate parent [Group] that holds it.
     */
    private fun traverseWithGroupName(
        root: Group,
        block: (entry: Entry, groupName: String) -> Unit
    ) {
        val stack = ArrayDeque<Pair<Group, String>>()
        stack.addLast(root to root.name)

        while (stack.isNotEmpty()) {
            val (group, name) = stack.removeFirst()
            group.entries.forEach { block(it, name) }
            group.groups.forEach { stack.addLast(it to it.name) }
        }
    }

    override fun mapToPasswordEntries(records: List<ImportRecord>) =
        records.map { it.toPasswordEntry() }
}
