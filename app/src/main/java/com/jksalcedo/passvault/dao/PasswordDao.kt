package com.jksalcedo.passvault.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jksalcedo.passvault.data.PasswordEntry

@Dao
interface PasswordDao {
    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 ORDER BY title ASC")
    fun getAll(): LiveData<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0")
    suspend fun getAllEntries(): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE id = :id")
    fun getEntryById(id: Long): LiveData<PasswordEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PasswordEntry): Long

    @Update
    suspend fun update(entry: PasswordEntry)

    @Delete
    suspend fun delete(entry: PasswordEntry)

    @Query("UPDATE password_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE password_entries SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("SELECT * FROM password_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedEntries(): LiveData<List<PasswordEntry>>

    @Query("DELETE FROM password_entries WHERE isDeleted = 1 AND deletedAt < :timestamp")
    suspend fun purgeOldDeletedEntries(timestamp: Long)

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND category = :category ORDER BY title ASC")
    fun getEntriesByCategory(category: String): LiveData<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND url LIKE '%' || :domain || '%'")
    suspend fun getEntriesByDomain(domain: String): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%')")
    suspend fun searchEntries(query: String): List<PasswordEntry>
}