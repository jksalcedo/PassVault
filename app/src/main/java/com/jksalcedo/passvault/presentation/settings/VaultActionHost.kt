package com.jksalcedo.passvault.presentation.settings

/**
 * Interface that any activity hosting [SettingsFragment] or [ImportDialog] must implement
 * to provide vault-action callbacks (export, print, restart, password prompt).
 */
interface VaultActionHost {
    fun createFileForExport()
    fun printVaultData()
    fun triggerRestart()
    fun ensurePasswordExists(isImporting: Boolean, onPasswordReady: (String) -> Unit)
}

