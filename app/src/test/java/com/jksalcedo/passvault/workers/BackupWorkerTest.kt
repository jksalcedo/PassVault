package com.jksalcedo.passvault.workers

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.repositories.PasswordRepository
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.utils.Utility
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
import java.util.Date

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
        every { Utility.serializeEntries(any(), any()) } returns "{\"key\":\"dummy_json\"}"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `doWork returns Success when entries exist`() = runBlocking {
        val fakeEntries = listOf(
            PasswordEntry(1, "Test", "user", "pass", "iv1", "notes1", Date().time),
        )
        coEvery { mockPasswordRepo.getAllEntries() } returns fakeEntries
        coEvery { mockPreferenceRepo.getExportFormat() } returns "json"
        coEvery { mockPreferenceRepo.updateLastBackupTime() } returns Unit

        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(TestWorkerFactory(mockPasswordRepo, mockPreferenceRepo))
            .build()
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

        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(TestWorkerFactory(mockPasswordRepo, mockPreferenceRepo))
            .build()
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { mockPreferenceRepo.updateLastBackupTime() }
    }

    @Test
    fun `doWork returns Failure when repository throws an exception`() = runBlocking {
        coEvery { mockPasswordRepo.getAllEntries() } throws RuntimeException("Database is corrupted")

        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(TestWorkerFactory(mockPasswordRepo, mockPreferenceRepo))
            .build()
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}