package com.jksalcedo.passvault.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.jksalcedo.passvault.repositories.PasswordRepository
import com.jksalcedo.passvault.repositories.PreferenceRepository

class TestWorkerFactory(
    private val passwordRepository: PasswordRepository,
    private val preferenceRepository: PreferenceRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            BackupWorker::class.java.name -> {
                BackupWorker(
                    appContext,
                    workerParameters,
                    passwordRepository,
                    preferenceRepository
                )
            }

            else -> null
        }
    }
}