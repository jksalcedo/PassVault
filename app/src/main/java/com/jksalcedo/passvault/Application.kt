package com.jksalcedo.passvault

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.work.Configuration
import com.jksalcedo.passvault.data.AppDatabase
import com.jksalcedo.passvault.data.CategoryRepository
import com.jksalcedo.passvault.repositories.PasswordRepository
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.auth.UnlockActivity
import com.jksalcedo.passvault.utils.PassVaultCrashHandler
import com.jksalcedo.passvault.utils.SessionManager
import com.jksalcedo.passvault.workers.BackupWorkerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Application : Application(),
    Configuration.Provider {

    private lateinit var passwordRepository: PasswordRepository
    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var workerFactory: BackupWorkerFactory
    private var lastInteractionTime: Long = 0

    companion object {
        // Removed fixed timeout
    }

    override fun onCreate() {
        super.onCreate()

        passwordRepository = PasswordRepository(this.applicationContext)
        preferenceRepository = PreferenceRepository(this.applicationContext)
        workerFactory = BackupWorkerFactory(passwordRepository, preferenceRepository)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            PassVaultCrashHandler(
                this,
                preferenceRepository,
                defaultHandler
            )
        )

        // Initialize default categories
        val categoryDao = AppDatabase.getDatabase(this).categoryDao()
        val categoryRepository = CategoryRepository(categoryDao)

        CoroutineScope(Dispatchers.IO).launch {
            if (preferenceRepository.isFirstLaunch()) {
                categoryRepository.initializeDefaultCategories()
                }
        }

        // Apply Dynamic Colors if enabled
        if (preferenceRepository.getUseDynamicColors()) {
            com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this)
        }

        // Initialize interaction time from prefs to handle process death
        lastInteractionTime = preferenceRepository.getLastInteractionTime()
        if (lastInteractionTime == 0L) {
            lastInteractionTime = System.currentTimeMillis()
            preferenceRepository.setLastInteractionTime(lastInteractionTime)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                // Check if app should be locked due to inactivity
                val timeout = preferenceRepository.getAutoLockTimeout()

                if (timeout != -1L && lastInteractionTime > 0 &&
                    System.currentTimeMillis() - lastInteractionTime >= timeout &&
                    activity !is UnlockActivity &&
                    !activity.isFinishing &&
                    !activity.isDestroyed
                ) {
                    SessionManager.isUnlocked = false
                    // Lock the app by launching UnlockActivity
                    val intent = Intent(activity, UnlockActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                // Record the time when user leaves the app
                lastInteractionTime = System.currentTimeMillis()
                preferenceRepository.setLastInteractionTime(lastInteractionTime)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
