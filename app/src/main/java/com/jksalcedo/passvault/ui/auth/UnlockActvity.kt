package com.jksalcedo.passvault.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.databinding.ActivityUnlockBinding
import com.jksalcedo.passvault.ui.main.MainActivity

class UnlockActivity : AppCompatActivity(), SetPinFragment.OnPinSetListener {

    private lateinit var binding: ActivityUnlockBinding

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure encryption key exists early in the session
        try {
            Encryption.ensureKeyExists()
        } catch (_: Exception) {
        }

        val prefs = getSharedPreferences("auth", MODE_PRIVATE)

        val input = binding.etPin.text?.toString()?.trim().orEmpty()
        val cipher = prefs.getString("pin_cipher", null)
        val iv = prefs.getString("pin_iv", null)

        // Decrypt the stored cipher and iv
        val storedPin = try {
            Encryption.decrypt(cipher!!, iv!!)
        } catch (_: Exception) {
            ""
        }

        // Setup biometrics if available and a PIN exists
        setupBiometricIfAvailable(storedPin.isNotEmpty())

        binding.btnUnlock.setOnClickListener {
            // Compare the decrypted pin to the input pin
            if (input.isNotEmpty() && storedPin == input) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.pin_incorrect), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBiometricIfAvailable(hasPin: Boolean) {
        if (!hasPin) {
            binding.btnUseBiometric.visibility = android.view.View.GONE
            return
        }
        val biometricManager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.btnUseBiometric.visibility = android.view.View.VISIBLE
            binding.btnUseBiometric.setOnClickListener { showBiometricPrompt(authenticators) }
        } else {
            binding.btnUseBiometric.visibility = android.view.View.GONE
        }
    }

    private fun showBiometricPrompt(authenticators: Int) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_CANCELED) {
                        Toast.makeText(this@UnlockActivity, errString, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@UnlockActivity, MainActivity::class.java))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@UnlockActivity,
                        getString(R.string.biometric_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_negative))
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun onPinSet(pin: String) {
        Toast.makeText(this, getString(R.string.pin_configured), Toast.LENGTH_SHORT).show()
        // After setting PIN, re-evaluate biometric availability
        setupBiometricIfAvailable(true)
    }
}

