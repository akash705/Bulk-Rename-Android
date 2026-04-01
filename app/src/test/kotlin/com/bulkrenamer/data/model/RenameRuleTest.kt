package com.bulkrenamer.data.model

import android.net.Uri
import android.provider.DocumentsContract
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RenameRuleTest {

    private fun fileNode(name: String, isDir: Boolean = false): FileNode {
        val uri = mockk<Uri>(relaxed = true)
        val mimeType = if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else "image/jpeg"
        return FileNode(
            documentId = "doc_$name",
            treeUri = uri,
            uri = uri,
            name = name,
            mimeType = mimeType,
            size = 1024,
            lastModified = System.currentTimeMillis(),
            flags = DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        )
    }

    // AddPrefix
    @Test fun `AddPrefix prepends to filename`() {
        assertEquals("2024_photo.jpg", RenameRule.AddPrefix("2024_").apply(fileNode("photo.jpg")))
    }

    @Test fun `AddPrefix prepends to filename with no extension`() {
        assertEquals("2024_photo", RenameRule.AddPrefix("2024_").apply(fileNode("photo")))
    }

    // AddSuffix
    @Test fun `AddSuffix inserts before extension by default`() {
        assertEquals(
            "photo_backup.jpg",
            RenameRule.AddSuffix("_backup").apply(fileNode("photo.jpg"))
        )
    }

    @Test fun `AddSuffix appends after name when beforeExtension is false`() {
        assertEquals(
            "photo.jpg_backup",
            RenameRule.AddSuffix("_backup", beforeExtension = false).apply(fileNode("photo.jpg"))
        )
    }

    @Test fun `AddSuffix on file with no extension appends directly`() {
        assertEquals("photo_backup", RenameRule.AddSuffix("_backup").apply(fileNode("photo")))
    }

    // ChangeExtension
    @Test fun `ChangeExtension replaces extension`() {
        assertEquals("photo.png", RenameRule.ChangeExtension("png").apply(fileNode("photo.jpg")))
    }

    @Test fun `ChangeExtension with empty string strips extension`() {
        assertEquals("photo", RenameRule.ChangeExtension("").apply(fileNode("photo.jpg")))
    }

    @Test fun `ChangeExtension on directory returns unchanged name`() {
        assertEquals("MyFolder", RenameRule.ChangeExtension("txt").apply(fileNode("MyFolder", isDir = true)))
    }

    // SetBaseName
    @Test fun `SetBaseName keeps extension by default`() {
        assertEquals("vacation.jpg", RenameRule.SetBaseName("vacation").apply(fileNode("photo.jpg")))
    }

    @Test fun `SetBaseName replaces entire name when keepExtension false`() {
        assertEquals("vacation", RenameRule.SetBaseName("vacation", keepExtension = false).apply(fileNode("photo.jpg")))
    }

    @Test fun `SetBaseName on file with no extension`() {
        assertEquals("vacation", RenameRule.SetBaseName("vacation").apply(fileNode("photo")))
    }

    @Test fun `SetBaseName on directory ignores keepExtension`() {
        // Directories have no extension concept; returns baseName
        assertEquals("NewFolder", RenameRule.SetBaseName("NewFolder").apply(fileNode("OldFolder", isDir = true)))
    }

    // ReplaceText
    @Test fun `ReplaceText replaces all occurrences case-insensitively`() {
        assertEquals("Photo_001.jpg", RenameRule.ReplaceText("IMG_", "Photo_").apply(fileNode("IMG_001.jpg")))
    }

    @Test fun `ReplaceText is case-insensitive by default`() {
        assertEquals("Photo_001.jpg", RenameRule.ReplaceText("img_", "Photo_").apply(fileNode("IMG_001.jpg")))
    }

    @Test fun `ReplaceText case-sensitive does not match wrong case`() {
        assertEquals("IMG_001.jpg", RenameRule.ReplaceText("img_", "Photo_", caseSensitive = true).apply(fileNode("IMG_001.jpg")))
    }

    @Test fun `ReplaceText replaces multiple occurrences`() {
        assertEquals("aa_aa.jpg", RenameRule.ReplaceText("b", "a").apply(fileNode("bb_bb.jpg")))
    }
}
