package com.jksalcedo.passvault.core.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository

/**
 * A factory for creating [BackupWorker] instances.
 * @param passwordRepository The password repository.
 * @param preferenceRepository The preference repository.
 */
class BackupWorkerFactory(
    private val passwordRepository: PasswordRepository,
    private val preferenceRepository: PreferenceRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            BackupWorker::class.java.name ->
                BackupWorker(
                    appContext,
                    workerParameters,
                    passwordRepository,
                    preferenceRepository
                )

            else -> null
        }
    }
}
