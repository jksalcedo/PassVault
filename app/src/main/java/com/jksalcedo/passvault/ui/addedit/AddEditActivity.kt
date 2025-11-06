package com.jksalcedo.passvault.ui.addedit

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

class AddEditActivity : AppCompatActivity(), PasswordGenDialog.PasswordGeneratedListener {
    private lateinit var binding: ActivityAddEditBinding
    private lateinit var viewModel: PasswordViewModel
    var password: String = ""

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        val et = binding.etPassword
        et.text = Editable.Factory.getInstance().newEditable((password.ifEmpty { "" }))

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val rawPassword = binding.etPassword.text.toString()

            if (title.isEmpty() || rawPassword.isEmpty()) {
                Toast.makeText(this, "Title and password cannot be empty", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            try {
                // Ensure key exists and encrypt password
                Encryption.ensureKeyExists()
                val (cipherText, iv) = Encryption.encrypt(rawPassword)

                val entry = PasswordEntry(
                    title = title,
                    username = binding.etUsername.text.toString(),
                    passwordCipher = cipherText,
                    passwordIv = iv,
                    notes = binding.etNotes.text.toString(),
                    updatedAt = System.currentTimeMillis()
                )

                viewModel.insert(entry)
                finish()
            } catch (_: Exception) {
                Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show()
            }
        }

        // password generator custom dialog
        binding.cardGeneratePassword.setOnClickListener {
            val dialog = PasswordGenDialog()
            dialog.setPasswordGeneratedListener(this)
            dialog.show(supportFragmentManager, null)
        }

        binding.switchShowPassword.setOnCheckedChangeListener { _, isChecked ->
            // hide/show password
            et.transformationMethod =
                if (isChecked) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            // move cursor to the end
            et.setSelection(et.text?.length ?: 0)

        }
    }

    override fun onPasswordGenerated(password: String) {
        this.password = password
        binding.etPassword.text = Editable.Factory.getInstance().newEditable(password)
    }
}
