package com.jksalcedo.passvault.ui.base

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.auth.UnlockActivity
import com.jksalcedo.passvault.utils.SessionManager

/**
 * Base activity that applies common security settings like screenshot blocking.
 * All activities in the app should extend this class.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val preferenceRepository by lazy { PreferenceRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        applySecuritySettings()
    }

    override fun onResume() {
        super.onResume()
        checkLockStatus()
    }

    private fun checkLockStatus() {
        if (this is UnlockActivity) return
        if (!SessionManager.isUnlocked) {
            goToUnlockScreen()
        }
    }

    private fun goToUnlockScreen() {
        val unlockIntent = Intent(this, UnlockActivity::class.java)
        // Clear the stack
        unlockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Create a copy of the current intent to redirect to after unlock
        val targetIntent = Intent(intent)
        targetIntent.setClass(this, this::class.java)
        targetIntent.flags = 0

        unlockIntent.putExtra("EXTRA_REDIRECT_INTENT", targetIntent)

        startActivity(unlockIntent)
        finish()
    }

    private fun applyTheme() {
        val theme = preferenceRepository.getTheme()
        val mode = when (theme) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Applies security settings like screenshot blocking based on user preferences.
     */
    private fun applySecuritySettings() {
        if (preferenceRepository.getBlockScreenshots()) {
            // Block screenshots and screen recording
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            // Allow screenshots
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
