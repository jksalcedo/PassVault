package com.jksalcedo.passvault.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jksalcedo.passvault.dao.PasswordDao
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordDaoTest {

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
    fun insertPasswordEntry_getsPasswordEntry() = runTest {
        val passwordEntry = PasswordEntry(
            title = "Test Title",
            username = "Test Username",
            passwordCipher = "cipher",
            passwordIv = "iv",
            notes = "Test Notes"
        )
        val insertedId = dao.insert(passwordEntry)

        val allEntries = dao.getAll().getOrAwaitValue()
        val retrievedEntry = allEntries.find { it.id == insertedId }
        assertThat(retrievedEntry).isNotNull()
        assertThat(retrievedEntry?.title).isEqualTo(passwordEntry.title)
        assertThat(retrievedEntry?.username).isEqualTo(passwordEntry.username)
        assertThat(retrievedEntry?.notes).isEqualTo(passwordEntry.notes)
    }

    @Test
    fun updatePasswordEntry_getsUpdatedPasswordEntry() = runTest {
        val passwordEntry = PasswordEntry(
            title = "Test Title",
            username = "Test Username",
            passwordCipher = "cipher",
            passwordIv = "iv",
            notes = "Test Notes"
        )
        val insertedId = dao.insert(passwordEntry)

        val entryToUpdate = passwordEntry.copy(id = insertedId, title = "Updated Title")
        dao.update(entryToUpdate)

        val retrievedEntry = dao.getEntryById(insertedId).getOrAwaitValue()
        assertThat(retrievedEntry.title).isEqualTo("Updated Title")
    }

    @Test
    fun deletePasswordEntry_entryIsDeleted() = runTest {
        val passwordEntry = PasswordEntry(
            title = "Test Title",
            username = "Test Username",
            passwordCipher = "cipher",
            passwordIv = "iv",
            notes = "Test Notes"
        )
        val insertedId = dao.insert(passwordEntry)
        dao.delete(passwordEntry.copy(id = insertedId))

        val allEntries = dao.getAll().getOrAwaitValue()
        assertThat(allEntries).isEmpty()
    }

    @Test
    fun `getAll from empty table`() = runTest {
        val allEntries = dao.getAll().getOrAwaitValue()
        assertThat(allEntries).isEmpty()
    }

    @Test
    fun `getAll returns sorted entries`() = runTest {
        dao.insert(
            PasswordEntry(
                title = "C",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        dao.insert(
            PasswordEntry(
                title = "A",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        dao.insert(
            PasswordEntry(
                title = "B",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val allEntries = dao.getAll().getOrAwaitValue()
        assertThat(allEntries.map { it.title }).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun `getAll with entries having identical titles`() = runTest {
        val id1 = dao.insert(
            PasswordEntry(
                title = "Same",
                username = "user1",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val id2 = dao.insert(
            PasswordEntry(
                title = "Same",
                username = "user2",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val allEntries = dao.getAll().getOrAwaitValue()
        assertThat(allEntries).hasSize(2)
        assertThat(allEntries.map { it.id }).containsExactly(id1, id2).inOrder()
    }

    @Test
    fun `getAll LiveData updates on insert`() = runTest {
        val initialEntries = dao.getAll().getOrAwaitValue()
        assertThat(initialEntries).isEmpty()

        dao.insert(
            PasswordEntry(
                title = "New",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val updatedEntries = dao.getAll().getOrAwaitValue()
        assertThat(updatedEntries).hasSize(1)
        assertThat(updatedEntries[0].title).isEqualTo("New")
    }

    @Test
    fun `getAll LiveData updates on delete`() = runTest {
        val id1 = dao.insert(
            PasswordEntry(
                title = "Entry1",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val id2 = dao.insert(
            PasswordEntry(
                title = "Entry2",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.delete(
            PasswordEntry(
                id = id1,
                title = "Entry1",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val remainingEntries = dao.getAll().getOrAwaitValue()
        assertThat(remainingEntries).hasSize(1)
        assertThat(remainingEntries[0].id).isEqualTo(id2)
    }

    @Test
    fun `getAll LiveData updates on update`() = runTest {
        val id = dao.insert(
            PasswordEntry(
                title = "B",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        dao.insert(
            PasswordEntry(
                title = "A",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.update(
            PasswordEntry(
                id = id,
                title = "Z",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val entries = dao.getAll().getOrAwaitValue()
        assertThat(entries.map { it.title }).containsExactly("A", "Z").inOrder()
    }

    @Test
    fun `getEntryById for a non existent ID`() = runTest {
        val entry = dao.getEntryById(999).getOrAwaitValue()
        assertThat(entry).isNull()
    }

    @Test
    fun `getEntryById LiveData updates on change`() = runTest {
        val id = dao.insert(
            PasswordEntry(
                title = "Original",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.update(
            PasswordEntry(
                id = id,
                title = "Updated",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val entry = dao.getEntryById(id).getOrAwaitValue()
        assertThat(entry.title).isEqualTo("Updated")
    }

    @Test
    fun `getEntryById returns correct entry`() = runTest {
        val id1 = dao.insert(
            PasswordEntry(
                title = "Entry1",
                username = "user1",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val id2 = dao.insert(
            PasswordEntry(
                title = "Entry2",
                username = "user2",
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val entry1 = dao.getEntryById(id1).getOrAwaitValue()
        val entry2 = dao.getEntryById(id2).getOrAwaitValue()

        assertThat(entry1.title).isEqualTo("Entry1")
        assertThat(entry2.title).isEqualTo("Entry2")
    }

    @Test
    fun `insert with conflict replaces existing entry`() = runTest {
        val id = dao.insert(
            PasswordEntry(
                title = "Original",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.insert(
            PasswordEntry(
                id = id,
                title = "Replaced",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val entry = dao.getEntryById(id).getOrAwaitValue()
        assertThat(entry.title).isEqualTo("Replaced")
    }

    @Test
    fun `insert entry with null or empty fields`() = runTest {
        val id = dao.insert(
            PasswordEntry(
                title = "",
                username = null,
                passwordCipher = "",
                passwordIv = "",
                notes = null
            )
        )

        val entry = dao.getEntryById(id).getOrAwaitValue()
        assertThat(entry).isNotNull()
        assertThat(entry.title).isEmpty()
        assertThat(entry.username).isNull()
        assertThat(entry.notes).isNull()
    }

    @Test
    fun `insert returns correct new row ID`() = runTest {
        val id = dao.insert(
            PasswordEntry(
                title = "Test",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        assertThat(id).isGreaterThan(0)
    }

    @Test
    fun `update non existent entry`() = runTest {
        val allBefore = dao.getAll().getOrAwaitValue()

        dao.update(
            PasswordEntry(
                id = 999,
                title = "Ghost",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val allAfter = dao.getAll().getOrAwaitValue()
        assertThat(allAfter).isEqualTo(allBefore)
    }

    @Test
    fun `update does not change primary key`() = runTest {
        val id = dao.insert(
            PasswordEntry(
                title = "Original",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.update(
            PasswordEntry(
                id = id,
                title = "Updated",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val entry = dao.getEntryById(id).getOrAwaitValue()
        assertThat(entry.id).isEqualTo(id)
        assertThat(entry.title).isEqualTo("Updated")
    }

    @Test
    fun `delete non existent entry`() = runTest {
        val allBefore = dao.getAll().getOrAwaitValue()

        dao.delete(
            PasswordEntry(
                id = 999,
                title = "Ghost",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val allAfter = dao.getAll().getOrAwaitValue()
        assertThat(allAfter).isEqualTo(allBefore)
    }

    @Test
    fun `delete correct entry among many`() = runTest {
        val idA = dao.insert(
            PasswordEntry(
                title = "A",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val idB = dao.insert(
            PasswordEntry(
                title = "B",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val idC = dao.insert(
            PasswordEntry(
                title = "C",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.delete(
            PasswordEntry(
                id = idB,
                title = "B",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val remaining = dao.getAll().getOrAwaitValue()
        assertThat(remaining.map { it.id }).containsExactly(idA, idC)
    }

    @Test
    fun `concurrent operations`() = runTest {
        val id1 = dao.insert(
            PasswordEntry(
                title = "Entry1",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        dao.insert(
            PasswordEntry(
                title = "Entry2",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.update(
            PasswordEntry(
                id = id1,
                title = "Updated",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        dao.insert(
            PasswordEntry(
                title = "Entry3",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        val entries = dao.getAll().getOrAwaitValue()
        assertThat(entries).hasSize(3)
        assertThat(entries.find { it.id == id1 }?.title).isEqualTo("Updated")
    }

    @Test
    fun `deleteAllTrashedEntries removes only trashed entries`() = runTest {
        val id1 = dao.insert(
            PasswordEntry(
                title = "Active Entry",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val id2 = dao.insert(
            PasswordEntry(
                title = "Trashed Entry 1",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )
        val id3 = dao.insert(
            PasswordEntry(
                title = "Trashed Entry 2",
                username = null,
                passwordCipher = "c",
                passwordIv = "iv",
                notes = null
            )
        )

        dao.moveToTrash(id2, System.currentTimeMillis())
        dao.moveToTrash(id3, System.currentTimeMillis())

        val trashedBefore = dao.getDeletedEntries().getOrAwaitValue()
        assertThat(trashedBefore).hasSize(2)

        dao.deleteAllTrashedEntries()

        val trashedAfter = dao.getDeletedEntries().getOrAwaitValue()
        assertThat(trashedAfter).isEmpty()

        val activeEntries = dao.getAll().getOrAwaitValue()
        assertThat(activeEntries).hasSize(1)
        assertThat(activeEntries[0].id).isEqualTo(id1)
    }
}