package com.jksalcedo.passvault.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BitwardenItem(
    val type: Int,
    val name: String = "",
    val notes: String? = null,
    val login: BitwardenLogin? = null,
    @SerialName("revisionDate") val revisionDate: String? = null,
    @SerialName("creationDate") val creationDate: String? = null
)

@Serializable
data class BitwardenLogin(
    val username: String? = null,
    val password: String? = null,
    val uris: List<BitwardenUris?> = emptyList()
)

@Serializable
data class BitwardenUris(
    val uri: String? = null
)

@Serializable
data class BitwardenExport(val items: List<BitwardenItem> = emptyList())