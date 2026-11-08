package com.jksalcedo.passvault.util

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jksalcedo.passvault.core.crypto.Encryption
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.core.util.Utility.formatTime
import com.jksalcedo.passvault.data.local.AppDatabase
import com.jksalcedo.passvault.data.local.dao.PasswordDao
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UtilityTest {

    private val utc = ZoneId.of("UTC")

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var dao: PasswordDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.passwordDao()
        io.mockk.mockkObject(Encryption)
        io.mockk.every { Encryption.encrypt(any()) } returns Pair(
            "cipher",
            "iv"
        )
        io.mockk.every {
            Encryption.decrypt(
                any(),
                any()
            )
        } returns "SuperSecretPassword123!"
        io.mockk.every {
            Encryption.decrypt(
                eq("c"),
                eq("iv")
            )
        } returns "password"
    }

    @After
    fun teardown() {
        io.mockk.unmockkAll()
        database.close()
    }

    @Test
    fun `serialize entries`() = runTest {
        dao.insert(
            PasswordEntry(
                id = 1,
                title = "Entry1",
                username = "user1",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        dao.insert(
            PasswordEntry(
                id = 2,
                title = "Entry2",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = "some notes"
            )
        )

        val entries = dao.getAll().getOrAwaitValue()
        val result = Utility.serializeEntries(
            entries,
            format = "json"
        )

        print(result.serializedData)

        // Check for the first entry with a non-null username
        assertThat(result.serializedData).contains("\"title\": \"Entry1\"")
        assertThat(result.serializedData).contains("\"username\": \"user1\"")

        // Check for the second entry with null username
        assertThat(result.serializedData).contains("\"title\": \"Entry2\"")
        assertThat(result.serializedData).contains("\"username\": null")
        assertThat(result.serializedData).contains("\"notes\": \"some notes\"")
    }

    @Test
    fun `deserialize entries`() = runTest {
        val to = listOf(
            PasswordEntry(
                id = 0,
                title = "Entry1",
                username = "user1",
                passwordCipher = "cipher",
                passwordIv = "iv",
                notes = null,
                createdAt = 0L,
                updatedAt = 0L
            )
        )
        val entry = Utility.serializeEntries(to, "json")
        val result = Utility.deserializeEntries(entry.serializedData, "json")

        print(result)

        assertThat(result).contains(
            PasswordEntry(
                id = 0,
                title = "Entry1",
                username = "user1",
                passwordCipher = "cipher",
                passwordIv = "iv",
                notes = null,
                createdAt = 0L,
                updatedAt = 0L
            )
        )
    }

    @Test
    fun `serialize and deserialize entries handle uppercase JSON`() = runTest {
        val entries = listOf(
            PasswordEntry(
                id = 10,
                title = "UpperJson",
                username = "upper@json.dev",
                passwordCipher = "cipher",
                passwordIv = "iv",
                notes = null,
                createdAt = 10L,
                updatedAt = 20L
            )
        )

        val payload = Utility.serializeEntries(entries, "JSON")
        val parsed = Utility.deserializeEntries(payload.serializedData, "JSON")

        assertThat(parsed).hasSize(1)
        assertThat(parsed.first().title).isEqualTo("UpperJson")
    }

    @Test
    fun `serialize and deserialize entries handle uppercase CSV`() = runTest {
        val entries = listOf(
            PasswordEntry(
                id = 11,
                title = "UpperCsv",
                username = "upper@csv.dev",
                passwordCipher = "cipher",
                passwordIv = "iv",
                notes = "csv",
                createdAt = 30L,
                updatedAt = 30L
            )
        )

        val payload = Utility.serializeEntries(entries, "CSV")
        val parsed = Utility.deserializeEntries(payload.serializedData, "CSV")

        assertThat(parsed).hasSize(1)
        assertThat(parsed.first().title).isEqualTo("UpperCsv")
        assertThat(parsed.first().notes).isEqualTo("csv")
    }

    @Test
    fun `formatTime with known timestamp`() {
        val zonedDateTime = ZonedDateTime.of(2025, 11, 8, 10, 30, 0, 0, utc)
        val timestampInMillis = zonedDateTime.toInstant().toEpochMilli() // This gives us our Long

        val formattedDate = timestampInMillis.formatTime(zoneId = utc)

        assertThat(formattedDate).isEqualTo("Nov 08 2025 10:30 AM")
    }

    @Test
    fun `formatTime with epoch Jan 01 1970`() {
        val epochTimestamp = 0L

        val formattedDate = epochTimestamp.formatTime(zoneId = utc)

        assertThat(formattedDate).isEqualTo("Jan 01 1970 12:00 AM")
    }

    @Test
    fun `formatTime with pre-epoch timestamp`() {
        val zonedDateTime = ZonedDateTime.of(1969, 12, 25, 18, 0, 0, 0, utc)
        val preEpochTimestamp = zonedDateTime.toInstant().toEpochMilli()

        val formattedDate = preEpochTimestamp.formatTime(zoneId = utc)

        assertThat(formattedDate).isEqualTo("Dec 25 1969 6:00 PM")
    }

    @Test
    fun `formatTime with epoch timestamp in different timezone`() {
        val epochTimestamp = 0L
        val newYorkZone = ZoneId.of("America/New_York")

        val formattedDate = epochTimestamp.formatTime(zoneId = newYorkZone)

        assertThat(formattedDate).isEqualTo("Dec 31 1969 7:00 PM")
    }

    @Test
    fun `formatTime with different default locale`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRENCH)
            val zonedDateTime = ZonedDateTime.of(2025, 11, 8, 10, 30, 0, 0, utc)
            val timestampInMillis = zonedDateTime.toInstant().toEpochMilli()

            val formattedDate = timestampInMillis.formatTime(zoneId = utc)

            assertThat(formattedDate).isEqualTo("nov. 08 2025 10:30 AM")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `serialize and deserialize preserves password`() = runTest {
        val originalPassword = "SuperSecretPassword123!"
        val (cipher, iv) = Encryption.encrypt(originalPassword)

        val entries = listOf(
            PasswordEntry(
                id = 1,
                title = "PasswordTest",
                username = "user",
                passwordCipher = cipher,
                passwordIv = iv,
                notes = "notes",
                createdAt = 100L,
                updatedAt = 200L
            )
        )

        val json = Utility.serializeEntries(entries, "json")
        val deserialized = Utility.deserializeEntries(json.serializedData, "json")

        assertThat(deserialized).hasSize(1)
        val entry = deserialized.first()

        val decryptedPassword = Encryption.decrypt(
            entry.passwordCipher,
            entry.passwordIv
        )
        assertThat(decryptedPassword).isEqualTo(originalPassword)
    }
}