package com.jksalcedo.passvault.core.util

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PassVaultCrashHandler(
    private val context: Context,
    private val preferenceRepository: PreferenceRepository,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "PassVaultCrashHandler"
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                .format(Date())

            val packageInfo: PackageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (_: Exception) {
                PackageInfo().apply {
                    packageName = context.packageName
                    versionName = "Unknown"
                }
            }

            val contentToWrite = """
                $timestamp
                ${packageInfo.packageName} ${packageInfo.versionName}
                ${Build.HOST} ${Build.DEVICE}
                ${Build.VERSION.BASE_OS}
                                
                ${Log.getStackTraceString(e)}
            """.trimIndent()

            val fileName = "PV_Crash_$timestamp.txt"
            val crashLocationUri = preferenceRepository.getCrashLogsLocation()

            if (crashLocationUri != null) {
                // Use custom location (SAF)
                try {
                    val treeUri = crashLocationUri.toUri()
                    val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                        context,
                        treeUri
                    )

                    if (pickedDir != null && pickedDir.canWrite()) {
                        val newFile = pickedDir.createFile("text/plain", fileName)
                        if (newFile != null) {
                            context.contentResolver.openOutputStream(newFile.uri)
                                ?.use { outputStream ->
                                    outputStream.write(contentToWrite.toByteArray())
                                }
                        } else {
                            Log.e(TAG, "Failed to create crash log file")
                        }
                    } else {
                        Log.e(TAG, "Custom crash logs location is not accessible or writable")
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error writing crash log to custom location", ex)
                }
            } else {
                // Use default internal storage
                try {
                    val crashLogDir = File(context.getExternalFilesDir(null), "crash_logs")
                    if (!crashLogDir.exists()) {
                        crashLogDir.mkdirs()
                    }
                    val crashLogFile = File(crashLogDir, fileName)
                    crashLogFile.writeText(contentToWrite)
                } catch (ex: Exception) {
                    Log.e(TAG, "Error writing crash log to internal storage", ex)
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error in crash handler", ex)
        } finally {
            // Always call the default handler to ensure the app crashes properly
            defaultHandler?.uncaughtException(t, e)
        }
    }
}
