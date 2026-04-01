package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.data.db.RenameJournalEntity
import com.bulkrenamer.data.model.RenameResult
import com.bulkrenamer.data.repository.FileSystemRepository
import com.bulkrenamer.service.RenameProgressState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class RenameOperation(
    val uri: Uri,
    val originalName: String,
    val newName: String,
    val batchId: String
)

@Singleton
class RenameFilesUseCase @Inject constructor(
    private val repository: FileSystemRepository,
    private val journalDao: RenameJournalDao
) {

    // StateFlow: progress is state, not events — new collectors get latest value immediately
    private val _progress = MutableStateFlow<RenameProgressState>(RenameProgressState.Idle)
    val progress: StateFlow<RenameProgressState> = _progress.asStateFlow()

    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    /**
     * Execute [operations] sequentially. SAF ContentProvider IPC is serialized —
     * parallel calls add no throughput benefit and risk ANR.
     *
     * Journal-before-proceed: each successful rename is journaled *before* moving
     * to the next file, so undo data survives process death mid-batch.
     */
    suspend fun executeBatch(operations: List<RenameOperation>): List<RenameResult> =
        withContext(Dispatchers.IO) {
            cancelled = false
            val batchId = operations.firstOrNull()?.batchId ?: UUID.randomUUID().toString()
            val results = mutableListOf<RenameResult>()

            _progress.value = RenameProgressState.InProgress(
                batchId = batchId,
                total = operations.size,
                completed = 0,
                currentFileName = operations.firstOrNull()?.originalName ?: ""
            )

            operations.forEachIndexed { index, op ->
                if (cancelled || !isActive) {
                    _progress.value = RenameProgressState.Cancelled(results.size)
                    return@withContext results
                }

                _progress.value = RenameProgressState.InProgress(
                    batchId = batchId,
                    total = operations.size,
                    completed = index,
                    currentFileName = op.originalName
                )

                try {
                    val newUri = repository.renameDocument(op.uri, op.newName)
                        ?: throw IOException("renameTo returned null for '${op.originalName}'")

                    // Journal AFTER successful rename, BEFORE next file — enables undo
                    journalDao.insert(
                        RenameJournalEntity(
                            batchId = batchId,
                            originalUri = op.uri.toString(),
                            originalName = op.originalName,
                            newUri = newUri.toString(),
                            newName = op.newName,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    results.add(
                        RenameResult(
                            originalUri = op.uri,
                            originalName = op.originalName,
                            newName = op.newName,
                            newUri = newUri,
                            error = null
                        )
                    )
                } catch (e: Exception) {
                    results.add(
                        RenameResult(
                            originalUri = op.uri,
                            originalName = op.originalName,
                            newName = op.newName,
                            newUri = null,
                            error = e
                        )
                    )
                    // Per-file errors do not stop the batch
                }
            }

            _progress.value = RenameProgressState.Completed(batchId = batchId, results = results)
            results
        }
}
