package com.bulkrenamer.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.data.repository.FileSystemRepository
import com.bulkrenamer.domain.BrowseFilesUseCase
import com.bulkrenamer.domain.computePreview
import com.bulkrenamer.ui.state.FileExplorerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_NAV_STACK = "nav_stack"

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val browseFiles: BrowseFilesUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileExplorerUiState>(FileExplorerUiState.Loading)
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    // Navigation stack: list of URI strings, persisted across process death
    private var navStack: List<String>
        get() = savedStateHandle[KEY_NAV_STACK] ?: emptyList()
        set(value) { savedStateHandle[KEY_NAV_STACK] = value }

    // Selection is folder-scoped and NOT persisted — clears on navigation
    private var currentSelection: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            val stack = navStack
            if (stack.isEmpty()) {
                checkPermissionsAndInit()
            } else {
                loadFolder(Uri.parse(stack.last()))
            }
        }
    }

    private suspend fun checkPermissionsAndInit() {
        val stale = fileSystemRepository.getStalePermissions()
        stale.forEach { fileSystemRepository.revokeUriPermission(it) }

        val allGranted = fileSystemRepository.getGrantedUriCount() > 0
        if (!allGranted) {
            _uiState.value = FileExplorerUiState.PermissionRequired
        } else {
            // This shouldn't normally happen (navStack would have URIs), but recover gracefully
            _uiState.value = FileExplorerUiState.PermissionRequired
        }
    }

    fun onFolderGranted(uri: Uri) {
        viewModelScope.launch {
            fileSystemRepository.persistUriPermission(uri)
            navStack = listOf(uri.toString())
            currentSelection = emptySet()
            loadFolder(uri)
        }
    }

    fun navigateTo(folderUri: Uri) {
        navStack = navStack + folderUri.toString()
        currentSelection = emptySet()
        viewModelScope.launch { loadFolder(folderUri) }
    }

    fun navigateUp(): Boolean {
        val stack = navStack
        if (stack.size <= 1) return false
        val newStack = stack.dropLast(1)
        navStack = newStack
        currentSelection = emptySet()
        viewModelScope.launch { loadFolder(Uri.parse(newStack.last())) }
        return true
    }

    private suspend fun loadFolder(uri: Uri) {
        _uiState.value = FileExplorerUiState.Loading
        try {
            val files = browseFiles(uri)
            val nearQuota = fileSystemRepository.isNearUriQuota()
            _uiState.value = FileExplorerUiState.Browsing(
                currentUri = uri,
                displayPath = buildDisplayPath(),
                entries = files,
                selection = currentSelection,
                canGoUp = navStack.size > 1,
                isNearUriQuota = nearQuota
            )
        } catch (e: Exception) {
            _uiState.value = FileExplorerUiState.Error(
                message = e.message ?: "Failed to load folder",
                recoverable = true
            )
        }
    }

    fun toggleSelection(documentId: String) {
        currentSelection = if (documentId in currentSelection) {
            currentSelection - documentId
        } else {
            currentSelection + documentId
        }
        updateSelectionInState()
    }

    fun selectAll() {
        val browsing = _uiState.value as? FileExplorerUiState.Browsing ?: return
        // Only select non-directory files (directories cannot be renamed)
        currentSelection = browsing.entries
            .filter { !it.isDirectory && it.isRenameable }
            .map { it.documentId }
            .toSet()
        updateSelectionInState()
    }

    fun deselectAll() {
        currentSelection = emptySet()
        updateSelectionInState()
    }

    private fun updateSelectionInState() {
        val browsing = _uiState.value as? FileExplorerUiState.Browsing ?: return
        _uiState.value = browsing.copy(selection = currentSelection)
    }

    fun previewRename(rule: RenameRule) {
        val browsing = _uiState.value as? FileExplorerUiState.Browsing ?: return
        val selectedFiles = browsing.selectedFiles
        if (selectedFiles.isEmpty()) return

        // Pass existing non-selected filenames for collision detection
        val nonSelectedNames = browsing.entries
            .filter { it.documentId !in browsing.selection }
            .map { it.name }
            .toSet()

        val preview = computePreview(selectedFiles, rule, nonSelectedNames)
        _uiState.value = FileExplorerUiState.RenamePreviewing(
            previewItems = preview,
            rule = rule,
            selectedCount = selectedFiles.size
        )
    }

    fun backToBrowsing() {
        val stack = navStack
        if (stack.isEmpty()) {
            _uiState.value = FileExplorerUiState.PermissionRequired
            return
        }
        viewModelScope.launch { loadFolder(Uri.parse(stack.last())) }
    }

    fun retry() {
        val stack = navStack
        if (stack.isEmpty()) {
            viewModelScope.launch { checkPermissionsAndInit() }
        } else {
            viewModelScope.launch { loadFolder(Uri.parse(stack.last())) }
        }
    }

    private fun buildDisplayPath(): String {
        // Simple path derived from stack depth; actual display path set by UI from URI
        return navStack.size.let { depth ->
            if (depth == 1) "/" else "/${navStack.drop(1).size} levels deep"
        }
    }
}
