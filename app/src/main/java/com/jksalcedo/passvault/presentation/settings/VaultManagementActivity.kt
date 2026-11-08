package com.jksalcedo.passvault.presentation.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.text.htmlEncode
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.ui.base.BaseActivity
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.presentation.auth.UnlockActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * A standalone activity for Vault Management actions (import, export, print, clear).
 * Accessible directly from the main action bar overflow menu.
 */
class VaultManagementActivity : BaseActivity(), VaultActionHost {

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsModelFactory(application = this.application)
    }

    private val preferenceRepository by lazy {
        (application as com.jksalcedo.passvault.Application).container.preferenceRepository
    }

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
                        delay(2000.milliseconds)
                        password = null
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault_management)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val args = Bundle().apply {
                putString(
                    androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT,
                    "pref_vault_management"
                )
            }
            val fragment = SettingsFragment().apply { arguments = args }
            supportFragmentManager.beginTransaction()
                .replace(R.id.vault_management_fragment_container, fragment)
                .commit()
        }

        observeViewModelResults()
    }

    /**
     * Observes the results of the view model's operations.
     */
    private fun observeViewModelResults() {
        // Observe export UI state
        settingsViewModel.exportUiState.observe(this) { state ->
            when (state) {
                is ExportUiState.Idle -> {
                    // Do nothing
                }

                is ExportUiState.Loading -> {
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

                is ExportUiState.Success -> {
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

                is ExportUiState.Error -> {
                    loadingDialog?.dismiss()
                    val error = state.exception
                    android.util.Log.e("ExportError", "Export failed unexpectedly", error)

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
                    .setPositiveButton("Continue Anyway") { _, _ -> }
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
                    error.printStackTrace()
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
    override fun triggerRestart() {
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
    override fun ensurePasswordExists(isImporting: Boolean, onPasswordReady: (String) -> Unit) {
        if (password == null) {
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
                        newPassword.isNotEmpty() && isImporting -> {
                            password = newPassword
                            dialog.dismiss()
                            onPasswordReady(newPassword)
                            lifecycleScope.launch {
                                delay(1000.milliseconds)
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
                            onPasswordReady(newPassword)
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
    override fun createFileForExport() {
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
                    button.text = buildString { append("(3) Proceed") }
                    delay(1000.milliseconds)
                    button.text = buildString { append("(2) Proceed") }
                    delay(1000.milliseconds)
                    button.text = buildString { append("(1) Proceed") }
                    delay(1000.milliseconds)
                    button.text = buildString { append("Proceed") }
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

    /**
     * Printing vault data using Android PrintManager & WebView.
     */
    override fun printVaultData() {
        lifecycleScope.launch {
            val entries = settingsViewModel.getAllEntriesForPrint()
            if (entries.isEmpty()) {
                Toast.makeText(
                    this@VaultManagementActivity,
                    "No entries available to print",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            printEntriesWithWebView(entries)
        }
    }

    private fun printEntriesWithWebView(entries: List<com.jksalcedo.passvault.data.local.entity.PasswordEntry>) {
        val htmlContent = buildPrintHtml(entries)
        val webView = android.webkit.WebView(this)
        val timestampFormat = preferenceRepository.getBackupTimestampFormat()
        val formatter = SimpleDateFormat(timestampFormat, Locale.getDefault())
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView, url: String) {
                val printManager = getSystemService(PRINT_SERVICE) as? android.print.PrintManager
                val jobName =
                    "${getString(R.string.app_name)}_passwords-${formatter.format(Date())}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    android.print.PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun buildPrintHtml(entries: List<com.jksalcedo.passvault.data.local.entity.PasswordEntry>): String {
        val dateFormat = SimpleDateFormat.getDateTimeInstance()
        val formattedDate = dateFormat.format(Date())

        val rows = StringBuilder()
        for ((_, title1, username1, passwordCipher, passwordIv, notes1, email, url1, category1) in entries) {
            val title = title1.htmlEncode()
            val category = (category1 ?: "").htmlEncode()
            val username = (username1 ?: email ?: "").htmlEncode()
            val decryptedPassword = try {
                if (passwordCipher.isNotEmpty()) {
                    Encryption.decrypt(passwordCipher, passwordIv)
                } else ""
            } catch (_: Exception) {
                ""
            }
            val password = decryptedPassword.htmlEncode()
            val url = (url1 ?: "").htmlEncode()
            val notes = (notes1 ?: "").htmlEncode()

            rows.append(
                """
                <tr>
                    <td>$title</td>
                    <td>$category</td>
                    <td>$username</td>
                    <td>$password</td>
                    <td>$url</td>
                    <td>$notes</td>
                </tr>
                """.trimIndent()
            )
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="author" content="${getString(R.string.app_name)}">
                <title>PassVault Passwords</title>
                <style>
                    body { font-family: sans-serif; margin: 20px; color: #111; }
                    h1 { text-align: center; font-size: 20px; margin-bottom: 4px; }
                    .meta { text-align: center; font-size: 12px; color: #666; margin-bottom: 16px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                    th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; font-size: 11px; word-break: break-word; }
                    th { background-color: #f2f2f2; font-weight: bold; }
                    tr:nth-child(even) { background-color: #fafafa; }
                    @media print {
                        tr { page-break-inside: avoid; }
                    }
                </style>
            </head>
            <body>
                <h1>PassVault Passwords</h1>
                <div class="meta">Exported on: $formattedDate | Total Entries: ${entries.size}</div>
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Category</th>
                            <th>Username / Email</th>
                            <th>Password</th>
                            <th>URL</th>
                            <th>Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
