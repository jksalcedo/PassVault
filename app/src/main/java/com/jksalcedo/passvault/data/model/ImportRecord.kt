package com.jksalcedo.passvault.data.model

import com.jksalcedo.passvault.data.local.entity.CustomField
import kotlinx.serialization.Serializable

@Serializable
data class ImportRecord(
    val title: String,
    val username: String?,
    val password: String,
    val email: String? = null,
    val url: String? = null,
    val category: String? = null,
    val notes: String?,
    val createdAt: Long?,
    val updatedAt: Long?,
    val customFields: List<CustomField> = emptyList(),
    val type: String? = null
)