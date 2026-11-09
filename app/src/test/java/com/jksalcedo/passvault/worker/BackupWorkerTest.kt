package com.jksalcedo.passvault.worker

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.core.worker.BackupWorker
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.ExportResult
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = com.jksalcedo.passvault.PassVaultApplication::class)
class BackupWorkerTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @JvmField
    @Rule
    var tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var mockPasswordRepo: PasswordRepository
    private lateinit var preferenceRepo: PreferenceRepository
    private lateinit var worker: BackupWorker

    @Before
    fun setup() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getExternalFilesDir(null) } returns tempFolder.root
        every { context.getSharedPreferences(any(), any()) } answers {
            baseContext.getSharedPreferences(arg(0), arg(1))
        }

        mockPasswordRepo = mockk()
        preferenceRepo = PreferenceRepository(context).apply { clear() }

        // Mock the Utility object and Encryption
        mockkObject(Utility)
        mockkObject(Encryption)
        every { Encryption.isKeystoreValid() } returns true
        every { Utility.serializeEntries(any(), any()) } returns ExportResult(
            serializedData = "{\"key\":\"dummy_json\"}",
            successCount = 1,
            failedEntries = emptyList(),
            totalCount = 1
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `doWork returns Success when entries exist`() = runBlocking {
        val fakeEntries = listOf(
            PasswordEntry(1, "Test", "user", "pass", "iv1", "notes1"),
        )
        coEvery { mockPasswordRepo.getAllEntries() } returns fakeEntries
        preferenceRepo.setEncryptBackups(false)
        preferenceRepo.setPasswordForAutoBackups("")

        worker = BackupWorker(context, mockk(), mockPasswordRepo, preferenceRepo)
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { mockPasswordRepo.getAllEntries() }

        // Verify that a file was actually created
        val backupsDir = File(tempFolder.root, "backups")
        assertTrue(backupsDir.exists())
        assertTrue(backupsDir.listFiles()?.isNotEmpty() == true)
        val backupFile = backupsDir.listFiles()!![0]
        assertEquals("{\"key\":\"dummy_json\"}", backupFile.readText())
    }

    @Test
    fun `doWork returns Success when no entries exist`() = runBlocking {
        coEvery { mockPasswordRepo.getAllEntries() } returns emptyList()
        preferenceRepo.setEncryptBackups(false)
        preferenceRepo.setPasswordForAutoBackups("")

        worker = BackupWorker(context, mockk(), mockPasswordRepo, preferenceRepo)
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns Failure when repository throws an exception`() = runBlocking {
        coEvery { mockPasswordRepo.getAllEntries() } throws RuntimeException("Database is corrupted")
        preferenceRepo.setEncryptBackups(false)
        preferenceRepo.setPasswordForAutoBackups("")

        worker = BackupWorker(context, mockk(), mockPasswordRepo, preferenceRepo)
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
