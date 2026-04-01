package com.bulkrenamer.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RenameJournalDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RenameJournalDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.renameJournalDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun entry(
        batchId: String,
        originalName: String,
        newName: String,
        timestamp: Long = System.currentTimeMillis()
    ) = RenameJournalEntity(
        batchId = batchId,
        originalUri = "content://test/$originalName",
        originalName = originalName,
        newUri = "content://test/$newName",
        newName = newName,
        timestamp = timestamp
    )

    @Test
    fun `insert and query by batchId returns all entries`() = runTest {
        val batchId = "batch-001"
        dao.insert(entry(batchId, "a.jpg", "new_a.jpg"))
        dao.insert(entry(batchId, "b.jpg", "new_b.jpg"))
        dao.insert(entry(batchId, "c.jpg", "new_c.jpg"))

        val results = dao.getBatchEntries(batchId)
        assertEquals(3, results.size)
        assertTrue(results.all { it.batchId == batchId })
    }

    @Test
    fun `markBatchAsUndone sets undone flag for all entries in batch`() = runTest {
        val batchId = "batch-002"
        dao.insert(entry(batchId, "x.jpg", "y.jpg"))
        dao.insert(entry(batchId, "a.jpg", "b.jpg"))

        dao.markBatchAsUndone(batchId)

        val results = dao.getBatchEntries(batchId)
        assertTrue(results.all { it.undone })
    }

    @Test
    fun `getLastActiveBatch returns most recent non-undone batch`() = runTest {
        dao.insert(entry("batch-old", "a.jpg", "b.jpg", timestamp = 1000L))
        dao.insert(entry("batch-new", "c.jpg", "d.jpg", timestamp = 2000L))

        val last = dao.getLastActiveBatch()
        assertNotNull(last)
        assertEquals("batch-new", last!!.batchId)
    }

    @Test
    fun `getLastActiveBatch returns null when all batches are undone`() = runTest {
        dao.insert(entry("batch-a", "x.jpg", "y.jpg"))
        dao.markBatchAsUndone("batch-a")

        val last = dao.getLastActiveBatch()
        assertNull(last)
    }

    @Test
    fun `searchByFilename matches on originalName`() = runTest {
        dao.insert(entry("b1", "photo_001.jpg", "renamed.jpg"))
        dao.insert(entry("b2", "document.pdf", "new_doc.pdf"))

        val results = dao.searchByFilename("photo*")
        assertEquals(1, results.size)
        assertEquals("photo_001.jpg", results[0].originalName)
    }

    @Test
    fun `searchByFilename matches on newName`() = runTest {
        dao.insert(entry("b1", "old.jpg", "vacation_photo.jpg"))
        dao.insert(entry("b2", "doc.pdf", "report.pdf"))

        val results = dao.searchByFilename("vacation*")
        assertEquals(1, results.size)
        assertEquals("vacation_photo.jpg", results[0].newName)
    }

    @Test
    fun `getBatchSummaries returns correct file counts`() = runTest {
        dao.insert(entry("batch-x", "a.jpg", "1.jpg", 1000L))
        dao.insert(entry("batch-x", "b.jpg", "2.jpg", 1000L))
        dao.insert(entry("batch-y", "c.jpg", "3.jpg", 2000L))

        var summaries: List<BatchSummary> = emptyList()
        db.renameJournalDao().getBatchSummaries().collect { summaries = it }

        assertEquals(2, summaries.size)
        val batchX = summaries.first { it.batchId == "batch-x" }
        assertEquals(2, batchX.fileCount)
    }
}
