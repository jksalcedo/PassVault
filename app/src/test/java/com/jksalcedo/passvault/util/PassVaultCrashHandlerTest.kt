package com.jksalcedo.passvault.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.jksalcedo.passvault.core.util.PassVaultCrashHandler
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.Thread.UncaughtExceptionHandler

class PassVaultCrashHandlerTest {

    private lateinit var context: Context
    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var defaultHandler: UncaughtExceptionHandler
    private lateinit var crashHandler: PassVaultCrashHandler
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        preferenceRepository = mockk(relaxed = true)
        defaultHandler = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)

        every { context.packageName } returns "com.jksalcedo.passvault"
        every { context.packageManager } returns packageManager

        // Mock PackageInfo
        val packageInfo = PackageInfo()
        packageInfo.packageName = "com.jksalcedo.passvault"
        packageInfo.versionName = "1.0.0"
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } returns packageInfo

        // Mock File operations
        mockkStatic(File::class)

        crashHandler = PassVaultCrashHandler(context, preferenceRepository, defaultHandler)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `uncaughtException should call default handler`() {
        val thread = Thread.currentThread()
        val exception = RuntimeException("Test Exception")

        val mockFile = mockk<File>(relaxed = true)
        every { context.getExternalFilesDir(any()) } returns mockFile
        every { mockFile.exists() } returns true

        crashHandler.uncaughtException(thread, exception)

        verify { defaultHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun `uncaughtException should handle exceptions during logging and still call default handler`() {
        val thread = Thread.currentThread()
        val exception = RuntimeException("Test Exception")

        every { context.packageName } throws RuntimeException("Context Error")

        crashHandler.uncaughtException(thread, exception)

        verify { defaultHandler.uncaughtException(thread, exception) }
    }
}
