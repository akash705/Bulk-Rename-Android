package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.data.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class UndoResult(
    val batchId: String,
    val reversedCount: Int,
    val skippedCount: Int,
    val failedCount: Int
) {
    val hasWarning: Boolean get() = skippedCount > 0
    val warningMessage: String?
        get() = if (skippedCount > 0)
            "$skippedCount file(s) could not be reversed — already modified by a later batch."
        else null
}

@Singleton
class UndoOperationUseCase @Inject constructor(
    private val repository: FileSystemRepository,
    private val journalDao: RenameJournalDao
) {

    suspend fun undoBatch(batchId: String): UndoResult = withContext(Dispatchers.IO) {
        val entries = journalDao.getBatchEntries(batchId)

        var reversedCount = 0
        var skippedCount = 0
        var failedCount = 0

        for (entry in entries) {
            // newUri is a file:// URI; .path gives the absolute path after the rename
            val newPath = Uri.parse(entry.newUri).path
            if (newPath == null) { skippedCount++; continue }

            val currentName = repository.getFileName(newPath)
            if (currentName == null || currentName != entry.newName) {
                // File gone or already renamed again by a later batch
                skippedCount++
                continue
            }

            val resultPath = repository.renameFile(newPath, entry.originalName)
            if (resultPath != null) reversedCount++ else failedCount++
        }

        journalDao.markBatchAsUndone(batchId)

        UndoResult(
            batchId = batchId,
            reversedCount = reversedCount,
            skippedCount = skippedCount,
            failedCount = failedCount
        )
    }
}
