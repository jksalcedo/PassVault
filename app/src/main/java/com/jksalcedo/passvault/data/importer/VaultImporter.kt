package com.jksalcedo.passvault.data.importer

import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.ImportRecord

interface VaultImporter {
    suspend fun parse(raw: String): List<ImportRecord>
    fun mapToPasswordEntries(records: List<ImportRecord>): List<PasswordEntry>
}