package com.jksalcedo.passvault.ui.addedit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.databinding.ActivityAddEditBinding
import com.jksalcedo.passvault.viewmodel.PasswordViewModel

class AddEditActivity : AppCompatActivity(), PasswordDialogListener {
    private lateinit var binding: ActivityAddEditBinding
    private lateinit var viewModel: PasswordViewModel
    private var currentEntry: PasswordEntry? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        val entryFromIntent: PasswordEntry? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_ENTRY, PasswordEntry::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_ENTRY)
            }

        if (entryFromIntent != null) {
            // If the full object is passed, use it directly
            currentEntry = entryFromIntent
            populateUi(entryFromIntent)
            binding.toolbar.title = "Edit Password"
        } else if (intent.hasExtra(EXTRA_ID)) {
            // Fallback for when only the ID is provided.
            val id = intent.getLongExtra(EXTRA_ID, -1)
            viewModel.getEntryById(id).observe(this) { entry ->
                entry?.let {
                    currentEntry = it
                    populateUi(it)
                }
            }
            binding.toolbar.title = "Edit Password"
        }

        binding.btnSave.setOnClickListener {
            saveEntry()
        }

        // password generator custom dialog
        binding.cardGeneratePassword.setOnClickListener {
            PasswordGenDialog().show(supportFragmentManager, "PasswordGenDialog")
        }

        binding.switchShowPassword.setOnCheckedChangeListener { _, isChecked ->
            // hide/show password
            binding.etPassword.transformationMethod =
                if (isChecked) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            // move cursor to the end
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }
    }

    private fun populateUi(entry: PasswordEntry) {
        binding.etTitle.setText(entry.title)
        binding.etUsername.setText(entry.username)
        binding.etNotes.setText(entry.notes)

        // Decrypt and set password
        try {
            val decryptedPassword = Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
            binding.etPassword.setText(decryptedPassword)
        } catch (_: Exception) {
            binding.etPassword.setText("")
            Toast.makeText(this, "Failed to decrypt password", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun saveEntry() {
        val title = binding.etTitle.text.toString()
        val username = binding.etUsername.text.toString()
        val rawPassword = binding.etPassword.text.toString()
        val notes = binding.etNotes.text.toString()

        if (title.isEmpty()) {
            binding.etTitle.error = "Title cannot be empty!"
            return
        }
        if (rawPassword.isEmpty()) {
            binding.etPassword.error = "Password cannot be empty!"
            return
        }
        if (username.isEmpty()) {
            binding.etUsername.error = "Username cannot be empty!"
            return
        }

        try {
            Encryption.ensureKeyExists()
            val (cipherText, iv) = Encryption.encrypt(rawPassword)

            val entry = currentEntry?.copy(
                title = title,
                username = username,
                passwordCipher = cipherText,
                passwordIv = iv,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            ) ?: PasswordEntry(
                title = title,
                username = username,
                passwordCipher = cipherText,
                passwordIv = iv,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )

            if (currentEntry == null) {
                viewModel.insert(entry)
            } else {
                viewModel.update(entry)
            }
            finish()
        } catch (_: Exception) {
            Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPasswordGenerated(password: String) {
        binding.etPassword.text = Editable.Factory.getInstance().newEditable((password.ifEmpty { "" }))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_ENTRY = " extra_entry"

        fun createIntent(context: Context, entry: PasswordEntry? = null): Intent {
            return Intent(context, AddEditActivity::class.java).apply {
                entry?.let {
                    putExtra(EXTRA_ID, it.id)
                    putExtra(EXTRA_ENTRY, it)
                }
            }
        }
    }
}
