package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.data.db.RenameJournalEntity
import com.bulkrenamer.data.repository.FileSystemRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UndoOperationUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FileSystemRepository
    private lateinit var journalDao: RenameJournalDao
    private lateinit var useCase: UndoOperationUseCase

    /** Builds a journal entry whose newUri encodes the path as a file:// URI. */
    private fun entry(
        originalName: String,
        newName: String,
        batchId: String = "batch-1"
    ) = RenameJournalEntity(
        id = 0,
        batchId = batchId,
        originalUri = "file:///test/$originalName",
        originalName = originalName,
        newUri = "file:///test/$newName",
        newName = newName,
        timestamp = System.currentTimeMillis()
    )

    /** Returns the path that UndoOperationUseCase derives from a file:// newUri. */
    private fun pathOf(newName: String) = "/test/$newName"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        journalDao = mockk(relaxed = true)
        useCase = UndoOperationUseCase(repository, journalDao)

        // Uri.parse is used inside UndoOperationUseCase to extract the path from newUri.
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val uriStr = firstArg<String>()
            mockk<Uri>(relaxed = true).also { uri ->
                every { uri.path } returns uriStr.removePrefix("file://")
            }
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `all files still at expected names — all reversed`() = runTest {
        val batchId = "batch-1"
        val entries = listOf(
            entry("a.jpg", "new_a.jpg", batchId),
            entry("b.jpg", "new_b.jpg", batchId)
        )
        coEvery { journalDao.getBatchEntries(batchId) } returns entries
        entries.forEach { e ->
            coEvery { repository.getFileName(pathOf(e.newName)) } returns e.newName
            coEvery { repository.renameFile(pathOf(e.newName), e.originalName) } returns pathOf(e.originalName)
        }

        val result = useCase.undoBatch(batchId)

        assertEquals(2, result.reversedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.failedCount)
        assertFalse(result.hasWarning)
        coVerify(exactly = 1) { journalDao.markBatchAsUndone(batchId) }
    }

    @Test
    fun `file renamed again by later batch — skipped, not failed`() = runTest {
        val batchId = "batch-2"
        val entries = listOf(
            entry("original.jpg", "renamed.jpg", batchId),
            entry("other.jpg", "other_new.jpg", batchId)
        )
        coEvery { journalDao.getBatchEntries(batchId) } returns entries

        // First file: current name differs from journaled newName → later batch renamed it
        coEvery { repository.getFileName(pathOf("renamed.jpg")) } returns "yet_another_name.jpg"

        // Second file: current name matches → can undo
        coEvery { repository.getFileName(pathOf("other_new.jpg")) } returns "other_new.jpg"
        coEvery { repository.renameFile(pathOf("other_new.jpg"), "other.jpg") } returns pathOf("other.jpg")

        val result = useCase.undoBatch(batchId)

        assertEquals(1, result.reversedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.hasWarning)
        assertNotNull(result.warningMessage)
    }

    @Test
    fun `file no longer exists — skipped`() = runTest {
        val batchId = "batch-3"
        val entries = listOf(entry("x.jpg", "x_new.jpg", batchId))
        coEvery { journalDao.getBatchEntries(batchId) } returns entries

        // null means the file is gone
        coEvery { repository.getFileName(pathOf("x_new.jpg")) } returns null

        val result = useCase.undoBatch(batchId)

        assertEquals(0, result.reversedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.hasWarning)
        coVerify(exactly = 0) { repository.renameFile(any(), any()) }
    }

    @Test
    fun `rename back fails — counted as failed`() = runTest {
        val batchId = "batch-4"
        val entries = listOf(entry("f.jpg", "f_new.jpg", batchId))
        coEvery { journalDao.getBatchEntries(batchId) } returns entries

        coEvery { repository.getFileName(pathOf("f_new.jpg")) } returns "f_new.jpg"
        // renameFile returns null → failure
        coEvery { repository.renameFile(pathOf("f_new.jpg"), "f.jpg") } returns null

        val result = useCase.undoBatch(batchId)

        assertEquals(0, result.reversedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(1, result.failedCount)
        assertFalse(result.hasWarning) // failedCount does not set hasWarning
        coVerify(exactly = 1) { journalDao.markBatchAsUndone(batchId) }
    }

    @Test
    fun `empty batch — no-op, batch still marked undone`() = runTest {
        val batchId = "batch-5"
        coEvery { journalDao.getBatchEntries(batchId) } returns emptyList()

        val result = useCase.undoBatch(batchId)

        assertEquals(0, result.reversedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.failedCount)
        coVerify(exactly = 1) { journalDao.markBatchAsUndone(batchId) }
    }
}
