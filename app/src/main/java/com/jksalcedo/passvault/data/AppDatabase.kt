package com.jksalcedo.passvault.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.jksalcedo.passvault.dao.CategoryDao
import com.jksalcedo.passvault.dao.PasswordDao

@Database(
    entities = [PasswordEntry::class, Category::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = AppDatabase.Migration3To4::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "passvault_db"
                ).build().also { INSTANCE = it }
            }
        }

        @VisibleForTesting
        fun initializeForTesting(instance: AppDatabase) {
            INSTANCE = instance
        }
    }

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "categories", columnName = "isDefault")
    )
    class Migration3To4 : AutoMigrationSpec
}
