package com.jksalcedo.passvault.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class CustomFieldsPayload(
    val version: Int = 1,
    val fields: List<CustomField> = emptyList()
)

@Serializable
data class CustomField(
    val id: String,
    val name: String,
    val value: String,
    val isSecret: Boolean = false,
    val meta: Map<String, String> = emptyMap(),
    val order: Int = 0
)
