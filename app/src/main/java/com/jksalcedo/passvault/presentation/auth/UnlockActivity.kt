package com.jksalcedo.passvault.presentation.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.autofill.PassVaultAutofillService
import com.jksalcedo.passvault.core.ui.base.BaseActivity
import com.jksalcedo.passvault.core.util.SessionManager
import com.jksalcedo.passvault.databinding.ActivityUnlockBinding
import com.jksalcedo.passvault.presentation.main.MainActivity

/**
 * An activity for unlocking the app.
 */
class UnlockActivity : BaseActivity(), SetPinFragment.OnPinSetListener {

    lateinit var binding: ActivityUnlockBinding
    private val biometricAuthenticator = BiometricAuthenticator()
    private lateinit var viewModel: UnlockViewModel
    private var lockoutTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UnlockViewModel::class.java]

        // Ensure encryption key exists early in the session
        try {
            com.jksalcedo.passvault.core.crypto.Encryption.ensureKeyExists()
        } catch (_: Exception) {
        }

        val hasPin = viewModel.hasPinConfigured

        if (!hasPin) {
            val setPinFragment = SetPinFragment()
            binding.clMain.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
            this.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, setPinFragment)
                .commitNow()
        } else {
            // Validate that the keystore can still decrypt the stored PIN
            if (!viewModel.isKeystoreValid()) {
                showKeystoreInvalidDialog()
                return
            }

            binding.clMain.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            if (viewModel.biometricLoginEnabled) {
                biometricAuthenticator.showBiometricPrompt(
                    activity = this,
                    onSuccess = {
                        navigateToNextScreen()
                    },
                    onFailure = { _, errString ->
                        Toast.makeText(this, errString, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Check for lockout
        if (viewModel.isLockedOut()) {
            startLockoutTimer()
        } else {
            // Setup biometrics if available and a PIN exists
            setupBiometricIfAvailable(hasPin)
        }

        viewModel.resetResult.observe(this) { success ->
            if (success == null) return@observe
            viewModel.resetResetResult()
            if (success) {
                recreate()
            } else {
                Toast.makeText(this, "Error clearing data.", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnUnlock.setOnClickListener {
            if (viewModel.isLockedOut()) return@setOnClickListener

            // Get the input
            val input = binding.etPin.text?.toString()?.trim().orEmpty()

            when (val result = viewModel.verifyPin(input)) {
                is UnlockResult.Success -> navigateToNextScreen()

                is UnlockResult.KeystoreInvalid -> showKeystoreInvalidDialog()

                is UnlockResult.LockedOut -> {
                    startLockoutTimer()
                    Toast.makeText(
                        this,
                        "Too many failed attempts. App locked.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is UnlockResult.Incorrect -> {
                    binding.mtPinMessage.text =
                        buildString {
                            append(getString(R.string.pin_incorrect))
                            append(". ${result.remainingAttempts} attempts remaining.")
                        }
                }
            }
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun navigateToNextScreen() {
        SessionManager.setUnlocked()
        viewModel.onUnlocked()

        if (intent.getBooleanExtra(PassVaultAutofillService.EXTRA_AUTOFILL_AUTH, false)) {
            setResult(RESULT_OK)
            finish()
            return
        }

        // Check if there is a pending intent passed from BaseActivity
        val redirectIntent =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("EXTRA_REDIRECT_INTENT", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("EXTRA_REDIRECT_INTENT")
            }

        if (redirectIntent != null) {
            redirectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(redirectIntent)
        } else {
            val homeIntent = Intent(this, MainActivity::class.java)
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(homeIntent)
        }

        finish()
    }

    /**
     * Sets up the biometric authentication if it is available.
     * @param hasPin True if a PIN has been set, false otherwise.
     */
    private fun setupBiometricIfAvailable(hasPin: Boolean) {
        if (!hasPin || !viewModel.biometricLoginEnabled) {
            binding.btnUseBiometric.visibility = View.GONE
            return
        }
        val biometricManager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.btnUseBiometric.visibility = View.VISIBLE
            binding.btnUseBiometric.setOnClickListener {
                biometricAuthenticator.showBiometricPrompt(
                    activity = this,
                    onSuccess = {
                        navigateToNextScreen()
                    },
                    onFailure = { _, errString ->
                        Toast.makeText(this, errString, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            binding.btnUseBiometric.visibility = View.GONE
        }
    }

    override fun onRestart() {
        super.onRestart()
        binding.clMain.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
    }

    override fun onPinSet(pin: String) {
        // After setting PIN, re-evaluate biometric availability
        supportFragmentManager.popBackStack()
        binding.clMain.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
        setupBiometricIfAvailable(true)
    }

    private fun showKeystoreInvalidDialog() {
        binding.clMain.visibility = View.GONE
        MaterialAlertDialogBuilder(this)
            .setTitle("Security Key Invalidated")
            .setMessage(
                "Your device's security key has been invalidated, likely due to a change in your " +
                        "device lock screen or biometrics.\n\n" +
                        "Your stored master password can no longer be verified. " +
                        "You must reset the app to continue.\n\n" +
                        "WARNING: This will delete all your stored passwords using the old key. " +
                        "You can restore your data from a backup after resetting."
            )
            .setCancelable(false)
            .setPositiveButton("Reset App") { _, _ ->
                viewModel.resetApp()
            }
            .setNegativeButton("Close App") { _, _ ->
                finishAffinity()
            }
            .show()
    }

    private fun startLockoutTimer() {
        // Disable UI
        binding.etPin.isEnabled = false
        binding.btnUnlock.isEnabled = false
        binding.btnUseBiometric.visibility = View.GONE

        val remainingTime = viewModel.getRemainingLockoutTime()

        lockoutTimer?.cancel()
        lockoutTimer = object : android.os.CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.mtPinMessage.text = buildString {
                    append("Try again in ")
                    append(seconds)
                    append("s")
                }
            }

            override fun onFinish() {
                binding.etPin.isEnabled = true
                binding.btnUnlock.isEnabled = true
                binding.mtPinMessage.text = null
                binding.etPin.text = null

                // Re-enable biometrics if applicable
                setupBiometricIfAvailable(viewModel.hasPinConfigured)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }
}
