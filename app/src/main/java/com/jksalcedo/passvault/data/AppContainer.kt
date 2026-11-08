package com.jksalcedo.passvault.data

import android.content.Context
import com.jksalcedo.passvault.data.local.AppDatabase
import com.jksalcedo.passvault.data.repository.CategoryRepository
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple manual dependency container shared by the whole app.
 *
 * Keeping a single container avoids re-creating the database and repositories in every
 * ViewModel/Activity while staying independent of a DI framework.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = AppDatabase.getDatabase(context)

    val passwordRepository: PasswordRepository = PasswordRepository(database.passwordDao())
    val categoryRepository: CategoryRepository = CategoryRepository(database.categoryDao())
    val preferenceRepository: PreferenceRepository = PreferenceRepository(context)

    /**
     * Clears every table in the database. Used when the master key has been invalidated.
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
