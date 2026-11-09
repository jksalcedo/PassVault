package com.jksalcedo.passvault.presentation.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.data.local.entity.Category
import com.jksalcedo.passvault.data.local.entity.CustomField
import com.jksalcedo.passvault.data.local.entity.CustomFieldsPayload
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.EntryDetails
import com.jksalcedo.passvault.data.repository.CategoryRepository
import com.jksalcedo.passvault.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.jksalcedo.passvault.PassVaultApplication as PassVaultApp

/**
 * ViewModel for creating and editing vault entries.
 *
 * All encryption/decryption and persistence lives here; the Activity only gathers form input.
 */
class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    private val passwordRepository: PasswordRepository =
        (application as PassVaultApp).container.passwordRepository
    private val categoryRepository: CategoryRepository =
        (application as PassVaultApp).container.categoryRepository

    val categories: LiveData<List<Category>> = categoryRepository.allCategories

    private val _saveResult = MutableLiveData<SaveResult?>(null)
    val saveResult: LiveData<SaveResult?> = _saveResult

    fun getEntryById(id: Long): LiveData<PasswordEntry> = passwordRepository.getEntryById(id)

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

        return EntryDetails(
            password = password,
            customFields = customFields,
            decryptionFailed = decryptionFailed
        )
    }

    fun save(existing: PasswordEntry?, form: EntryForm) {
        viewModelScope.launch {
            try {
                val finalEntry = withContext(Dispatchers.IO) { buildEntry(existing, form) }
                withContext(Dispatchers.IO) {
                    if (existing == null) {
                        passwordRepository.insert(finalEntry)
                    } else {
                        passwordRepository.update(finalEntry)
                    }
                }
                _saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(e)
            }
        }
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }

    private fun buildEntry(existing: PasswordEntry?, form: EntryForm): PasswordEntry {
        Encryption.ensureKeyExists()

        val (cipherText, iv) = if (form.password.isNotEmpty()) {
            Encryption.encrypt(form.password)
        } else {
            Pair("", "")
        }

        val entry = existing?.copy(
            title = form.title,
            username = form.username,
            passwordCipher = cipherText,
            passwordIv = iv,
            notes = form.notes,
            email = form.email,
            url = form.url,
            category = form.category.ifEmpty { "General" },
            type = form.type,
            updatedAt = System.currentTimeMillis()
        ) ?: PasswordEntry(
            title = form.title,
            username = form.username,
            passwordCipher = cipherText,
            passwordIv = iv,
            notes = form.notes,
            email = form.email,
            url = form.url,
            category = form.category.ifEmpty { "General" },
            type = form.type,
            updatedAt = System.currentTimeMillis()
        )

        if (form.customFields.isEmpty()) {
            return entry.copy(customFieldsCipher = null, customFieldsIv = null)
        }

        val payload = CustomFieldsPayload(fields = form.customFields)
        val json = Json.encodeToString(payload)
        val (cfCipher, cfIv) = Encryption.encrypt(json)
        return entry.copy(customFieldsCipher = cfCipher, customFieldsIv = cfIv)
    }
}

data class EntryForm(
    val title: String,
    val username: String?,
    val password: String,
    val notes: String,
    val category: String,
    val email: String?,
    val url: String?,
    val type: EntryType,
    val customFields: List<CustomField>
)

sealed class SaveResult {
    object Success : SaveResult()
    data class Error(val exception: Exception) : SaveResult()
}
