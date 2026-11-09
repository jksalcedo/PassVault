package com.jksalcedo.passvault.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.jksalcedo.passvault.PassVaultApplication as PassVaultApp

/**
 * ViewModel for the trash screen.
 */
class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val passwordRepository: PasswordRepository =
        (application as PassVaultApp).container.passwordRepository

    val deletedEntries: LiveData<List<PasswordEntry>> = passwordRepository.getDeletedEntries()

    init {
        purgeOldDeletedEntries()
    }

    fun purgeOldDeletedEntries() {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.purgeOldDeletedEntries() }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.restoreFromTrash(id) }
    }

    fun deletePermanently(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.delete(entry) }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.emptyTrash() }
    }
}
