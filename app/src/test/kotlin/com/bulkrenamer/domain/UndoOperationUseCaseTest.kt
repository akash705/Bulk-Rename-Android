package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.data.db.RenameJournalEntity
import com.bulkrenamer.data.repository.FileSystemRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

    private fun fakeUri(name: String): Uri {
        val uri = mockk<Uri>(relaxed = true)
        io.mockk.every { uri.toString() } returns "content://test/$name"
        io.mockk.every { Uri.parse("content://test/$name") } returns uri
        return uri
    }

    private fun entry(
        originalName: String,
        newName: String,
        batchId: String = "batch-1"
    ) = RenameJournalEntity(
        id = 0,
        batchId = batchId,
        originalUri = "content://test/$originalName",
        originalName = originalName,
        newUri = "content://test/$newName",
        newName = newName,
        timestamp = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        journalDao = mockk(relaxed = true)
        useCase = UndoOperationUseCase(repository, journalDao)
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
            val uri = mockk<Uri>(relaxed = true)
            io.mockk.every { uri.toString() } returns e.newUri
            io.mockk.every { Uri.parse(e.newUri) } returns uri
            coEvery { repository.getDocumentDisplayName(uri) } returns e.newName
            coEvery { repository.renameDocument(uri, e.originalName) } returns mockk(relaxed = true)
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

        // First file: current name is DIFFERENT from journaled newName → later batch renamed it
        val uri1 = mockk<Uri>(relaxed = true)
        io.mockk.every { uri1.toString() } returns entries[0].newUri
        io.mockk.every { Uri.parse(entries[0].newUri) } returns uri1
        coEvery { repository.getDocumentDisplayName(uri1) } returns "yet_another_name.jpg"

        // Second file: current name matches → can undo
        val uri2 = mockk<Uri>(relaxed = true)
        io.mockk.every { uri2.toString() } returns entries[1].newUri
        io.mockk.every { Uri.parse(entries[1].newUri) } returns uri2
        coEvery { repository.getDocumentDisplayName(uri2) } returns entries[1].newName
        coEvery { repository.renameDocument(uri2, entries[1].originalName) } returns mockk(relaxed = true)

        val result = useCase.undoBatch(batchId)

        assertEquals(1, result.reversedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.hasWarning)
        assertNotNull(result.warningMessage)
    }

    @Test
    fun `URI dead — skipped`() = runTest {
        val batchId = "batch-3"
        val entries = listOf(entry("x.jpg", "x_new.jpg", batchId))
        coEvery { journalDao.getBatchEntries(batchId) } returns entries

        val uri = mockk<Uri>(relaxed = true)
        io.mockk.every { uri.toString() } returns entries[0].newUri
        io.mockk.every { Uri.parse(entries[0].newUri) } returns uri
        // Null means the URI is dead
        coEvery { repository.getDocumentDisplayName(uri) } returns null

        val result = useCase.undoBatch(batchId)

        assertEquals(0, result.reversedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.hasWarning)
        coVerify(exactly = 0) { repository.renameDocument(any(), any()) }
    }

    @Test
    fun `provider rename fails — counted as failed`() = runTest {
        val batchId = "batch-4"
        val entries = listOf(entry("f.jpg", "f_new.jpg", batchId))
        coEvery { journalDao.getBatchEntries(batchId) } returns entries

        val uri = mockk<Uri>(relaxed = true)
        io.mockk.every { uri.toString() } returns entries[0].newUri
        io.mockk.every { Uri.parse(entries[0].newUri) } returns uri
        coEvery { repository.getDocumentDisplayName(uri) } returns entries[0].newName
        // Provider returns null — rename failed
        coEvery { repository.renameDocument(uri, entries[0].originalName) } returns null

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
