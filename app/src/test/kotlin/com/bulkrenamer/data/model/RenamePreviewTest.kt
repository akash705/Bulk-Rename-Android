package com.bulkrenamer.data.model

import android.net.Uri
import android.provider.DocumentsContract
import com.bulkrenamer.domain.computePreview
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenamePreviewTest {

    private fun fileNode(name: String, isDir: Boolean = false): FileNode {
        val uri = mockk<Uri>(relaxed = true)
        return FileNode(
            documentId = "doc_$name",
            treeUri = uri,
            uri = uri,
            name = name,
            mimeType = if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else "image/jpeg",
            size = 1024,
            lastModified = 0L,
            flags = DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        )
    }

    @Test fun `prefix rule produces correct proposed names`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        val result = computePreview(files, listOf(RenameRule.AddPrefix("2024_")))
        assertEquals("2024_a.jpg", result[0].proposedName)
        assertEquals("2024_b.jpg", result[1].proposedName)
    }

    @Test fun `unchanged files are marked as unchanged`() {
        val files = listOf(fileNode("photo.jpg"))
        val result = computePreview(files, listOf(RenameRule.AddPrefix("")))
        assertTrue(result[0].isUnchanged)
    }

    @Test fun `within-batch duplicate produces auto-suffix`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        val result = computePreview(files, listOf(RenameRule.SetBaseName("same")))
        assertEquals("same.jpg", result[0].proposedName)
        assertEquals("same_1.jpg", result[1].proposedName)
        assertFalse(result[0].hasConflict)
        assertTrue(result[1].hasConflict)
    }

    @Test fun `collision with existing non-selected file triggers auto-suffix`() {
        val files = listOf(fileNode("a.jpg"))
        val existing = setOf("new.jpg")
        val result = computePreview(files, listOf(RenameRule.SetBaseName("new")), existingNamesInFolder = existing)
        assertEquals("new_1.jpg", result[0].proposedName)
        assertTrue(result[0].hasConflict)
    }

    @Test fun `auto-suffix increments until unique`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"), fileNode("c.jpg"))
        val existing = setOf("same_1.jpg")
        val result = computePreview(files, listOf(RenameRule.SetBaseName("same")), existingNamesInFolder = existing)
        assertEquals("same.jpg", result[0].proposedName)
        assertEquals("same_2.jpg", result[1].proposedName)
        assertEquals("same_3.jpg", result[2].proposedName)
    }

    @Test fun `empty proposed name returns validation error`() {
        val files = listOf(fileNode("abc"))
        val result = computePreview(files, listOf(RenameRule.ReplaceText("abc", "")))
        assertNotNull(result[0].validationError)
    }

    @Test fun `name with slash returns validation error`() {
        val files = listOf(fileNode("photo.jpg"))
        val result = computePreview(files, listOf(RenameRule.AddPrefix("a/b_")))
        assertNotNull(result[0].validationError)
    }

    @Test fun `name over 255 chars returns validation error`() {
        val files = listOf(fileNode("photo.jpg"))
        val longPrefix = "a".repeat(256)
        val result = computePreview(files, listOf(RenameRule.AddPrefix(longPrefix)))
        assertNotNull(result[0].validationError)
    }

    @Test fun `valid operation has no validation error`() {
        val files = listOf(fileNode("photo.jpg"))
        val result = computePreview(files, listOf(RenameRule.AddPrefix("2024_")))
        assertNull(result[0].validationError)
    }

    @Test fun `file with no extension auto-suffix appends before nothing`() {
        val files = listOf(fileNode("photo"), fileNode("photo2"))
        val result = computePreview(files, listOf(RenameRule.SetBaseName("doc")))
        assertEquals("doc", result[0].proposedName)
        assertEquals("doc_1", result[1].proposedName)
    }

    // Multi-rule chaining tests
    @Test fun `chained prefix and suffix apply in order`() {
        val files = listOf(fileNode("photo.jpg"), fileNode("snap.jpg"))
        val result = computePreview(
            files,
            listOf(RenameRule.AddPrefix("2024_"), RenameRule.AddSuffix("_v2"))
        )
        assertEquals("2024_photo_v2.jpg", result[0].proposedName)
        assertEquals("2024_snap_v2.jpg", result[1].proposedName)
    }

    @Test fun `numbering prefix assigns sequential index across batch`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"), fileNode("c.jpg"))
        val result = computePreview(
            files,
            listOf(RenameRule.AddNumbering(startAt = 1, separator = "_", position = NumberPosition.PREFIX))
        )
        assertEquals("1_a.jpg", result[0].proposedName)
        assertEquals("2_b.jpg", result[1].proposedName)
        assertEquals("3_c.jpg", result[2].proposedName)
    }

    @Test fun `numbering with padding produces zero-padded names`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        val result = computePreview(
            files,
            listOf(RenameRule.AddNumbering(startAt = 1, padWidth = 3, separator = "_", position = NumberPosition.PREFIX))
        )
        assertEquals("001_a.jpg", result[0].proposedName)
        assertEquals("002_b.jpg", result[1].proposedName)
    }

    @Test fun `chained numbering then replace works correctly`() {
        val files = listOf(fileNode("IMG_001.jpg"), fileNode("IMG_002.jpg"))
        val result = computePreview(
            files,
            listOf(
                RenameRule.ReplaceText("IMG_", "photo_"),
                RenameRule.AddNumbering(startAt = 10, step = 10, separator = "-", position = NumberPosition.SUFFIX)
            )
        )
        assertEquals("photo_001-10.jpg", result[0].proposedName)
        assertEquals("photo_002-20.jpg", result[1].proposedName)
    }

    @Test fun `empty rules list leaves all files unchanged`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        val result = computePreview(files, emptyList())
        assertTrue(result[0].isUnchanged)
        assertTrue(result[1].isUnchanged)
    }
}
