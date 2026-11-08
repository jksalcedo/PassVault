package com.jksalcedo.passvault.data.model

import com.jksalcedo.passvault.data.local.entity.CustomField

/**
 * Decrypted, presentation-friendly view of a stored entry.
 */
data class EntryDetails(
    val password: String,
    val customFields: List<CustomField>,
    val decryptionFailed: Boolean = false
)
