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
        val result = computePreview(files, RenameRule.AddPrefix("2024_"))
        assertEquals("2024_a.jpg", result[0].proposedName)
        assertEquals("2024_b.jpg", result[1].proposedName)
    }

    @Test fun `unchanged files are marked as unchanged`() {
        val files = listOf(fileNode("photo.jpg"))
        val result = computePreview(files, RenameRule.AddPrefix(""))
        assertTrue(result[0].isUnchanged)
    }

    @Test fun `within-batch duplicate produces auto-suffix`() {
        // SetBaseName("same") will try to give both files the same name
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        val result = computePreview(files, RenameRule.SetBaseName("same"))
        // First gets "same.jpg", second should get "same_1.jpg"
        assertEquals("same.jpg", result[0].proposedName)
        assertEquals("same_1.jpg", result[1].proposedName)
        assertFalse(result[0].hasConflict)
        assertTrue(result[1].hasConflict)
    }

    @Test fun `collision with existing non-selected file triggers auto-suffix`() {
        val files = listOf(fileNode("a.jpg"))
        val existing = setOf("new.jpg") // a non-selected file already named "new.jpg"
        val result = computePreview(files, RenameRule.SetBaseName("new"), existingNamesInFolder = existing)
        assertEquals("new_1.jpg", result[0].proposedName)
        assertTrue(result[0].hasConflict)
    }

    @Test fun `auto-suffix increments until unique`() {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"), fileNode("c.jpg"))
        val existing = setOf("same_1.jpg")
        val result = computePreview(files, RenameRule.SetBaseName("same"), existingNamesInFolder = existing)
        // file 0: "same.jpg" (no conflict)
        // file 1: "same_1.jpg" conflicts with existing → "same_2.jpg"
        // file 2: "same_3.jpg"
        assertEquals("same.jpg", result[0].proposedName)
        assertEquals("same_2.jpg", result[1].proposedName)
        assertEquals("same_3.jpg", result[2].proposedName)
    }

    @Test fun `empty proposed name returns validation error`() {
        // ReplaceText that removes all chars
        val files = listOf(fileNode("abc"))
        val result = computePreview(files, RenameRule.ReplaceText("abc", ""))
        assertNotNull(result[0].validationError)
    }

    @Test fun `name with slash returns validation error`() {
        val files = listOf(fileNode("photo.jpg"))
        val result = computePreview(files, RenameRule.AddPrefix("a/b_"))
        assertNotNull(result[0].validationError)
    }

    @Test fun `name over 255 chars returns validation error`() {
        val files = listOf(fileNode("photo.jpg"))
        val longPrefix = "a".repeat(256)
        val result = computePreview(files, RenameRule.AddPrefix(longPrefix))
        assertNotNull(result[0].validationError)
    }

    @Test fun `valid operation has no validation error`() {
        val files = listOf(fileNode("photo.jpg"))
        val result = computePreview(files, RenameRule.AddPrefix("2024_"))
        assertNull(result[0].validationError)
    }

    @Test fun `file with no extension auto-suffix appends before nothing`() {
        val files = listOf(fileNode("photo"), fileNode("photo2"))
        val result = computePreview(files, RenameRule.SetBaseName("doc"))
        assertEquals("doc", result[0].proposedName)
        assertEquals("doc_1", result[1].proposedName)
    }
}
