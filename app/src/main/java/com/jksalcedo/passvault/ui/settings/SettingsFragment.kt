package com.jksalcedo.passvault.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.auth.BiometricAuthenticator
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.utils.Utility.formatFileSize
import com.jksalcedo.passvault.viewmodel.SettingsModelFactory
import com.jksalcedo.passvault.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat() {

    private var settingsActivity: SettingsActivity? = null
    private val biometricAuthenticator = BiometricAuthenticator()

    private val prefsRepository: PreferenceRepository by lazy {
        PreferenceRepository(requireContext())
    }
    private val viewModel: SettingsViewModel by viewModels {
        SettingsModelFactory(application = requireActivity().application, requireActivity())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingsActivity) {
            settingsActivity = context
        } else {
            throw IllegalStateException("The activity must be SettingsActivity.")
        }
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        setupAboutPreference()
        setupExportAndImport()
        setupExportFormat()
        setupAutoBackups()
        setupLastBackup()
        setupStorageInfo()
        setupClearData()
        setupSecurityPreferences()
    }

    private fun setupAboutPreference() {
        findPreference<Preference>("about_app")?.summary = viewModel.getAppVersion()
        findPreference<Preference>("about_app")?.setOnPreferenceClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_container, AboutFragment())
                .addToBackStack(null)
                .commit()
            true
        }
    }

    private fun setupExportAndImport() {
        findPreference<Preference>("export_data")?.setOnPreferenceClickListener {
            handleExportRequest()
            true
        }
        findPreference<Preference>("import_data")?.setOnPreferenceClickListener {
            settingsActivity?.openFileForImport()
            true
        }
    }

    private fun handleExportRequest() {
        // Check if the "Require Auth" setting is enabled
        val requireAuth = prefsRepository.getRequireAuthForExport()

        if (requireAuth && BiometricAuthenticator.canAuthenticate(requireContext())) {
            //  show the prompt if required
            biometricAuthenticator.showBiometricPrompt(
                fragment = this,
                onSuccess = {
                    // proceed with the export
                    settingsActivity?.createFileForExport()
                },
                onFailure = { _, errString ->
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            // proceed directly
            settingsActivity?.createFileForExport()
        }
    }

    private fun setupExportFormat() {
        findPreference<ListPreference>("export_format")?.setOnPreferenceChangeListener { _, newValue ->
            val format = newValue as String
            prefsRepository.setExportFormat(format)
            Utility.showToast(requireContext(), "Export format set to: ${format.uppercase()}")
            true
        }
    }

    private fun setupAutoBackups() {
        val autoBackupPref = findPreference<SwitchPreferenceCompat>("auto_backups")
        autoBackupPref?.isChecked = prefsRepository.getAutoBackups()
        autoBackupPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            viewModel.setAutoBackups(enabled)
            if (enabled) {
                Utility.showToast(requireContext(), "Auto backups enabled (Daily)")
                updateLastBackupSummary()
            } else {
                Utility.showToast(requireContext(), "Auto backups disabled")
            }
            true
        }
    }

    private fun setupLastBackup() {
        updateLastBackupSummary()
        findPreference<Preference>("last_backup")?.setOnPreferenceClickListener {
            val lastBackupTime = prefsRepository.getLastBackupTime()
            if (lastBackupTime > 0) {
                val date = Date(lastBackupTime)
                val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                AlertDialog.Builder(requireContext())
                    .setTitle("Last Backup")
                    .setMessage("Last backup was performed on:\n${format.format(date)}")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                Utility.showToast(requireContext(), "No backup has been performed yet")
            }
            true
        }
    }

    private fun updateLastBackupSummary() {
        val lastBackupPref = findPreference<Preference>("last_backup")
        val lastBackupTime = prefsRepository.getLastBackupTime()
        lastBackupPref?.summary = if (lastBackupTime > 0) {
            val date = Date(lastBackupTime)
            val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            format.format(date)
        } else {
            "Never"
        }
    }

    private fun setupStorageInfo() {
        updateStorageInfoSummary()
        findPreference<Preference>("storage_info")?.setOnPreferenceClickListener {
            val (dbSize, prefsSize) = viewModel.getStorageInfo()
            val totalSize = dbSize + prefsSize
            val message = "Database: ${formatFileSize(dbSize)}\n" +
                    "Preferences: ${formatFileSize(prefsSize)}\n" +
                    "Total: ${formatFileSize(totalSize)}"
            AlertDialog.Builder(requireContext())
                .setTitle("Storage Information")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
            true
        }
    }

    private fun updateStorageInfoSummary() {
        val (dbSize, prefsSize) = viewModel.getStorageInfo()
        val totalSize = dbSize + prefsSize
        findPreference<Preference>("storage_info")?.summary = "Total: ${formatFileSize(totalSize)}"
    }

    private fun setupClearData() {
        findPreference<Preference>("clear_data")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will delete all stored passwords and settings. This action cannot be undone.\n\nAre you sure you want to continue?")
                .setPositiveButton("Delete All") { _, _ ->
                    viewModel.clearAllData()
                    Utility.showToast(requireContext(), "All data cleared successfully")
                }
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
            true
        }
    }

    private fun setupSecurityPreferences() {
        val requireAuthPref = findPreference<SwitchPreferenceCompat>("require_auth_export")
        requireAuthPref?.isChecked = prefsRepository.getRequireAuthForExport()
        requireAuthPref?.setOnPreferenceChangeListener { _, newValue ->
            prefsRepository.setRequireAuthForExport(newValue as Boolean)
            Utility.showToast(
                requireContext(),
                if (newValue) "Authentication required for exports" else "Authentication not required for exports"
            )
            true
        }

        val encryptBackupsPref = findPreference<SwitchPreferenceCompat>("encrypt_backups")
        encryptBackupsPref?.isChecked = prefsRepository.getEncryptBackups()
        encryptBackupsPref?.setOnPreferenceChangeListener { _, newValue ->
            prefsRepository.setEncryptBackups(newValue as Boolean)
            Utility.showToast(
                requireContext(),
                if (newValue) "Backups will be encrypted" else "Backups will not be encrypted"
            )
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateLastBackupSummary()
        updateStorageInfoSummary()
    }

    override fun onDetach() {
        super.onDetach()
        settingsActivity = null
    }
}
