package com.jksalcedo.passvault.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.jksalcedo.passvault.data.AppDatabase
import com.jksalcedo.passvault.data.PasswordEntry

class PasswordRepository(context: Context) {

    private val passwordDao = AppDatabase.getDatabase(context).passwordDao()

    fun getAll(): LiveData<List<PasswordEntry>> {
        return passwordDao.getAll()
    }

    suspend fun getAllEntries(): List<PasswordEntry> {
        return passwordDao.getAllEntries()
    }

    fun getEntryById(id: Long): LiveData<PasswordEntry> {
        return passwordDao.getEntryById(id)
    }

    suspend fun insert(entry: PasswordEntry) {
        passwordDao.insert(entry)
    }

    suspend fun update(entry: PasswordEntry) {
        passwordDao.update(entry)
    }

    suspend fun delete(entry: PasswordEntry) {
        passwordDao.delete(entry)
    }

}