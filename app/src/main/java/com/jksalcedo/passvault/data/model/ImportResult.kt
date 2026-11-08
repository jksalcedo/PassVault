package com.jksalcedo.passvault.data.model

data class ImportResult(
    val title: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
