package com.jksalcedo.passvault.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jksalcedo.passvault.data.enums.EntryType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val username: String?,
    val passwordCipher: String,
    val passwordIv: String,
    val notes: String?,
    val email: String? = null,
    val url: String? = null,
    val category: String? = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val customFieldsCipher: String? = null,
    val customFieldsIv: String? = null,
    @ColumnInfo(defaultValue = "PASSWORD") val type: EntryType = EntryType.PASSWORD,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) : Parcelable
