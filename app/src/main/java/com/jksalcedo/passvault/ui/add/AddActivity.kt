package com.jksalcedo.passvault.ui.add

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.databinding.ActivityAddEditBinding
import com.jksalcedo.passvault.viewmodel.PasswordViewModel

class AddActivity : AppCompatActivity(), PasswordDialogListener {
    private lateinit var binding: ActivityAddEditBinding
    private lateinit var viewModel: PasswordViewModel
    private lateinit var etPassword: TextInputEditText

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        etPassword = binding.etPassword

        binding.btnSave.setOnClickListener {
            val rawPassword = binding.etPassword.text.toString()

            try {
                // Ensure key exists and encrypt password
                Encryption.ensureKeyExists()
                val (cipherText, iv) = Encryption.encrypt(rawPassword)

                val entry = PasswordEntry(
                    title = binding.etTitle.text.toString(),
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
            PasswordGenDialog().show(supportFragmentManager, "PasswordGenDialog")
        }

        binding.switchShowPassword.setOnCheckedChangeListener { _, isChecked ->
            // hide/show password
            etPassword.transformationMethod =
                if (isChecked) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            // move cursor to the end
            etPassword.setSelection(etPassword.text?.length ?: 0)

        }
    }

    override fun onPasswordGenerated(password: String) {
        etPassword.text = Editable.Factory.getInstance().newEditable((password.ifEmpty { "" }))
    }
}
