package com.jksalcedo.passvault.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeepassRecord(
    @SerialName("Title")
    val title: String = "",
    @SerialName("UserName")
    val username: String = "",
    @SerialName("Password")
    val password: String = "",
    @SerialName("URL")
    val url: String = "",
    @SerialName("Notes")
    val notes: String = "",
    @SerialName("CreationTime")
    val creationTime: String? = null,
    @SerialName("LastModificationTime")
    val lastModificationTime: String? = null
)