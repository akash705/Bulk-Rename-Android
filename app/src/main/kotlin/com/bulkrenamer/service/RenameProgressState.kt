package com.bulkrenamer.service

import com.bulkrenamer.data.model.RenameResult

sealed class RenameProgressState {
    object Idle : RenameProgressState()

    data class InProgress(
        val batchId: String,
        val total: Int,
        val completed: Int,
        val currentFileName: String
    ) : RenameProgressState() {
        val progress: Float get() = if (total == 0) 0f else completed.toFloat() / total
    }

    data class Completed(
        val batchId: String,
        val results: List<RenameResult>
    ) : RenameProgressState() {
        val successCount: Int get() = results.count { it.isSuccess }
        val failureCount: Int get() = results.count { !it.isSuccess }
    }

    data class Cancelled(val completedCount: Int) : RenameProgressState()
}
