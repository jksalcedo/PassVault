package com.jksalcedo.passvault.data.importer

import com.jksalcedo.passvault.data.model.ImportRecord
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class KeepassDxCsvImporterTest {

    @Test
    fun `parse function with valid and complete CSV data`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
GitHub,alice,secret123,SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        assertEquals(1, records.size)
        assertEquals("GitHub", records[0].title)
        assertEquals("alice", records[0].username)
        assertEquals("secret123", records[0].password)
        assertEquals("SomeNotes", records[0].notes)
        assertNotNull(records[0].createdAt)
        assertNotNull(records[0].updatedAt)
    }

    @Test
    fun `parse function with empty CSV input`() = runBlocking {
        val csv = ""

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        assertEquals(emptyList<ImportRecord>(), records)
    }

    @Test
    fun `parse function for CSV with only a header row`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        assertEquals(emptyList<ImportRecord>(), records)
    }

    @Test
    fun `parse function where a row has a missing password`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
GitHub,alice,,SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
GitLab,bob,validpass,MoreNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        // Both rows should be included now
        assertEquals(2, records.size)
        assertEquals("GitHub", records[0].title)
        assertEquals("", records[0].password)
        assertEquals("GitLab", records[1].title)
        assertEquals("validpass", records[1].password)
    }

    @Test
    fun `parse function where a row has an empty password`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
GitHub,alice,"",SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
GitLab,bob,validpass,MoreNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        // Both rows should be included now
        assertEquals(2, records.size)
        assertEquals("GitHub", records[0].title)
        assertEquals("", records[0].password)
        assertEquals("GitLab", records[1].title)
    }

    @Test
    fun `parse function where a row s password contains only whitespace`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
GitHub,alice,"   ",SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
GitLab,bob,validpass,MoreNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        // Only the row with non-whitespace password should be included
        assertEquals(2, records.size)
        assertEquals("GitLab", records[1].title)
    }

    @Test
    fun `parse function with missing non password columns`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
GitHub,,secret123,,,
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        assertEquals(1, records.size)
        assertEquals("GitHub", records[0].title)
        assertEquals("", records[0].username)
        assertEquals("secret123", records[0].password)
        assertEquals("", records[0].notes)
    }

    @Test
    fun `parse function with extra unknown CSV columns`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,URL,ExtraField,CreationTime,LastModificationTime
GitHub,alice,secret123,SomeNotes,https://github.com,extra,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        // Should parse known columns and ignore unknown ones
        assertEquals(1, records.size)
        assertEquals("GitHub", records[0].title)
        assertEquals("alice", records[0].username)
        assertEquals("secret123", records[0].password)
    }

    @Test
    fun `parse function with special characters in fields`() = runBlocking {
        val csv = """
Title,UserName,Password,Notes,CreationTime,LastModificationTime
"Title, with comma",user@email.com,"pass""word",Note with "quotes" and
newlines,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z
        """.trimIndent()

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        assertEquals(2, records.size)
        assertEquals("Title, with comma", records[0].title)
        assertEquals("user@email.com", records[0].username)
        assertTrue(records[0].password.isNotEmpty())
    }

    @Test
    fun `parse function with very large CSV input`() = runBlocking {
        val header = "Title,UserName,Password,Notes,CreationTime,LastModificationTime"
        val rows = (1..1000).joinToString("\n") { index ->
            "Title$index,user$index,pass$index,notes$index,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z"
        }
        val csv = "$header\n$rows"

        val importer = KeePassImporter()
        val records = importer.parse(csv)

        assertEquals(1000, records.size)
        assertEquals("Title1", records[0].title)
        assertEquals("Title1000", records[999].title)
    }

    @Test
    fun `parse function with different line endings`() = runBlocking {
        // Test with Windows-style CRLF line endings
        val csvCRLF =
            "Title,UserName,Password,Notes,CreationTime,LastModificationTime\r\nGitHub,alice,secret123,SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z"

        val importer = KeePassImporter()
        val recordsCRLF = importer.parse(csvCRLF)

        assertEquals(1, recordsCRLF.size)
        assertEquals("GitHub", recordsCRLF[0].title)

        // Test with Unix-style LF line endings
        val csvLF =
            "Title,UserName,Password,Notes,CreationTime,LastModificationTime\nGitHub,alice,secret123,SomeNotes,2007-12-03T10:15:30Z,2007-12-03T10:15:30Z"

        val recordsLF = importer.parse(csvLF)

        assertEquals(1, recordsLF.size)
        assertEquals("GitHub", recordsLF[0].title)
    }

    @Test
    fun `mapToPasswordEntries with an empty list input`() = runBlocking {
        val importer = KeePassImporter()
        val entries = importer.mapToPasswordEntries(emptyList())

        assertEquals(emptyList<Any>(), entries)
    }
}