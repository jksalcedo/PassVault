package com.jksalcedo.passvault.worker

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.ListenableWorker
import com.jksalcedo.passvault.core.worker.BackupWorker
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.model.ExportResult
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import com.jksalcedo.passvault.util.Utility
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
import java.io.File

class BackupWorkerTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @JvmField
    @Rule
    var tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var mockPasswordRepo: PasswordRepository
    private lateinit var mockPreferenceRepo: PreferenceRepository
    private lateinit var backupsDir: File
    private lateinit var worker: BackupWorker

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockPasswordRepo = mockk()
        mockPreferenceRepo = mockk()

        // Set up the file directory
        backupsDir = tempFolder.newFolder("backups")
        every { context.getExternalFilesDir(null) } returns tempFolder.root

        // Mock the Utility object
        mockkObject(Utility)
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
        coEvery { mockPreferenceRepo.getExportFormat() } returns "json"
        coEvery { mockPreferenceRepo.updateLastBackupTime() } returns Unit
        coEvery { mockPreferenceRepo.getEncryptBackups() } returns false
        coEvery { mockPreferenceRepo.getPasswordForAutoBackups() } returns ""

        worker = BackupWorker(context, mockk(), mockPasswordRepo, mockPreferenceRepo)
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { mockPreferenceRepo.updateLastBackupTime() }
        coVerify(exactly = 1) { mockPasswordRepo.getAllEntries() }

        // Verify that a file was actually created
        assertTrue(backupsDir.exists())
        assertTrue(backupsDir.listFiles()?.isNotEmpty() == true)
        val backupFile = backupsDir.listFiles()!![0]
        assertEquals("{\"key\":\"dummy_json\"}", backupFile.readText())
    }

    @Test
    fun `doWork returns Success when no entries exist`() = runBlocking {
        coEvery { mockPasswordRepo.getAllEntries() } returns emptyList()
        coEvery { mockPreferenceRepo.getEncryptBackups() } returns false
        coEvery { mockPreferenceRepo.getPasswordForAutoBackups() } returns ""

        worker = BackupWorker(context, mockk(), mockPasswordRepo, mockPreferenceRepo)
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { mockPreferenceRepo.updateLastBackupTime() }
    }

    @Test
    fun `doWork returns Failure when repository throws an exception`() = runBlocking {
        coEvery { mockPasswordRepo.getAllEntries() } throws RuntimeException("Database is corrupted")
        coEvery { mockPreferenceRepo.getEncryptBackups() } returns false
        coEvery { mockPreferenceRepo.getPasswordForAutoBackups() } returns ""

        worker = BackupWorker(context, mockk(), mockPasswordRepo, mockPreferenceRepo)
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}