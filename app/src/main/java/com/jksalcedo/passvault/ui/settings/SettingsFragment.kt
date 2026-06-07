package com.jksalcedo.passvault.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.data.enums.Action
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.auth.BiometricAuthenticator
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.utils.Utility.formatFileSize
import com.jksalcedo.passvault.viewmodel.SettingsModelFactory
import com.jksalcedo.passvault.viewmodel.SettingsViewModel
import com.mikepenz.aboutlibraries.LibsBuilder
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
        SettingsModelFactory(application = requireActivity().application)
    }

    private val pickCrashLocationLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                try {
                    val takeFlags: Int =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)

                    prefsRepository.setCrashLogsLocation(it.toString())
                    updateCrashLogsLocationSummary()
                    Utility.showToast(requireContext(), "Crash logs location updated")
                } catch (e: Exception) {
                    Utility.showToast(
                        requireContext(),
                        "Failed to set crash logs location: ${e.message}"
                    )
                }
            }
        }

    @Suppress("DEPRECATION")
    private fun setupChangePassword() {
        findPreference<Preference>("change_master_password")?.setOnPreferenceClickListener {
            val layout = layoutInflater.inflate(R.layout.dialog_change_password, null)
            val etOld = layout.findViewById<TextInputEditText>(R.id.etOldPassword)
            val etNew = layout.findViewById<TextInputEditText>(R.id.etNewPassword)
            val etConfirm = layout.findViewById<TextInputEditText>(R.id.etConfirmNewPassword)
            val tilOld = layout.findViewById<TextInputLayout>(R.id.tilOldPassword)
            val tilNew = layout.findViewById<TextInputLayout>(R.id.tilNewPassword)
            val tilConfirm = layout.findViewById<TextInputLayout>(R.id.tilConfirmNewPassword)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Master Password")
                .setView(layout)
                .setPositiveButton("Save", null) // Overridden below
                .setNegativeButton("Cancel", null)
                .create().apply {
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val oldPass = etOld.text.toString()
                            val newPass = etNew.text.toString()
                            val confirmPass = etConfirm.text.toString()

                            tilOld.error = null
                            tilNew.error = null
                            tilConfirm.error = null

                            // Validate Old Password
                            val prefs =
                                requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                            val cipher = prefs.getString("pin_cipher", null)
                            val iv = prefs.getString("pin_iv", null)

                            val storedPin = try {
                                if (cipher != null && iv != null) {
                                    com.jksalcedo.passvault.crypto.Encryption.decrypt(cipher, iv)
                                } else {
                                    ""
                                }
                            } catch (e: Exception) {
                                ""
                            }

                            if (oldPass != storedPin) {
                                tilOld.error = "Incorrect password"
                                return@setOnClickListener
                            }

                            // Validate New Password
                            if (newPass.length < 4) {
                                tilNew.error = "Password must be at least 4 characters"
                                return@setOnClickListener
                            }

                            if (newPass != confirmPass) {
                                tilConfirm.error = "Passwords do not match"
                                return@setOnClickListener
                            }

                            // Save New Password
                            try {
                                val (newCipher, newIv) = com.jksalcedo.passvault.crypto.Encryption.encrypt(
                                    newPass
                                )
                                prefs.edit {
                                    putString("pin_cipher", newCipher)
                                    putString("pin_iv", newIv)
                                }
                                Utility.showToast(requireContext(), "Password changed successfully")
                                dismiss()
                            } catch (e: Exception) {
                                Utility.showToast(requireContext(), "Error saving password")
                            }
                        }
                    }
                }.show()
            true
        }
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
        val key = arguments?.getString(ARG_PREFERENCE_ROOT)

        when (key) {
            "pref_security" -> {
                setPreferencesFromResource(R.xml.settings_security, null)
                setupSecurityPreferences()
            }

            "pref_display" -> {
                setPreferencesFromResource(R.xml.settings_display, null)
                setupBottomAppBarPreference()
                setupThemePreference()
                setupDynamicColorsPreference()
            }

            "pref_data_sync" -> {
                setPreferencesFromResource(R.xml.settings_data_sync, null)
                setupExportAndImport()
                setupExportFormat()
                setupAutoBackups()
                setupManageBackups()
                setupLastBackup()
                setupStorageInfo()
                setupClearData()
                setupBackupLocation()
                setupBackupRetention()
                setupBackupCopies()
                setupBackupFilenameConfig()
                setupCrashHandlerLocation()
            }

            "pref_about" -> {
                setPreferencesFromResource(R.xml.settings_about, null)
                setupAboutPreference()
                setupGitHubLink()
                setupTelegramLink()
                setupOSSLicenses()
            }

            else -> {
                setPreferencesFromResource(R.xml.settings_root, rootKey)
            }
        }
    }

    private fun setupCrashHandlerLocation() {
        updateCrashLogsLocationSummary()
        findPreference<Preference>("crash_logs_location")?.setOnPreferenceClickListener {
            try {
                pickCrashLocationLauncher.launch(null)
            } catch (e: Exception) {
                Utility.showToast(
                    requireContext(),
                    "Error launching directory picker: ${e.message}"
                )
            }
            true
        }
    }

    private fun updateCrashLogsLocationSummary() {
        val uriString = prefsRepository.getCrashLogsLocation()
        val summary = if (uriString != null) {
            try {
                val uri = uriString.toUri()
                val documentFile =
                    androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), uri)
                documentFile?.name ?: uriString
            } catch (_: Exception) {
                uriString
            }
        } else {
            "Default: Internal App Storage"
        }
        findPreference<Preference>("crash_logs_location")?.summary = summary
    }

    @Suppress("DEPRECATION")
    private fun setupOSSLicenses() {
        findPreference<Preference>("licenses")?.setOnPreferenceClickListener {
            LibsBuilder()
                .withLicenseShown(true)
                .withActivityTitle("Open Source Licenses")
                .start(requireContext())
            true
        }
    }

    private fun setupGitHubLink() {
        findPreference<Preference>("github_link")?.setOnPreferenceClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, "https://github.com/jksalcedo/PassVault".toUri())
            startActivity(intent)
            true
        }
    }

    private fun setupTelegramLink() {
        findPreference<Preference>("telegram_link")?.setOnPreferenceClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, "https://t.me/passvault_app".toUri())
            startActivity(intent)
            true
        }
    }

    private fun setupThemePreference() {
        val themePref = findPreference<ListPreference>("app_theme")
        themePref?.setOnPreferenceChangeListener { _, newValue ->
            val oldTheme = prefsRepository.getTheme()
            val theme = newValue as String
            prefsRepository.setTheme(theme)

            // Apply theme immediately
            val mode = when (theme) {
                "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                "dark", "oled" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)

            if (theme == "oled" || oldTheme == "oled") {
                requireActivity().recreate()
            }
            true
        }
    }

    private fun setupDynamicColorsPreference() {
        val dynamicColorsPref = findPreference<SwitchPreferenceCompat>("use_dynamic_colors")

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            dynamicColorsPref?.isVisible = false
            return
        }

        dynamicColorsPref?.isChecked = prefsRepository.getUseDynamicColors()
        dynamicColorsPref?.setOnPreferenceChangeListener { _, newValue ->
            prefsRepository.setUseDynamicColors(newValue as Boolean)

            // Show dialog informing user that app needs to restart
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restart Required")
                .setMessage("The app needs to restart for this change to take effect.")
                .setPositiveButton("Restart Now") { _, _ ->
                    settingsActivity?.triggerRestart()
                }
                .setNegativeButton("Later", null)
                .show()

            true
        }
    }

    private fun setupBottomAppBarPreference() {
        val bottomAppBarPref = findPreference<SwitchPreferenceCompat>("use_bottom_app_bar")
        bottomAppBarPref?.isChecked = prefsRepository.getUseBottomAppBar()
        bottomAppBarPref?.setOnPreferenceChangeListener { _, newValue ->
            prefsRepository.setUseBottomAppBar(newValue as Boolean)

            // Show dialog informing user that app needs to restart
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restart Required")
                .setMessage("The app needs to restart for this change to take effect.")
                .setPositiveButton("Restart Now") { _, _ ->
                    settingsActivity?.triggerRestart()
                }
                .setNegativeButton("Later", null)
                .show()

            true
        }
    }

    private fun setupBackupCopies() {
        val copiesPref = findPreference<ListPreference>("backup_copies")
        copiesPref?.setOnPreferenceChangeListener { _, newValue ->
            val count = (newValue as String).toInt()
            prefsRepository.setBackupCopies(count)
            updateBackupCopiesSummary()
            true
        }
        updateBackupCopiesSummary()
    }

    private fun updateBackupCopiesSummary() {
        val count = prefsRepository.getBackupCopies()
        val summary = "$count Copies"
        findPreference<ListPreference>("backup_copies")?.summary = summary
    }

    /**
     * Navigates to the BackupsFragment when the "manage_backups" preference is clicked.
     */
    private fun setupManageBackups() {
        findPreference<Preference>("manage_backups")?.setOnPreferenceClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_container, BackupsFragment())
                .addToBackStack(null)
                .commit()
            true
        }

    }

    /**
     * Sets up the "about_app" preference to show the app version and navigate to the AboutFragment.
     */
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

    /**
     * Sets up the "export_data" and "import_data" preferences to handle import/export actions.
     */
    private fun setupExportAndImport() {
        findPreference<Preference>("export_data")?.setOnPreferenceClickListener {
            handleImportExportRequest(Action.EXPORT)
            true
        }
        findPreference<Preference>("import_data")?.setOnPreferenceClickListener {
            //settingsActivity?.openFileForImport()
            handleImportExportRequest(Action.IMPORT)
            true
        }
    }

    /**
     * Handles the import or export action, showing a biometric prompt if required.
     * @param action The action to perform (import or export).
     */
    private fun handleImportExportRequest(action: Action) {
        // Check if the "Require Auth" setting is enabled
        val requireAuth = prefsRepository.getRequireAuthForExport()

        if (requireAuth && BiometricAuthenticator.canAuthenticate(requireContext())) {
            //  show the prompt if required
            biometricAuthenticator.showBiometricPrompt(
                fragment = this,
                onSuccess = {
                    // proceed
                    when (action) {
                        Action.EXPORT -> settingsActivity?.createFileForExport()
                        Action.IMPORT -> ImportDialog().apply {
                            show(
                                settingsActivity!!.supportFragmentManager,
                                ImportDialog.TAG
                            )
                        }

                    }

                },
                onFailure = { _, _ -> }
            )
        } else {
            // proceed directly
            when (action) {
                Action.EXPORT -> settingsActivity?.createFileForExport()
                Action.IMPORT -> ImportDialog().apply {
                    show(
                        settingsActivity!!.supportFragmentManager,
                        ImportDialog.TAG
                    )
                }
            }
        }
    }

    /**
     * Sets up the "export_format" preference to allow changing the export format.
     */
    private fun setupExportFormat() {
        findPreference<ListPreference>("export_format")?.setOnPreferenceChangeListener { _, newValue ->
            val format = newValue as String
            prefsRepository.setExportFormat(format)
            Utility.showToast(requireContext(), "Export format set to: ${format.uppercase()}")
            true
        }
    }

    /**
     * Sets up the "auto_backups" preference, allowing the user to enable or disable automatic backups.
     */
    private fun setupAutoBackups() {
        val autoBackupPref = findPreference<SwitchPreferenceCompat>("auto_backups")
        autoBackupPref?.isChecked = prefsRepository.getAutoBackups()
        autoBackupPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            android.util.Log.d(
                "SettingsFragment",
                "Switch toggled. Calling setAutoBackups($enabled)"
            )
            if (!enabled) {
                viewModel.setAutoBackups(false)
                Utility.showToast(requireContext(), "Auto backups disabled")
                return@setOnPreferenceChangeListener true
            }

            val layout = layoutInflater.inflate(R.layout.dialog_set_password, null)
            val etPassword = layout.findViewById<TextInputEditText>(R.id.et_password)
            val etConfirmPassword = layout.findViewById<TextInputEditText>(R.id.et_confirm_password)
            val til1 = layout.findViewById<TextInputLayout>(R.id.til1)
            val til2 = layout.findViewById<TextInputLayout>(R.id.til2)

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set password for automatic backups")
                .setMessage("This will be needed when you import or restore automatic backup files.")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel") { _, _ ->
                    autoBackupPref.isChecked = false // revert switch
                }
                .create()

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val password = etPassword.text.toString()
                    val confirm = etConfirmPassword.text.toString()

                    when {
                        password.isBlank() -> {
                            til1.error = "Password cannot be empty"
                        }

                        password.length < 4 -> {
                            til1.error = "Password too short"
                        }

                        password != confirm -> {
                            til2.error = "Passwords do not match"
                        }

                        else -> {
                            prefsRepository.setPasswordForAutoBackups(password)
                            viewModel.setAutoBackups(true)
                            autoBackupPref.isChecked = true
                            Utility.showToast(requireContext(), "Auto backups enabled!")
                            updateLastBackupSummary()
                            dialog.dismiss()
                        }
                    }
                }
            }
            dialog.show()
            false
        }
    }

    /**
     * Sets up the "last_backup" preference to show the last backup time.
     */
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

    /**
     * Updates the summary of the "last_backup" preference with the last backup date.
     */
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

    /**
     * Sets up the "storage_info" preference to show storage usage details.
     */
    private fun setupStorageInfo() {
        updateStorageInfoSummary()
        findPreference<Preference>("storage_info")?.setOnPreferenceClickListener {
            val (dbSize, prefsSize) = viewModel.getStorageInfo()
            val totalSize = dbSize + prefsSize
            val message = "Database: ${formatFileSize(dbSize)}\n" +
                    "Preferences: ${formatFileSize(prefsSize)}\n" +
                    "Total: ${formatFileSize(totalSize)}"
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Storage Information")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
            true
        }
    }

    /**
     * Updates the summary of the "storage_info" preference with the total storage size.
     */
    private fun updateStorageInfoSummary() {
        val (dbSize, prefsSize) = viewModel.getStorageInfo()
        val totalSize = dbSize + prefsSize
        findPreference<Preference>("storage_info")?.summary = "Total: ${formatFileSize(totalSize)}"
    }

    /**
     * Sets up the "clear_data" preference to allow clearing all app data.
     */
    private fun setupClearData() {
        findPreference<Preference>("clear_data")?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
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

    /**
     * Sets up the security-related preferences, such as requiring authentication for exports and encrypting backups.
     */
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

        val blockScreenshotsPref = findPreference<SwitchPreferenceCompat>("block_screenshots")
        blockScreenshotsPref?.isChecked = prefsRepository.getBlockScreenshots()
        blockScreenshotsPref?.setOnPreferenceChangeListener { _, newValue ->
            prefsRepository.setBlockScreenshots(newValue as Boolean)

            // Show dialog informing user that app needs to restart
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restart Required")
                .setMessage("The app needs to restart for this change to take effect.")
                .setPositiveButton("Restart Now") { _, _ ->
                    settingsActivity?.triggerRestart()
                }
                .setNegativeButton("Later", null)
                .show()

            true
        }

        val biometricLoginPref = findPreference<SwitchPreferenceCompat>("biometric_login_enabled")
        biometricLoginPref?.isChecked = prefsRepository.getBiometricLoginEnabled()
        biometricLoginPref?.setOnPreferenceChangeListener { _, newValue ->
            prefsRepository.setBiometricLoginEnabled(newValue as Boolean)
            true
        }

        setupAutoLockTimeout()
        setupChangePassword()
        setupAutofillPreference()
    }

    private fun setupAutofillPreference() {
        val pref = findPreference<Preference>("autofill_service") ?: return
        updateAutofillSummary(pref)

        pref.setOnPreferenceClickListener {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
            try {
                startActivity(intent)
            } catch (_: Exception) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                } catch (_: Exception) { }
            }
            true
        }
    }

    private fun updateAutofillSummary(pref: Preference? = findPreference("autofill_service")) {
        pref ?: return
        val autofillManager = requireContext().getSystemService(android.view.autofill.AutofillManager::class.java)
        val enabled = autofillManager?.hasEnabledAutofillServices() == true
        pref.summary = getString(
            if (enabled) R.string.autofill_pref_summary_enabled
            else R.string.autofill_pref_summary_disabled
        )
    }

    private fun setupAutoLockTimeout() {
        val timeoutPref = findPreference<ListPreference>("auto_lock_timeout")
        timeoutPref?.setOnPreferenceChangeListener { _, newValue ->
            val timeout = (newValue as String).toLong()
            prefsRepository.setAutoLockTimeout(timeout)
            true
        }
    }

    private val pickBackupLocationLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                try {
                    val takeFlags: Int =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)

                    prefsRepository.setBackupLocation(it.toString())
                    updateBackupLocationSummary()
                    Utility.showToast(requireContext(), "Backup location updated")
                } catch (e: Exception) {
                    Utility.showToast(
                        requireContext(),
                        "Failed to set backup location: ${e.message}"
                    )
                }
            }
        }

    private fun setupBackupLocation() {
        updateBackupLocationSummary()
        findPreference<Preference>("backup_location")?.setOnPreferenceClickListener {
            try {
                pickBackupLocationLauncher.launch(null)
            } catch (e: Exception) {
                Utility.showToast(requireContext(), "Error launching file picker: ${e.message}")
            }
            true
        }
    }

    private fun setupBackupFilenameConfig() {
        findPreference<Preference>("backup_filename_config")?.setOnPreferenceClickListener {
            showBackupFilenameDialog()
            true
        }
    }

    private fun showBackupFilenameDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_backup_filename, null)
        val etPattern = layout.findViewById<TextInputEditText>(R.id.et_filename_pattern)
        val rgFormat = layout.findViewById<android.widget.RadioGroup>(R.id.rg_timestamp_format)
        val tvPreview = layout.findViewById<android.widget.TextView>(R.id.tv_preview)

        // Load current values
        etPattern.setText(prefsRepository.getBackupFileNameFormat())
        val currentFormat = prefsRepository.getBackupTimestampFormat()
        when (currentFormat) {
            "yyyy-MM-dd_HH-mm-ss" -> rgFormat.check(R.id.rb_readable)
            "yyyyMMdd_HHmmss" -> rgFormat.check(R.id.rb_compact)
            "yyyy-MM-dd" -> rgFormat.check(R.id.rb_date_only)
            else -> rgFormat.check(R.id.rb_readable)
        }

        // Preview update logic
        fun updatePreview() {
            val pattern = etPattern.text.toString()
            val formatId = rgFormat.checkedRadioButtonId
            val dateFormat = when (formatId) {
                R.id.rb_compact -> "yyyyMMdd_HHmmss"
                R.id.rb_date_only -> "yyyy-MM-dd"
                else -> "yyyy-MM-dd_HH-mm-ss"
            }
            val timestamp = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date())
            val filename = pattern.replace("{timestamp}", timestamp)
            val ext = prefsRepository.getExportFormat()
            tvPreview.text = "$filename.$ext"
        }

        etPattern.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        rgFormat.setOnCheckedChangeListener { _, _ -> updatePreview() }

        // Initial preview
        updatePreview()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Backup Filename")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val pattern = etPattern.text.toString()
                val formatId = rgFormat.checkedRadioButtonId
                val dateFormat = when (formatId) {
                    R.id.rb_compact -> "yyyyMMdd_HHmmss"
                    R.id.rb_date_only -> "yyyy-MM-dd"
                    else -> "yyyy-MM-dd_HH-mm-ss"
                }

                if (pattern.isBlank()) {
                    Utility.showToast(requireContext(), "Filename pattern cannot be empty")
                    return@setPositiveButton
                }

                prefsRepository.setBackupFileNameFormat(pattern)
                prefsRepository.setBackupTimestampFormat(dateFormat)
                Utility.showToast(requireContext(), "Backup filename settings saved")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBackupLocationSummary() {
        val uriString = prefsRepository.getBackupLocation()
        val summary = if (uriString != null) {
            try {
                val uri = uriString.toUri()
                val documentFile =
                    androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), uri)
                documentFile?.name ?: uriString
            } catch (_: Exception) {
                uriString
            }
        } else {
            "Default: Internal App Storage"
        }
        findPreference<Preference>("backup_location")?.summary = summary
    }

    private fun setupBackupRetention() {
        val retentionPref = findPreference<EditTextPreference>("backup_retention")
        retentionPref?.setOnPreferenceChangeListener { _, newValue ->
            val count = (newValue as String).toInt()
            prefsRepository.setBackupRetention(count)
            updateBackupRetentionSummary()
            true
        }
        updateBackupRetentionSummary()
    }

    private fun setupShareCrashLogs() {
        val sharePref = findPreference<Preference>("share_crash_logs")
        sharePref?.setOnPreferenceClickListener {
            val crashLocationUri = prefsRepository.getCrashLogsLocation()
            val filesToZip = mutableListOf<java.io.File>()

            if (crashLocationUri != null) {
                // Custom location (SAF)
                try {
                    val treeUri = crashLocationUri.toUri()
                    val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                        requireContext(),
                        treeUri
                    )
                    pickedDir?.listFiles()?.forEach { file ->
                        if (file.name?.startsWith("PV_Crash") == true && file.name?.endsWith(".txt") == true) {
                            // Copy to cache
                            val cacheFile = java.io.File(requireContext().cacheDir, file.name!!)
                            requireContext().contentResolver.openInputStream(file.uri)
                                ?.use { input ->
                                    java.io.FileOutputStream(cacheFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            filesToZip.add(cacheFile)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                // Default location
                val crashLogDir =
                    java.io.File(requireContext().getExternalFilesDir(null), "crash_logs")
                if (crashLogDir.exists()) {
                    crashLogDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("PV_Crash") && file.name.endsWith(".txt")) {
                            filesToZip.add(file)
                        }
                    }
                }
            }

            if (filesToZip.isNotEmpty()) {
                val zipFile = java.io.File(requireContext().cacheDir, "crash_logs.zip")
                if (com.jksalcedo.passvault.utils.Utility.zipFiles(filesToZip, zipFile)) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        zipFile
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                    intent.type = "application/zip"
                    intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(android.content.Intent.createChooser(intent, "Share Crash Logs"))
                } else {
                    com.jksalcedo.passvault.utils.Utility.showToast(
                        requireContext(),
                        "Failed to create zip file"
                    )
                }
            } else {
                com.jksalcedo.passvault.utils.Utility.showToast(
                    requireContext(),
                    "No crash logs found"
                )
            }
            true
        }
    }

    private fun updateBackupRetentionSummary() {
        val count = prefsRepository.getBackupRetention()
        val summary = if (count == -1) "Unlimited" else "$count Backups"
        findPreference<EditTextPreference>("backup_retention")?.summary = summary
    }

    override fun onResume() {
        super.onResume()
        updateLastBackupSummary()
        updateStorageInfoSummary()
        updateBackupLocationSummary()
        updateBackupRetentionSummary()
        updateBackupCopiesSummary()
        updateCrashLogsLocationSummary()
        updateAutofillSummary()
        setupShareCrashLogs()
    }

    override fun onDetach() {
        super.onDetach()
        settingsActivity = null
    }
}
