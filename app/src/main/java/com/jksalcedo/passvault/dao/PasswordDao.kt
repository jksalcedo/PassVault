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
    @Query("SELECT * FROM password_entries ORDER BY title ASC")
    fun getAll(): LiveData<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries")
    suspend fun getAllEntries(): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE id = :id")
    fun getEntryById(id: Long): LiveData<PasswordEntry>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(entry: PasswordEntry): Long

    @Update
    suspend fun update(entry: PasswordEntry)

    @Delete
    suspend fun delete(entry: PasswordEntry)



    @Query("SELECT * FROM password_entries WHERE category = :category ORDER BY title ASC")
    fun getEntriesByCategory(category: String): LiveData<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE url LIKE '%' || :domain || '%'")
    suspend fun getEntriesByDomain(domain: String): List<PasswordEntry>
}