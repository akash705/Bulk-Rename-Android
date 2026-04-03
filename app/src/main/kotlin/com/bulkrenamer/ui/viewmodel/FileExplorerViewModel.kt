package com.bulkrenamer.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.data.repository.FileSystemRepository
import com.bulkrenamer.domain.BrowseFilesUseCase
import com.bulkrenamer.domain.ConflictStrategy
import com.bulkrenamer.domain.RenameFilesUseCase
import com.bulkrenamer.domain.RenameOperation
import com.bulkrenamer.domain.UndoOperationUseCase
import com.bulkrenamer.domain.computePreview
import com.bulkrenamer.service.RenameProgressState
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.state.FileFilter
import com.bulkrenamer.ui.state.RenameError
import com.bulkrenamer.ui.state.SortDirection
import com.bulkrenamer.ui.state.SortField
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val KEY_NAV_STACK = "nav_stack"

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val browseFiles: BrowseFilesUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val renameFilesUseCase: RenameFilesUseCase,
    private val undoOperation: UndoOperationUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileExplorerUiState>(FileExplorerUiState.Loading)
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    // Navigation stack: list of absolute paths, persisted across process death
    private var navStack: List<String>
        get() = savedStateHandle[KEY_NAV_STACK] ?: emptyList()
        set(value) { savedStateHandle[KEY_NAV_STACK] = value }

    private var currentSelection: Set<String> = emptySet()
    private var currentSortField: SortField = SortField.NAME
    private var currentSortDirection: SortDirection = SortDirection.ASC
    private var currentFilter: FileFilter = FileFilter.ALL
    private var showHiddenFiles: Boolean = false
    private var rawEntries: List<com.bulkrenamer.data.model.FileNode> = emptyList()

    // Cached inputs for re-running preview when conflict strategy changes
    private var cachedSelectedFiles: List<com.bulkrenamer.data.model.FileNode> = emptyList()
    private var cachedNonSelectedNames: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            val stack = navStack
            if (stack.isNotEmpty()) {
                loadFolder(stack.last())
            } else {
                checkPermissionAndInit()
            }
        }
    }

    private fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    private suspend fun checkPermissionAndInit() {
        if (!hasStoragePermission()) {
            _uiState.value = FileExplorerUiState.PermissionRequired
            return
        }
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        navStack = listOf(rootPath)
        loadFolder(rootPath)
    }

    /** Called by the permission screen once the user has granted access. */
    fun onPermissionGranted() {
        viewModelScope.launch { checkPermissionAndInit() }
    }

    fun navigateTo(path: String) {
        navStack = navStack + path
        currentSelection = emptySet()
        viewModelScope.launch { loadFolder(path) }
    }

    fun navigateToBreadcrumb(path: String) {
        val root = Environment.getExternalStorageDirectory().absolutePath
        // Rebuild the nav stack up to the target path
        val newStack = mutableListOf(root)
        if (path != root && path.startsWith(root)) {
            val relative = path.removePrefix(root).trimStart('/')
            var current = root
            for (part in relative.split("/")) {
                if (part.isNotEmpty()) {
                    current = "$current/$part"
                    newStack.add(current)
                }
            }
        }
        navStack = newStack
        currentSelection = emptySet()
        viewModelScope.launch { loadFolder(path) }
    }

    fun navigateUp(): Boolean {
        val stack = navStack
        if (stack.size <= 1) return false
        val newStack = stack.dropLast(1)
        navStack = newStack
        currentSelection = emptySet()
        viewModelScope.launch { loadFolder(newStack.last()) }
        return true
    }

    private suspend fun loadFolder(path: String) {
        _uiState.value = FileExplorerUiState.Loading
        try {
            rawEntries = browseFiles(path)
            _uiState.value = FileExplorerUiState.Browsing(
                currentPath = path,
                displayPath = buildDisplayPath(path),
                entries = applySortAndFilter(rawEntries),
                selection = currentSelection,
                canGoUp = navStack.size > 1,
                sortField = currentSortField,
                sortDirection = currentSortDirection,
                fileFilter = currentFilter,
                showHiddenFiles = showHiddenFiles,
                breadcrumbs = buildBreadcrumbs(path)
            )
        } catch (e: Exception) {
            _uiState.value = FileExplorerUiState.Error(
                message = e.message ?: "Failed to load folder",
                recoverable = true
            )
        }
    }

    fun setSortField(field: SortField) {
        if (currentSortField == field) {
            currentSortDirection = if (currentSortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            currentSortField = field
            currentSortDirection = SortDirection.ASC
        }
        refreshBrowsingState()
    }

    fun setFilter(filter: FileFilter) {
        currentFilter = filter
        refreshBrowsingState()
    }

    fun toggleShowHiddenFiles() {
        showHiddenFiles = !showHiddenFiles
        refreshBrowsingState()
    }

    private fun refreshBrowsingState() {
        val browsing = _uiState.value as? FileExplorerUiState.Browsing ?: return
        _uiState.value = browsing.copy(
            entries = applySortAndFilter(rawEntries),
            sortField = currentSortField,
            sortDirection = currentSortDirection,
            fileFilter = currentFilter,
            showHiddenFiles = showHiddenFiles
        )
    }

    private fun applySortAndFilter(
        files: List<com.bulkrenamer.data.model.FileNode>
    ): List<com.bulkrenamer.data.model.FileNode> {
        val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic")
        val videoExts = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
        val docExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "odt")

        val visible = if (showHiddenFiles) files else files.filter { !it.name.startsWith(".") }

        val filtered = when (currentFilter) {
            FileFilter.ALL -> visible
            FileFilter.FILES_ONLY -> visible.filter { !it.isDirectory }
            FileFilter.FOLDERS_ONLY -> visible.filter { it.isDirectory }
            FileFilter.IMAGES -> visible.filter { it.isDirectory || it.extension.lowercase() in imageExts }
            FileFilter.VIDEOS -> visible.filter { it.isDirectory || it.extension.lowercase() in videoExts }
            FileFilter.DOCUMENTS -> visible.filter { it.isDirectory || it.extension.lowercase() in docExts }
        }

        val comparator: Comparator<com.bulkrenamer.data.model.FileNode> = when (currentSortField) {
            SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortField.SIZE -> compareBy { it.size }
            SortField.DATE -> compareBy { it.lastModified }
            SortField.EXTENSION -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.extension }
        }

        // Directories first, then sort within each group
        val dirs = filtered.filter { it.isDirectory }.sortedWith(comparator)
        val nonDirs = filtered.filter { !it.isDirectory }.sortedWith(comparator)

        val sortedDirs = if (currentSortDirection == SortDirection.DESC) dirs.reversed() else dirs
        val sortedFiles = if (currentSortDirection == SortDirection.DESC) nonDirs.reversed() else nonDirs

        return sortedDirs + sortedFiles
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

    fun previewRename(rules: List<RenameRule>) {
        val browsing = _uiState.value as? FileExplorerUiState.Browsing ?: return
        val selectedFiles = browsing.selectedFiles
        if (selectedFiles.isEmpty() || rules.isEmpty()) return

        val nonSelectedNames = browsing.entries
            .filter { it.documentId !in browsing.selection }
            .map { it.name }
            .toSet()

        cachedSelectedFiles = selectedFiles
        cachedNonSelectedNames = nonSelectedNames

        val preview = computePreview(selectedFiles, rules, nonSelectedNames)
        _uiState.value = FileExplorerUiState.RenamePreviewing(
            previewItems = preview,
            rules = rules,
            selectedCount = selectedFiles.size
        )
    }

    fun setCopyMode(createCopy: Boolean) {
        val previewing = _uiState.value as? FileExplorerUiState.RenamePreviewing ?: return
        _uiState.value = previewing.copy(createCopy = createCopy)
    }

    fun setGlobalConflictStrategy(strategy: ConflictStrategy) {
        val previewing = _uiState.value as? FileExplorerUiState.RenamePreviewing ?: return
        val preview = computePreview(
            cachedSelectedFiles,
            previewing.rules,
            cachedNonSelectedNames,
            strategy
        )
        _uiState.value = previewing.copy(previewItems = preview, globalConflictStrategy = strategy)
    }

    fun setItemConflictStrategy(documentId: String, strategy: ConflictStrategy) {
        val previewing = _uiState.value as? FileExplorerUiState.RenamePreviewing ?: return
        val perItemStrategies = previewing.previewItems
            .filter { it.hasConflict }
            .associate { it.fileNode.documentId to it.conflictStrategy }
            .toMutableMap()
        perItemStrategies[documentId] = strategy
        val preview = computePreview(
            cachedSelectedFiles,
            previewing.rules,
            cachedNonSelectedNames,
            previewing.globalConflictStrategy,
            perItemStrategies
        )
        _uiState.value = previewing.copy(previewItems = preview)
    }

    fun confirmRename(previewingState: FileExplorerUiState.RenamePreviewing) {
        val batchId = UUID.randomUUID().toString()

        val ops = previewingState.previewItems
            .filter { !it.isUnchanged && !it.isSkipped && it.validationError == null }
            .map { item ->
                RenameOperation(
                    uri = item.fileNode.uri,
                    originalName = item.fileNode.name,
                    newName = item.proposedName,
                    batchId = batchId,
                    overwrite = item.conflictStrategy == ConflictStrategy.OVERWRITE,
                    createCopy = previewingState.createCopy
                )
            }

        if (ops.isEmpty()) {
            backToBrowsing()
            return
        }

        viewModelScope.launch {
            _uiState.value = FileExplorerUiState.RenameInProgress(
                total = ops.size,
                completed = 0,
                currentFileName = ops.first().originalName,
                batchId = batchId
            )

            val progressJob = launch {
                renameFilesUseCase.progress
                    .filterIsInstance<RenameProgressState.InProgress>()
                    .collect { p ->
                        _uiState.value = FileExplorerUiState.RenameInProgress(
                            total = p.total,
                            completed = p.completed,
                            currentFileName = p.currentFileName,
                            batchId = p.batchId
                        )
                    }
            }

            val results = renameFilesUseCase.executeBatch(ops)
            progressJob.cancel()

            val errors = results
                .filter { !it.isSuccess }
                .map { result ->
                    RenameError(
                        originalName = result.originalName,
                        newName = result.newName,
                        reason = result.error?.message ?: "Unknown error"
                    )
                }

            _uiState.value = FileExplorerUiState.RenameResult(
                successCount = results.count { it.isSuccess },
                failureCount = errors.size,
                errors = errors,
                batchId = batchId
            )
        }
    }

    fun cancelRename() { renameFilesUseCase.cancel() }

    fun undoLastBatch(batchId: String) {
        viewModelScope.launch {
            undoOperation.undoBatch(batchId)
            backToBrowsing()
        }
    }

    fun backToBrowsing() {
        val stack = navStack
        if (stack.isEmpty()) {
            _uiState.value = FileExplorerUiState.PermissionRequired
            return
        }
        viewModelScope.launch { loadFolder(stack.last()) }
    }

    fun retry() {
        viewModelScope.launch {
            val stack = navStack
            if (stack.isEmpty()) checkPermissionAndInit()
            else loadFolder(stack.last())
        }
    }

    private fun buildDisplayPath(path: String): String {
        val root = Environment.getExternalStorageDirectory().absolutePath
        return when {
            path == root -> "Internal Storage"
            path.startsWith(root) -> path.removePrefix(root)
            else -> path
        }
    }

    private fun buildBreadcrumbs(path: String): List<FileExplorerUiState.BreadcrumbSegment> {
        val root = Environment.getExternalStorageDirectory().absolutePath
        val segments = mutableListOf(
            FileExplorerUiState.BreadcrumbSegment("Internal Storage", root)
        )
        if (path != root && path.startsWith(root)) {
            val relative = path.removePrefix(root).trimStart('/')
            var current = root
            for (part in relative.split("/")) {
                if (part.isNotEmpty()) {
                    current = "$current/$part"
                    segments.add(FileExplorerUiState.BreadcrumbSegment(part, current))
                }
            }
        }
        return segments
    }
}
