package com.jksalcedo.passvault.core.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jksalcedo.passvault.core.worker.BackupWorker
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for the automatic backup WorkManager job.
 *
 * Call [sync] at application startup to reconcile the persisted preference with the
 * actual scheduled work. This also initializes WorkManager eagerly, which lets its own
 * force-stop recovery run and prevents "enabled but never scheduled" states.
 */
object BackupScheduler {

    const val UNIQUE_WORK_NAME = "auto_backup_work"

    /**
     * Reconciles the scheduled job with the current preferences.
     *
     * @param policy [ExistingPeriodicWorkPolicy.KEEP] on startup, so an already scheduled
     * job is left untouched; [ExistingPeriodicWorkPolicy.REPLACE] when the user explicitly
     * (re)enables auto backups.
     */
    fun sync(
        context: Context,
        preferences: PreferenceRepository,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
    ) {
        val appContext = context.applicationContext

        if (!preferences.getAutoBackups() || preferences.isAutoBackupSetupRequired()) {
            cancel(appContext)
            return
        }

        enqueue(appContext, policy)
    }

    /**
     * Forces a fresh periodic schedule. Used when the user enables auto backups.
     */
    fun reschedule(context: Context, preferences: PreferenceRepository) {
        sync(context, preferences, ExistingPeriodicWorkPolicy.REPLACE)
    }

    /**
     * Cancels the automatic backup job.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun enqueue(context: Context, policy: ExistingPeriodicWorkPolicy) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request)
    }
}
