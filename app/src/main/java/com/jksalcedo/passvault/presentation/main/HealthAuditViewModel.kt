package com.jksalcedo.passvault.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.util.PasswordStrengthAnalyzer
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import com.jksalcedo.passvault.Application as PassVaultApp

/**
 * ViewModel for the security health audit screen.
 *
 * Decrypts passwords and computes weak/reused/old groups off the main thread, then exposes
 * an immutable [AuditResult] for the Activity to render.
 */
class HealthAuditViewModel(application: Application) : AndroidViewModel(application) {

    private val passwordRepository: PasswordRepository =
        (application as PassVaultApp).container.passwordRepository

    private val _auditResult = MutableLiveData<AuditResult?>()
    val auditResult: LiveData<AuditResult?> = _auditResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun performAudit() {
        if (_isLoading.value == true) return
        _isLoading.value = true

        viewModelScope.launch {
            val allEntries = withContext(Dispatchers.IO) { passwordRepository.getAllEntries() }
            val passwordsOnly = allEntries.filter { it.type == EntryType.PASSWORD && !it.isDeleted }

            val weakEntries = mutableListOf<PasswordEntry>()
            val oldEntries = mutableListOf<PasswordEntry>()
            val passwordToEntries = mutableMapOf<String, MutableList<PasswordEntry>>()
            val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)

            withContext(Dispatchers.Default) {
                Encryption.ensureKeyExists()
                passwordsOnly.forEach { entry ->
                    try {
                        val plain = Encryption.decrypt(entry.passwordCipher, entry.passwordIv)

                        if (PasswordStrengthAnalyzer.analyze(plain).score < 45) {
                            weakEntries.add(entry)
                        }
                        if (plain.isNotEmpty()) {
                            passwordToEntries.getOrPut(plain) { mutableListOf() }.add(entry)
                        }
                        if (entry.updatedAt < sixMonthsAgo) {
                            oldEntries.add(entry)
                        }
                        yield()
                    } catch (_: Exception) {
                        // Entry cannot be decrypted; skip it from the audit.
                    }
                }
            }

            val duplicateGroups = passwordToEntries.values.filter { it.size > 1 }
            _auditResult.value = AuditResult(
                weakEntries = weakEntries,
                reusedEntries = duplicateGroups.flatten(),
                oldEntries = oldEntries,
                duplicateGroups = duplicateGroups
            )
            _isLoading.value = false
        }
    }

    fun moveToTrashBulk(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { passwordRepository.moveToTrash(it) }
        }
    }

    fun deleteBulk(entries: List<PasswordEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            entries.forEach { passwordRepository.delete(it) }
        }
    }
}

data class AuditResult(
    val weakEntries: List<PasswordEntry>,
    val reusedEntries: List<PasswordEntry>,
    val oldEntries: List<PasswordEntry>,
    val duplicateGroups: List<List<PasswordEntry>>
)
