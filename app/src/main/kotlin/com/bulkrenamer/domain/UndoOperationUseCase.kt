package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.data.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of an undo operation.
 *
 * @param reversedCount Files successfully renamed back to their original names.
 * @param skippedCount  Files skipped because their URI was dead or filename had changed —
 *                      meaning a later batch had already renamed them. This matches the
 *                      "soften the promise" decision: we don't fail the whole undo, we
 *                      skip these files and warn the user.
 * @param failedCount   Files whose URI was valid and name matched, but the rename failed
 *                      (provider error, permissions revoked, etc.).
 */
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

    /**
     * Attempt to reverse all renames in [batchId].
     *
     * For each journal entry:
     * 1. Query the URI stored as [newUri] (the file as it exists post-rename).
     * 2. If the current display name matches [newName], the file is still in the
     *    expected state — rename it back to [originalName].
     * 3. If the URI is dead or the name doesn't match, a later batch has modified
     *    this file. Skip it and increment [UndoResult.skippedCount].
     *
     * The batch is marked undone regardless of partial skips so that history UI
     * reflects the attempt.
     */
    suspend fun undoBatch(batchId: String): UndoResult = withContext(Dispatchers.IO) {
        val entries = journalDao.getBatchEntries(batchId)

        var reversedCount = 0
        var skippedCount = 0
        var failedCount = 0

        for (entry in entries) {
            val newUri = Uri.parse(entry.newUri)

            // Validate: is the file still at newUri with name newName?
            val currentName = repository.getDocumentDisplayName(newUri)
            if (currentName == null || currentName != entry.newName) {
                // URI dead or file was renamed again by a later batch — skip
                skippedCount++
                continue
            }

            // Rename back to original name
            val resultUri = repository.renameDocument(newUri, entry.originalName)
            if (resultUri != null) {
                reversedCount++
            } else {
                failedCount++
            }
        }

        // Mark undone even on partial success so the batch doesn't appear as actionable
        journalDao.markBatchAsUndone(batchId)

        UndoResult(
            batchId = batchId,
            reversedCount = reversedCount,
            skippedCount = skippedCount,
            failedCount = failedCount
        )
    }
}
