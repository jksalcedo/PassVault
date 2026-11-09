package com.jksalcedo.passvault.presentation.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.data.local.entity.CustomFieldsPayload
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.EntryDetails
import com.jksalcedo.passvault.data.repository.CategoryRepository
import com.jksalcedo.passvault.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.jksalcedo.passvault.PassVaultApplication as PassVaultApp

/**
 * ViewModel for the read-only entry screen.
 */
class ViewEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val passwordRepository: PasswordRepository =
        (application as PassVaultApp).container.passwordRepository
    private val categoryRepository: CategoryRepository =
        (application as PassVaultApp).container.categoryRepository

    fun readDetails(entry: PasswordEntry): EntryDetails {
        var decryptionFailed = false
        val password = try {
            Encryption.ensureKeyExists()
            if (entry.passwordCipher.isNotEmpty()) {
                Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
            } else {
                ""
            }
        } catch (_: Exception) {
            decryptionFailed = true
            ""
        }

        val customFields = try {
            if (entry.customFieldsCipher != null && entry.customFieldsIv != null) {
                val json = Encryption.decrypt(entry.customFieldsCipher, entry.customFieldsIv)
                Json.decodeFromString<CustomFieldsPayload>(json).fields
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        return EntryDetails(password, customFields, decryptionFailed)
    }

    suspend fun getCategoryColorHex(categoryName: String): String? =
        categoryRepository.getCategoryByName(categoryName)?.colorHex

    fun moveToTrash(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.moveToTrash(id) }
    }

    fun getEntryById(id: Long): LiveData<PasswordEntry> = passwordRepository.getEntryById(id)
}
