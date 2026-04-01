package com.bulkrenamer.ui.state

import com.bulkrenamer.data.db.BatchSummary
import com.bulkrenamer.domain.UndoResult

data class HistoryUiState(
    val batches: List<BatchSummary> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val undoInProgress: Boolean = false,
    val lastUndoResult: UndoResult? = null
)
