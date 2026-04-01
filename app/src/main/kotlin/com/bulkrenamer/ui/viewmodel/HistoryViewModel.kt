package com.bulkrenamer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bulkrenamer.data.db.BatchSummary
import com.bulkrenamer.data.db.RenameJournalDao
import com.bulkrenamer.domain.UndoOperationUseCase
import com.bulkrenamer.ui.state.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val journalDao: RenameJournalDao,
    private val undoOperation: UndoOperationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Backing state for search query — used to trigger filtered queries
    private val _searchQuery = MutableStateFlow("")

    // Cached full batch list for filtering without re-querying DB on every keystroke
    private var allBatches: List<BatchSummary> = emptyList()

    init {
        // Observe the live batch list from Room
        viewModelScope.launch {
            journalDao.getBatchSummaries().collectLatest { batches ->
                allBatches = batches
                applyFilter()
            }
        }

        // Debounce search to avoid running FTS queries on every keystroke
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    applyFilter(query)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSearch() {
        onSearchQueryChange("")
    }

    fun undoBatch(batchId: String) {
        if (_uiState.value.undoInProgress) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(undoInProgress = true, lastUndoResult = null)
            val result = undoOperation.undoBatch(batchId)
            _uiState.value = _uiState.value.copy(
                undoInProgress = false,
                lastUndoResult = result
            )
        }
    }

    fun dismissUndoResult() {
        _uiState.value = _uiState.value.copy(lastUndoResult = null)
    }

    private suspend fun applyFilter(query: String = _searchQuery.value) {
        val filtered = if (query.isBlank()) {
            allBatches
        } else {
            // FTS search returns journal entries; extract unique batchIds and filter batch list
            val matchingBatchIds = journalDao
                .searchByFilename(sanitizeFtsQuery(query))
                .map { it.batchId }
                .toSet()
            allBatches.filter { it.batchId in matchingBatchIds }
        }
        _uiState.value = _uiState.value.copy(
            batches = filtered,
            isLoading = false
        )
    }

    /**
     * FTS4 MATCH requires escaping special characters that have meaning in FTS queries.
     * Wrap the query in double-quotes to treat it as a phrase search.
     */
    private fun sanitizeFtsQuery(query: String): String {
        // Escape any existing double-quotes inside the query, then wrap in quotes
        val escaped = query.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
