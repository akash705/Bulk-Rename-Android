package com.bulkrenamer.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RenameRuleTest {

    // Helper: apply a rule to a plain filename string (non-directory, index 0)
    private fun RenameRule.applyFile(name: String) = apply(name, isDirectory = false, index = 0)
    private fun RenameRule.applyDir(name: String) = apply(name, isDirectory = true, index = 0)

    // AddPrefix
    @Test fun `AddPrefix prepends to filename`() {
        assertEquals("2024_photo.jpg", RenameRule.AddPrefix("2024_").applyFile("photo.jpg"))
    }

    @Test fun `AddPrefix prepends to filename with no extension`() {
        assertEquals("2024_photo", RenameRule.AddPrefix("2024_").applyFile("photo"))
    }

    // AddSuffix
    @Test fun `AddSuffix inserts before extension by default`() {
        assertEquals("photo_backup.jpg", RenameRule.AddSuffix("_backup").applyFile("photo.jpg"))
    }

    @Test fun `AddSuffix appends after name when beforeExtension is false`() {
        assertEquals("photo.jpg_backup", RenameRule.AddSuffix("_backup", beforeExtension = false).applyFile("photo.jpg"))
    }

    @Test fun `AddSuffix on file with no extension appends directly`() {
        assertEquals("photo_backup", RenameRule.AddSuffix("_backup").applyFile("photo"))
    }

    // ChangeExtension
    @Test fun `ChangeExtension replaces extension`() {
        assertEquals("photo.png", RenameRule.ChangeExtension("png").applyFile("photo.jpg"))
    }

    @Test fun `ChangeExtension with empty string strips extension`() {
        assertEquals("photo", RenameRule.ChangeExtension("").applyFile("photo.jpg"))
    }

    @Test fun `ChangeExtension on directory returns unchanged name`() {
        assertEquals("MyFolder", RenameRule.ChangeExtension("txt").applyDir("MyFolder"))
    }

    // SetBaseName
    @Test fun `SetBaseName keeps extension by default`() {
        assertEquals("vacation.jpg", RenameRule.SetBaseName("vacation").applyFile("photo.jpg"))
    }

    @Test fun `SetBaseName replaces entire name when keepExtension false`() {
        assertEquals("vacation", RenameRule.SetBaseName("vacation", keepExtension = false).applyFile("photo.jpg"))
    }

    @Test fun `SetBaseName on file with no extension`() {
        assertEquals("vacation", RenameRule.SetBaseName("vacation").applyFile("photo"))
    }

    @Test fun `SetBaseName on directory ignores keepExtension`() {
        assertEquals("NewFolder", RenameRule.SetBaseName("NewFolder").applyDir("OldFolder"))
    }

    // ReplaceText
    @Test fun `ReplaceText replaces all occurrences case-insensitively`() {
        assertEquals("Photo_001.jpg", RenameRule.ReplaceText("IMG_", "Photo_").applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText is case-insensitive by default`() {
        assertEquals("Photo_001.jpg", RenameRule.ReplaceText("img_", "Photo_").applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText case-sensitive does not match wrong case`() {
        assertEquals("IMG_001.jpg", RenameRule.ReplaceText("img_", "Photo_", caseSensitive = true).applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText replaces multiple occurrences`() {
        assertEquals("aa_aa.jpg", RenameRule.ReplaceText("b", "a").applyFile("bb_bb.jpg"))
    }

    // ReplaceText with regex
    @Test fun `ReplaceText regex matches pattern`() {
        assertEquals("photo.jpg", RenameRule.ReplaceText("IMG_\\d+", "photo", useRegex = true).applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText regex with capture groups`() {
        assertEquals("001_photo.jpg", RenameRule.ReplaceText("IMG_(\\d+)", "$1_photo", useRegex = true).applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText regex case insensitive by default`() {
        assertEquals("photo.jpg", RenameRule.ReplaceText("img_\\d+", "photo", useRegex = true).applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText regex case sensitive does not match wrong case`() {
        assertEquals("IMG_001.jpg", RenameRule.ReplaceText("img_\\d+", "photo", caseSensitive = true, useRegex = true).applyFile("IMG_001.jpg"))
    }

    @Test fun `ReplaceText invalid regex returns name unchanged`() {
        assertEquals("photo.jpg", RenameRule.ReplaceText("[invalid", "x", useRegex = true).applyFile("photo.jpg"))
    }

    // AddNumbering
    @Test fun `AddNumbering prefix adds sequential number with separator`() {
        val rule = RenameRule.AddNumbering(startAt = 1, step = 1, separator = "_", position = NumberPosition.PREFIX)
        assertEquals("1_photo.jpg", rule.apply("photo.jpg", false, 0))
        assertEquals("2_photo.jpg", rule.apply("photo.jpg", false, 1))
        assertEquals("3_photo.jpg", rule.apply("photo.jpg", false, 2))
    }

    @Test fun `AddNumbering suffix inserts before extension`() {
        val rule = RenameRule.AddNumbering(startAt = 1, step = 1, separator = "_", position = NumberPosition.SUFFIX)
        assertEquals("photo_1.jpg", rule.apply("photo.jpg", false, 0))
        assertEquals("photo_2.jpg", rule.apply("photo.jpg", false, 1))
    }

    @Test fun `AddNumbering with padding zero-pads numbers`() {
        val rule = RenameRule.AddNumbering(startAt = 1, padWidth = 3, separator = "_", position = NumberPosition.PREFIX)
        assertEquals("001_photo.jpg", rule.apply("photo.jpg", false, 0))
        assertEquals("002_photo.jpg", rule.apply("photo.jpg", false, 1))
        assertEquals("010_photo.jpg", rule.apply("photo.jpg", false, 9))
    }

    @Test fun `AddNumbering custom step`() {
        val rule = RenameRule.AddNumbering(startAt = 10, step = 10, separator = "-", position = NumberPosition.PREFIX)
        assertEquals("10-photo.jpg", rule.apply("photo.jpg", false, 0))
        assertEquals("20-photo.jpg", rule.apply("photo.jpg", false, 1))
        assertEquals("30-photo.jpg", rule.apply("photo.jpg", false, 2))
    }

    @Test fun `AddNumbering suffix on file with no extension`() {
        val rule = RenameRule.AddNumbering(startAt = 1, separator = "_", position = NumberPosition.SUFFIX)
        assertEquals("readme_1", rule.apply("readme", false, 0))
    }

    // Multi-rule chaining (via fold, matching what computePreview does)
    @Test fun `chaining prefix then suffix produces correct result`() {
        val name = "photo.jpg"
        val isDir = false
        val result = listOf(
            RenameRule.AddPrefix("2024_"),
            RenameRule.AddSuffix("_v2")
        ).fold(name) { acc, rule -> rule.apply(acc, isDir, 0) }
        assertEquals("2024_photo_v2.jpg", result)
    }

    @Test fun `chaining replace then numbering produces correct result`() {
        val name = "IMG_001.jpg"
        val isDir = false
        val result = listOf(
            RenameRule.ReplaceText("IMG_", "photo_"),
            RenameRule.AddNumbering(startAt = 5, separator = "-", position = NumberPosition.SUFFIX)
        ).fold(name) { acc, rule -> rule.apply(acc, isDir, 0) }
        assertEquals("photo_001-5.jpg", result)
    }
}
