package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.data.repository.FileSystemRepository
import com.bulkrenamer.service.RenameProgressState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RenameFilesUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FileSystemRepository
    private lateinit var journalDao: RenameJournalDao
    private lateinit var useCase: RenameFilesUseCase

    private fun testUri(name: String): Uri {
        val uri = mockk<Uri>(relaxed = true)
        io.mockk.every { uri.toString() } returns "content://test/$name"
        return uri
    }

    private fun op(
        originalName: String,
        newName: String,
        batchId: String = UUID.randomUUID().toString()
    ) = RenameOperation(
        uri = testUri(originalName),
        originalName = originalName,
        newName = newName,
        batchId = batchId
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        journalDao = mockk(relaxed = true)
        useCase = RenameFilesUseCase(repository, journalDao)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `all files succeed — results all success, journal has 3 entries`() = runTest {
        val batchId = "batch-001"
        val ops = listOf(
            op("a.jpg", "new_a.jpg", batchId),
            op("b.jpg", "new_b.jpg", batchId),
            op("c.jpg", "new_c.jpg", batchId)
        )
        ops.forEach { coEvery { repository.renameDocument(it.uri, it.newName) } returns testUri(it.newName) }

        val results = useCase.executeBatch(ops)

        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
        coVerify(exactly = 3) { journalDao.insert(any()) }
    }

    @Test
    fun `file 3 fails — files 1-2 and 4-5 renamed, file 3 error recorded`() = runTest {
        val batchId = "batch-002"
        val ops = listOf(
            op("a.jpg", "1.jpg", batchId),
            op("b.jpg", "2.jpg", batchId),
            op("c.jpg", "3.jpg", batchId), // will fail
            op("d.jpg", "4.jpg", batchId),
            op("e.jpg", "5.jpg", batchId)
        )
        coEvery { repository.renameDocument(ops[0].uri, any()) } returns testUri("1.jpg")
        coEvery { repository.renameDocument(ops[1].uri, any()) } returns testUri("2.jpg")
        coEvery { repository.renameDocument(ops[2].uri, any()) } returns null  // failure
        coEvery { repository.renameDocument(ops[3].uri, any()) } returns testUri("4.jpg")
        coEvery { repository.renameDocument(ops[4].uri, any()) } returns testUri("5.jpg")

        val results = useCase.executeBatch(ops)

        assertEquals(5, results.size)
        assertEquals(4, results.count { it.isSuccess })
        assertEquals(1, results.count { !it.isSuccess })
        assertNull(results[2].newUri)
        assertNotNull(results[2].error)
        // Journal has 4 entries (not 5 — failed file not journaled)
        coVerify(exactly = 4) { journalDao.insert(any()) }
    }

    @Test
    fun `all files fail — 0 successes, 0 journal entries`() = runTest {
        val batchId = "batch-003"
        val ops = listOf(op("a.jpg", "1.jpg", batchId), op("b.jpg", "2.jpg", batchId))
        ops.forEach { coEvery { repository.renameDocument(it.uri, any()) } returns null }

        val results = useCase.executeBatch(ops)

        assertEquals(2, results.size)
        assertTrue(results.none { it.isSuccess })
        coVerify(exactly = 0) { journalDao.insert(any()) }
    }

    @Test
    fun `progress emits correct states through batch`() = runTest {
        val batchId = "batch-004"
        val ops = listOf(op("x.jpg", "y.jpg", batchId))
        coEvery { repository.renameDocument(ops[0].uri, any()) } returns testUri("y.jpg")

        val progressValues = mutableListOf<RenameProgressState>()
        val job = kotlinx.coroutines.launch {
            useCase.progress.collect { progressValues.add(it) }
        }

        useCase.executeBatch(ops)
        job.cancel()

        assertTrue(progressValues.any { it is RenameProgressState.InProgress })
        assertTrue(progressValues.any { it is RenameProgressState.Completed })
    }

    @Test
    fun `cancel mid-batch stops remaining files`() = runTest {
        val batchId = "batch-005"
        val ops = (1..5).map { op("file$it.jpg", "new$it.jpg", batchId) }

        // File 1 renames and then we cancel
        coEvery { repository.renameDocument(ops[0].uri, any()) } answers {
            useCase.cancel()
            testUri("new1.jpg")
        }
        ops.drop(1).forEach { coEvery { repository.renameDocument(it.uri, any()) } returns testUri(it.newName) }

        val results = useCase.executeBatch(ops)

        // Only file 1 completed before cancel
        assertTrue(results.size <= 2)
    }
}
