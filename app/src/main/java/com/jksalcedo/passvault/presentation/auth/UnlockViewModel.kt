package com.jksalcedo.passvault.presentation.auth

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import com.jksalcedo.passvault.Application as PassVaultApp

/**
 * ViewModel that owns master PIN verification, lockout handling and app reset.
 *
 * The Activity is only responsible for rendering state and showing biometric prompts.
 */
class UnlockViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as PassVaultApp).container
    private val prefsRepository: PreferenceRepository = container.preferenceRepository

    val hasPinConfigured: Boolean
        get() = prefsRepository.hasPinConfigured()

    val biometricLoginEnabled: Boolean
        get() = prefsRepository.getBiometricLoginEnabled()

    private val _resetResult = MutableLiveData<Boolean?>()
    val resetResult: LiveData<Boolean?> = _resetResult

    fun isLockedOut(): Boolean = prefsRepository.isPinLockedOut()

    fun getRemainingLockoutTime(): Long = prefsRepository.getRemainingLockoutTime()

    /**
     * Checks that the stored PIN can still be decrypted with the Android Keystore key.
     */
    fun isKeystoreValid(): Boolean {
        val cipher = prefsRepository.getPinCipher()
        val iv = prefsRepository.getPinIv()
        if (cipher.isNullOrEmpty() || iv.isNullOrEmpty()) return false
        return try {
            Encryption.decrypt(cipher, iv)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Verifies the entered PIN and updates the failed-attempt/lockout state.
     */
    fun verifyPin(input: String): UnlockResult {
        if (prefsRepository.isPinLockedOut()) return UnlockResult.LockedOut

        val cipher = prefsRepository.getPinCipher()
        val iv = prefsRepository.getPinIv()
        val storedPin = try {
            if (cipher != null && iv != null) {
                Encryption.decrypt(cipher, iv)
            } else {
                ""
            }
        } catch (_: Exception) {
            return UnlockResult.KeystoreInvalid
        }

        if (input.isNotEmpty() && storedPin == input) return UnlockResult.Success

        val newCount = prefsRepository.incrementFailedPinAttempts()
        val maxAttempts = prefsRepository.getMaxFailedAttempts()
        return if (newCount >= maxAttempts) {
            val lockoutDuration = prefsRepository.getLockoutDuration()
            prefsRepository.setPinLockoutEndTime(System.currentTimeMillis() + lockoutDuration)
            prefsRepository.setFailedPinAttempts(0)
            UnlockResult.LockedOut
        } else {
            UnlockResult.Incorrect(maxAttempts - newCount)
        }
    }

    /**
     * Encrypts and stores a new master PIN.
     * @return True when the PIN was saved successfully.
     */
    fun savePin(pin: String): Boolean {
        return try {
            Encryption.ensureKeyExists()
            val (cipher, iv) = Encryption.encrypt(pin)
            prefsRepository.setPin(cipher, iv)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Records a successful unlock.
     */
    fun onUnlocked() {
        prefsRepository.setHasAuthenticatedBefore(true)
        prefsRepository.setLastInteractionTime(System.currentTimeMillis())
    }

    /**
     * Clears the auth preferences, deletes the invalidated key and wipes the database.
     */
    fun resetApp() {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>()
                        .getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                        .edit { clear() }
                    prefsRepository.setHasAuthenticatedBefore(false)

                    try {
                        val keyStore = KeyStore.getInstance("AndroidKeyStore")
                        keyStore.load(null)
                        keyStore.deleteEntry("passvault_key_v1")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    container.clearAllData()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            _resetResult.postValue(success)
        }
    }

    fun resetResetResult() {
        _resetResult.value = null
    }
}

/**
 * Result of a PIN verification attempt.
 */
sealed class UnlockResult {
    object Success : UnlockResult()
    data class Incorrect(val remainingAttempts: Int) : UnlockResult()
    object LockedOut : UnlockResult()
    object KeystoreInvalid : UnlockResult()
}
