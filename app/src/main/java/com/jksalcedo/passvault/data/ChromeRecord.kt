package com.jksalcedo.passvault.data

import kotlinx.serialization.Serializable

@Serializable
data class ChromeRecord(
    val name: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val note: String = ""
)
