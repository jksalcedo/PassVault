package com.jksalcedo.passvault.ui.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.importer.KeePassImporter
import junit.framework.TestCase
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptionTest {
    @Test
    fun sampleEncryptionTest() {
        Encryption.ensureKeyExists()
        val (cipher, iv) = Encryption.encrypt("sample123")
        println(cipher)
        println(iv)

        assertTrue(cipher.isNotEmpty())
        assertTrue(iv.isNotEmpty())

        val decrypted = Encryption.decrypt(cipher, iv)
        assertEquals("sample123", decrypted)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun mapToPasswordEntriesImportRecords() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
GitHub,alice,secret123,SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
GitLab,bob,pass456,MoreNotes,2008-01-15T14:30:00Z,2008-01-15T14:30:00Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)
        val entries = importer.mapToPasswordEntries(records)

        TestCase.assertEquals(2, entries.size)
        TestCase.assertEquals("GitHub", entries[0].title)
        TestCase.assertEquals("alice", entries[0].username)
        TestCase.assertEquals("SomeNotes", entries[0].notes)
        assertNotNull(entries[0].createdAt)
        assertNotNull(entries[0].updatedAt)
        TestCase.assertEquals("GitLab", entries[1].title)
        TestCase.assertEquals("bob", entries[1].username)
    }
}