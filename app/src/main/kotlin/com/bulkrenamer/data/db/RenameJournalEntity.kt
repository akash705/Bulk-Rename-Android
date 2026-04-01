package com.bulkrenamer.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "rename_journal")
data class RenameJournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: String,
    val originalUri: String,
    val originalName: String,
    val newUri: String,
    val newName: String,
    val timestamp: Long,
    val undone: Boolean = false
)

// FTS4 virtual table for full-text search on filenames.
// Content table mirrors rename_journal for FTS queries.
@Fts4(contentEntity = RenameJournalEntity::class)
@Entity(tableName = "rename_journal_fts")
data class RenameJournalFtsEntity(
    val originalName: String,
    val newName: String
)
