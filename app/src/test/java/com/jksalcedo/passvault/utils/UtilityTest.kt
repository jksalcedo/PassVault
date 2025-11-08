package com.jksalcedo.passvault.utils

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jksalcedo.passvault.data.AppDatabase
import com.jksalcedo.passvault.data.PasswordDao
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.getOrAwaitValue
import com.jksalcedo.passvault.utils.Utility.formatTime
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
    }

    @After
    fun teardown() {
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

        print(result)

        // Check for the first entry with a non-null username
        assertThat(result).contains("\"id\":1,\"title\":\"Entry1\",\"username\":\"user1\"")

        // Check for the second entry with null username
        assertThat(result).contains("\"id\":2,\"title\":\"Entry2\",\"username\":null")
        assertThat(result).contains("\"notes\":\"some notes\"")
    }

    @Test
    fun `deserialize entries`() = runTest {
        val to = listOf(
            PasswordEntry(
                id = 1,
                title = "Entry1",
                username = "user1",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null,
                createdAt = 0L,
                updatedAt = 0L
            )
        )
        val entry = Utility.serializeEntries(to, "json")
        val result = Utility.deserializeEntries(entry, "json")

        print(result)

        assertThat(result).contains(
            PasswordEntry(
                id = 1,
                title = "Entry1",
                username = "user1",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null,
                createdAt = 0L,
                updatedAt = 0L
            )
        )
    }

    @Test
    fun `formatTime with known timestamp`() {
        val zonedDateTime = ZonedDateTime.of(2025, 11, 8, 10, 30, 0, 0, utc)
        val timestampInMillis = zonedDateTime.toInstant().toEpochMilli() // This gives us our Long

        val formattedDate = timestampInMillis.formatTime(zoneId = utc)

        assertThat(formattedDate).isEqualTo("Nov 08 2025")
    }

    @Test
    fun `formatTime with epoch Jan 01 1970`() {
        val epochTimestamp = 0L

        val formattedDate = epochTimestamp.formatTime(zoneId = utc)

        assertThat(formattedDate).isEqualTo("Jan 01 1970")
    }

    @Test
    fun `formatTime with pre-epoch timestamp`() {
        val zonedDateTime = ZonedDateTime.of(1969, 12, 25, 18, 0, 0, 0, utc)
        val preEpochTimestamp = zonedDateTime.toInstant().toEpochMilli()

        val formattedDate = preEpochTimestamp.formatTime(zoneId = utc)

        assertThat(formattedDate).isEqualTo("Dec 25 1969")
    }

    @Test
    fun `formatTime with epoch timestamp in different timezone`() {
        val epochTimestamp = 0L
        val newYorkZone = ZoneId.of("America/New_York")

        val formattedDate = epochTimestamp.formatTime(zoneId = newYorkZone)

        assertThat(formattedDate).isEqualTo("Dec 31 1969")
    }

}