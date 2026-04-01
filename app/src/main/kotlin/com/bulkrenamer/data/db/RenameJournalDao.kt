package com.bulkrenamer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class BatchSummary(
    val batchId: String,
    val timestamp: Long,
    val fileCount: Int,
    val undone: Boolean
)

@Dao
interface RenameJournalDao {

    @Insert
    suspend fun insert(entry: RenameJournalEntity)

    @Query("SELECT * FROM rename_journal WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getBatchEntries(batchId: String): List<RenameJournalEntity>

    @Query("""
        SELECT batchId, MAX(timestamp) as timestamp, COUNT(*) as fileCount, MAX(CASE WHEN undone THEN 1 ELSE 0 END) as undone
        FROM rename_journal
        GROUP BY batchId
        ORDER BY timestamp DESC
    """)
    fun getBatchSummaries(): Flow<List<BatchSummary>>

    @Query("""
        SELECT batchId, MAX(timestamp) as timestamp, COUNT(*) as fileCount, MAX(CASE WHEN undone THEN 1 ELSE 0 END) as undone
        FROM rename_journal
        WHERE undone = 0
        GROUP BY batchId
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getLastActiveBatch(): BatchSummary?

    @Query("UPDATE rename_journal SET undone = 1 WHERE batchId = :batchId")
    suspend fun markBatchAsUndone(batchId: String)

    /**
     * Full-text search across both originalName and newName using FTS4 MATCH.
     * Query special characters in filenames must be escaped before calling.
     */
    @Query("""
        SELECT j.* FROM rename_journal j
        INNER JOIN rename_journal_fts fts ON j.rowid = fts.rowid
        WHERE rename_journal_fts MATCH :query
        ORDER BY j.timestamp DESC
    """)
    suspend fun searchByFilename(query: String): List<RenameJournalEntity>

    @Query("SELECT COUNT(*) FROM rename_journal WHERE batchId = :batchId")
    suspend fun getBatchEntryCount(batchId: String): Int
}
