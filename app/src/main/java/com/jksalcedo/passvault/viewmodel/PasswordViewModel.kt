package com.jksalcedo.passvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.repositories.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasswordViewModel(app: Application) : AndroidViewModel(app) {
    val passwordRepository = PasswordRepository(app.applicationContext)
    val allEntries: LiveData<List<PasswordEntry>> = passwordRepository.getAll()

    fun getEntryById(id: Long): LiveData<PasswordEntry> {
        return passwordRepository.getEntryById(id)
    }

    fun insert(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.insert(entry) }
    }

    fun update(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.update(entry) }
    }

    fun delete(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.delete(entry) }
    }
}
