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
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class RenameOperation(
    val uri: Uri,          // file:// URI — uri.path gives the absolute path
    val originalName: String,
    val newName: String,
    val batchId: String
)

@Singleton
class RenameFilesUseCase @Inject constructor(
    private val repository: FileSystemRepository,
    private val journalDao: RenameJournalDao
) {

    private val _progress = MutableStateFlow<RenameProgressState>(RenameProgressState.Idle)
    val progress: StateFlow<RenameProgressState> = _progress.asStateFlow()

    private var cancelled = false

    fun cancel() { cancelled = true }

    /**
     * Execute [operations] sequentially using [File.renameTo].
     * Journal-before-proceed: each rename is logged immediately so undo survives process death.
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

                val absolutePath = op.uri.path
                    ?: run {
                        results.add(RenameResult(op.uri, op.originalName, op.newName, null, IOException("Invalid URI")))
                        return@forEachIndexed
                    }

                try {
                    val newPath = repository.renameFile(absolutePath, op.newName)
                        ?: throw IOException("renameTo failed for '${op.originalName}'")

                    val newUri = Uri.fromFile(File(newPath))

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

                    results.add(RenameResult(op.uri, op.originalName, op.newName, newUri, null))
                } catch (e: Exception) {
                    results.add(RenameResult(op.uri, op.originalName, op.newName, null, e))
                }
            }

            _progress.value = RenameProgressState.Completed(batchId = batchId, results = results)
            results
        }
}
