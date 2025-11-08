package com.jksalcedo.passvault.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.viewmodel.SettingsModelFactory
import com.jksalcedo.passvault.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    //private lateinit var settingsViewModel: SettingsViewModel
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsModelFactory(application = this.application, this)
    }

    // Launcher for creating (exporting) a file
    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.exportEntries(uri)
                }
            }
        }

    // Launcher for opening (importing) a file
    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.importEntries(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Show Preference Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_fragment_container, SettingsFragment())
            .commit()

        //settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        observeViewModelResults()
    }

    private fun observeViewModelResults() {
        // Observe export result
        settingsViewModel.exportResult.observe(this) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    android.util.Log.e("ExportError", "Export failed unexpectedly", error)
                    Toast.makeText(
                        this,
                        "Export failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
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
    }

    private fun ensurePasswordExists(onPasskeyReady: () -> Unit) {
        val sharedPrefs = getSharedPreferences("settings", MODE_PRIVATE)
        val storedPasskey = sharedPrefs.getString("passkey", null)

        if (storedPasskey.isNullOrEmpty()) {
            // No passkey found
            val layout = layoutInflater.inflate(R.layout.dialog_passkey, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(layout)
                .setCancelable(false)
                .setTitle(R.string.set_passkey)
                .setMessage("Create a password to encrypt your backup.")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val passkeyEditText = layout.findViewById<TextInputEditText>(R.id.etPasskey)
                    val confirmPasskeyEditText =
                        layout.findViewById<TextInputEditText>(R.id.etConfirmPasskey)
                    val newPasskey = passkeyEditText.text.toString()
                    val confirmPasskey = confirmPasskeyEditText.text.toString()

                    when {
                        newPasskey.isBlank() -> {
                            passkeyEditText.error = "Password cannot be empty"
                        }

                        newPasskey != confirmPasskey -> {
                            confirmPasskeyEditText.error = "Password do not match"
                        }

                        else -> {
                            // Passkeys match and not empty, save it
                            sharedPrefs.edit().putString("passkey", newPasskey).apply()
                            Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            onPasskeyReady() // Proceed
                        }
                    }
                }
            }
            dialog.show()
        } else {
            // Passkey already exists, proceed
            onPasskeyReady()
        }
    }

    fun createFileForExport() {
        ensurePasswordExists {
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val exportFormat = settingsViewModel.getExportFormat()
            val fileName = "passvault_backup_${formatter.format(Date())}.$exportFormat"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            createFileLauncher.launch(intent)
        }
    }

    fun openFileForImport() {
        ensurePasswordExists {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Allow selection of any file type for import
            }
            openFileLauncher.launch(intent)
        }
    }
}