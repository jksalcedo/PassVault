package com.jksalcedo.passvault.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.auth.UnlockActivity
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.viewmodel.SettingsModelFactory
import com.jksalcedo.passvault.viewmodel.SettingsViewModel
import com.jksalcedo.passvault.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * An activity for managing app settings.
 */
class SettingsActivity : BaseActivity(), androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsModelFactory(application = this.application)
    }

    private val preferenceRepository by lazy { PreferenceRepository(application) }

    var password: String? = null
    private var loadingDialog: androidx.appcompat.app.AlertDialog? = null

    // Launcher for creating (exporting) a file
    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.exportEntries(
                        uri,
                        password = password
                    )
                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(2000)
                        password = null
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // Show Preference Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_fragment_container, SettingsFragment())
            .commit()

        //settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        observeViewModelResults()
    }

    /**
     * Observes the results of the view model's operations.
     */
    private fun observeViewModelResults() {
        // Observe export UI state
        settingsViewModel.exportUiState.observe(this) { state ->
            when (state) {
                is com.jksalcedo.passvault.ui.settings.ExportUiState.Idle -> {
                    // Do nothing
                }
                is com.jksalcedo.passvault.ui.settings.ExportUiState.Loading -> {
                    if (loadingDialog?.isShowing != true) {
                        loadingDialog = MaterialAlertDialogBuilder(this)
                            .setTitle("Exporting Data")
                            .setMessage("Please wait...")
                            .setCancelable(false)
                            .create()
                        loadingDialog?.show()
                    }
                    loadingDialog?.setMessage("Exporting ${state.progress} of ${state.total} entries...")
                }
                is com.jksalcedo.passvault.ui.settings.ExportUiState.Success -> {
                    loadingDialog?.dismiss()
                    val exportResult = state.result
                    when {
                        exportResult.allSucceeded -> {
                            Toast.makeText(
                                this,
                                "Export successful! ${exportResult.successCount} entries exported.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        exportResult.hasFailures -> {
                            // Show detailed dialog about partial success
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Export Partially Successful")
                                .setMessage(
                                    "Successfully exported ${exportResult.successCount} of ${exportResult.totalCount} entries.\n\n" +
                                            "${exportResult.failedEntries.size} entries failed to export:\n" +
                                            exportResult.failedEntries.joinToString("\n") { "• $it" } +
                                            "\n\nThese entries may have been encrypted with an invalid keystore key."
                                )
                                .setPositiveButton("OK", null)
                                .show()
                        }

                        else -> {
                            Toast.makeText(
                                this,
                                "Export completed with ${exportResult.successCount} entries.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    settingsViewModel.resetExportState()
                }
                is com.jksalcedo.passvault.ui.settings.ExportUiState.Error -> {
                    loadingDialog?.dismiss()
                    val error = state.exception
                    android.util.Log.e("ExportError", "Export failed unexpectedly", error)

                    // Check if it's a keystore validation error
                    val message = if (error.message?.contains("Keystore key is invalid") == true) {
                        "Export failed: Android Keystore key is invalid.\n\n" +
                                "This usually happens when:\n" +
                                "• Device security settings changed\n" +
                                "• App was reinstalled\n" +
                                "• Keystore was cleared\n\n" +
                                "Unfortunately, encrypted passwords cannot be recovered without the original key."
                    } else {
                        "Export failed: ${error.message}"
                    }

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Export Failed")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                    settingsViewModel.resetExportState()
                }
            }
        }

        // Observe keystore validation result
        settingsViewModel.keystoreValidationResult.observe(this) { isValid ->
            if (isValid == false) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Keystore Warning")
                    .setMessage(
                        "The Android Keystore key appears to be invalid.\n\n" +
                                "This may cause export failures. Some or all passwords may not be recoverable.\n\n" +
                                "Do you want to continue with the export?"
                    )
                    .setPositiveButton("Continue Anyway") { _, _ ->
                        // User chose to continue
                    }
                    .setNegativeButton("Cancel", null)
                    .setOnDismissListener {
                        settingsViewModel.resetKeystoreValidation()
                    }
                    .show()
            }
        }

        settingsViewModel.importResult.observe(this) { result ->
            result.fold(
                onSuccess = { count ->
                    Toast.makeText(
                        this,
                        "$count entries imported successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this,
                        "Import failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    error.printStackTrace() // For debugging
                }
            )
        }

        settingsViewModel.restartAppEvent.observe(this) { event ->
            if (event != null) {
                triggerRestart()
            }
        }
    }

    /**
     * Triggers a restart of the app.
     */
    fun triggerRestart() {
        val intent = Intent(this, UnlockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.startActivity(intent)
        this.finish()
        kotlin.system.exitProcess(0)
    }

    /**
     * Ensures that a password exists before performing an action.
     * @param isImporting True if importing, false if exporting.
     * @param onPasswordReady A callback to be invoked when the password is ready.
     */
    fun ensurePasswordExists(isImporting: Boolean, onPasswordReady: (String) -> Unit) {
        if (password == null) {
            // No password found
            val layoutResource =
                if (isImporting) R.layout.dialog_enter_password else R.layout.dialog_set_password
            val layout = layoutInflater.inflate(layoutResource, null)
            val title =
                if (isImporting) R.string.enter_backup_file_password else R.string.set_export_password
            val message =
                if (isImporting) "Enter the password to decrypt this backup file. \n " +
                        "If the file is not encrypted, leave this empty and proceed." else "Create a password to encrypt your backup."
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(layout)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val etPassword = layout.findViewById<TextInputEditText>(R.id.et_password)
                    val etConfirmPassword =
                        layout.findViewById(R.id.et_confirm_password) ?: TextInputEditText(this)
                    val newPassword = etPassword.text.toString().trim()
                    val confirmPassword = etConfirmPassword.text ?: ""
                    val til2 = layout.findViewById<TextInputLayout>(R.id.til2)

                    when {
//                        newPassword.isBlank() -> {
//                            til1.error = "Password cannot be empty"
//                        }

                        newPassword.isNotEmpty() && isImporting -> {
                            password = newPassword
                            dialog.dismiss()
                            onPasswordReady(newPassword)
                            lifecycleScope.launch {
                                delay(1000)
                                password = null
                            }
                        }

                        newPassword != confirmPassword.toString().trim() -> {
                            til2.error = "Password do not match"
                        }

                        else -> {
                            password = newPassword
                            if (!isImporting) Utility.showToast(
                                this,
                                getString(R.string.password_saved)
                            )
                            dialog.dismiss()
                            onPasswordReady(newPassword) // Proceed
                        }
                    }
                }
            }
            dialog.show()
        }
    }

    /**
     * Creates a file for export.
     */
    fun createFileForExport() {
        val timestampFormat = preferenceRepository.getBackupTimestampFormat()
        val filenameFormat = preferenceRepository.getBackupFileNameFormat()
        
        val formatter = SimpleDateFormat(timestampFormat, Locale.getDefault())
        val exportFormat = preferenceRepository.getExportFormat()
        val timestamp = formatter.format(Date())
        val fileName = "${filenameFormat.replace("{timestamp}", timestamp)}.$exportFormat"

        val mimeType = when (exportFormat.lowercase(Locale.ROOT)) {
            "csv", "chrome_csv" -> "text/csv"
            "kdbx" -> "application/x-keepass2"
            else -> "application/json"
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        if (exportFormat.lowercase(Locale.ROOT) == "kdbx" || preferenceRepository.getEncryptBackups()) {
            ensurePasswordExists(false) {
                createFileLauncher.launch(intent)
            }
        } else {
            val format = preferenceRepository.getExportFormat()
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Warning: Unencrypted export")
                .setMessage(
                    "This export contain your passwords in plain $format. Anyone with access to this file can read your passwords. " +
                            "Do not share it or save it to cloud storage. Are you sure?"
                )
                .setPositiveButton("Proceed") { _, _ -> }
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .create()

            dialog.setOnShowListener {
                val button = dialog.getButton(Dialog.BUTTON_POSITIVE)
                button.isEnabled = false
                lifecycleScope.launch {
                    button.text = buildString {
                        append("(3) Proceed")
                    }
                    delay(1000)
                    button.text = buildString {
                        append("(2) Proceed")
                    }
                    delay(1000)
                    button.text = buildString {
                        append("(1) Proceed")
                    }
                    delay(1000)
                    button.text = buildString {
                        append("Proceed")
                    }
                    button.isEnabled = true
                }
                button.setOnClickListener {
                    createFileLauncher.launch(intent)
                    dialog.dismiss()
                }
            }
            dialog.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onPreferenceStartFragment(
        caller: androidx.preference.PreferenceFragmentCompat,
        pref: androidx.preference.Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        args.putString(androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!
        )
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }
}