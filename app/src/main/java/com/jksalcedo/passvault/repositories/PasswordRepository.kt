package com.jksalcedo.passvault.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.jksalcedo.passvault.data.AppDatabase
import com.jksalcedo.passvault.data.PasswordEntry

/**
 * Repository for managing passwords.
 * @param context The application context.
 */
class PasswordRepository(context: Context) {

    private val passwordDao = AppDatabase.getDatabase(context).passwordDao()

    /**
     * Gets all password entries as [LiveData].
     * @return A [LiveData] list of [PasswordEntry].
     */
    fun getAll(): LiveData<List<PasswordEntry>> {
        return passwordDao.getAll()
    }

    /**
     * Gets all password entries.
     * @return A list of [PasswordEntry].
     */
    suspend fun getAllEntries(): List<PasswordEntry> {
        return passwordDao.getAllEntries()
    }

    /**
     * Gets a password entry by ID.
     * @param id The ID of the entry to retrieve.
     * @return A [LiveData] of the [PasswordEntry].
     */
    fun getEntryById(id: Long): LiveData<PasswordEntry> {
        return passwordDao.getEntryById(id)
    }

    /**
     * Inserts a new password entry.
     * @param entry The [PasswordEntry] to insert.
     */
    suspend fun insert(entry: PasswordEntry) {
        passwordDao.insert(entry)
    }

    /**
     * Updates an existing password entry.
     * @param entry The [PasswordEntry] to update.
     */
    suspend fun update(entry: PasswordEntry) {
        passwordDao.update(entry)
    }

    /**
     * Deletes a password entry.
     * @param entry The [PasswordEntry] to delete.
     */
    suspend fun delete(entry: PasswordEntry) {
        passwordDao.delete(entry)
    }



    fun getEntriesByCategory(category: String): LiveData<List<PasswordEntry>> {
        return passwordDao.getEntriesByCategory(category)
    }

    suspend fun getEntriesByDomain(domain: String): List<PasswordEntry> {
        return passwordDao.getEntriesByDomain(domain)
    }
}