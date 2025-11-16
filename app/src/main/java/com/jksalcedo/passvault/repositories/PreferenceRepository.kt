package com.jksalcedo.passvault.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun setExportFormat(format: String) {
        prefs.edit { putString("export_format", format) }
    }

    fun getExportFormat(): String {
        return prefs.getString("export_format", "json") ?: "json"
    }

    fun setAutoBackups(enabled: Boolean) {
        prefs.edit { putBoolean("auto_backups", enabled) }
        if (enabled) {
            updateLastBackupTime()
        }
    }

    fun getAutoBackups(): Boolean {
        return prefs.getBoolean("auto_backups", false)
    }

    fun updateLastBackupTime() {
        prefs.edit { putLong("last_backup_time", System.currentTimeMillis()) }
    }

    fun getLastBackupTime(): Long {
        return prefs.getLong("last_backup_time", 0L)
    }

    fun setRequireAuthForExport(enabled: Boolean) {
        prefs.edit { putBoolean("require_auth_export", enabled) }
    }

    fun getRequireAuthForExport(): Boolean {
        return prefs.getBoolean("require_auth_export", true)
    }

    fun setEncryptBackups(enabled: Boolean) {
        prefs.edit { putBoolean("encrypt_backups", enabled) }
    }

    fun getEncryptBackups(): Boolean {
        return prefs.getBoolean("encrypt_backups", true)
    }

    fun getPasskey(): String? {
        return prefs.getString("passkey", null)
    }

    fun clear() {
        prefs.edit { clear().apply() }
    }
}