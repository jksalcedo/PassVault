package com.jksalcedo.passvault.ui.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jksalcedo.passvault.data.local.AppDatabase
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import com.jksalcedo.passvault.presentation.settings.SettingsViewModel
import com.jksalcedo.passvault.ui.settings.ImportUiState
import com.jksalcedo.passvault.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var activity: Activity
    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var database: AppDatabase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        application = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        AppDatabase.initializeForTesting(database)
        preferenceRepository = PreferenceRepository(application).apply { clear() }
        viewModel = SettingsViewModel(application)
        io.mockk.mockkObject(com.jksalcedo.passvault.crypto.Encryption)
        io.mockk.every { com.jksalcedo.passvault.crypto.Encryption.encrypt(any()) } returns Pair("cipher", "iv")
        io.mockk.every { com.jksalcedo.passvault.crypto.Encryption.decrypt(any(), any()) } returns "password"
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
        database.clearAllTables()
    }

    @Test
    fun `importEntries uses format override for PassVault JSON`() = runBlocking {
        // Given export preference is CSV but we import a JSON backup
        preferenceRepository.setExportFormat("csv")
        val entry = PasswordEntry(
            title = "Json Entry",
            username = "user@example.com",
            passwordCipher = "cipher",
            passwordIv = "iv",
            notes = "note",
            createdAt = 123L,
            updatedAt = 456L
        )
        val jsonPayload = Utility.serializeEntries(listOf(entry), "json").serializedData
        val backupFile = File(application.filesDir, "passvault_override.json").apply {
            writeText(jsonPayload)
        }

        try {
            val uri = Uri.fromFile(backupFile)
            val latch = CountDownLatch(1)
            val observer = Observer<ImportUiState> { state ->
                if (state is ImportUiState.Success) {
                    latch.countDown()
                }
            }
            viewModel.importUiState.observeForever(observer)

            withContext(Dispatchers.IO) {
                viewModel.importEntries(uri, password = "", formatOverride = "json")
            }

            val completed = latch.await(3, TimeUnit.SECONDS)
            viewModel.importUiState.removeObserver(observer)
            check(completed) { "Import did not finish in time" }

            val stored = withContext(Dispatchers.IO) {
                database.passwordDao().getAllEntries()
            }

            assertThat(stored).hasSize(1)
            assertThat(stored.first().title).isEqualTo("Json Entry")
            assertThat(stored.first().username).isEqualTo("user@example.com")
        } finally {
            backupFile.delete()
        }
    }

    private class TestActivity : Activity()
}

