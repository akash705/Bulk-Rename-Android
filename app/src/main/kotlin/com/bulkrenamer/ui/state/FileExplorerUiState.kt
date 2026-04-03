package com.bulkrenamer.ui.state

import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.domain.ConflictStrategy
import com.bulkrenamer.domain.RenamePreviewItem

enum class SortField(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date"),
    EXTENSION("Extension")
}

enum class SortDirection { ASC, DESC }

enum class FileFilter(val label: String) {
    ALL("All"),
    FILES_ONLY("Files only"),
    FOLDERS_ONLY("Folders only"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    DOCUMENTS("Documents")
}

sealed class FileExplorerUiState {

    object Loading : FileExplorerUiState()

    object PermissionRequired : FileExplorerUiState()

    data class BreadcrumbSegment(
        val name: String,
        val path: String
    )

    data class Browsing(
        val currentPath: String,
        val displayPath: String,
        val entries: List<FileNode>,
        val selection: Set<String>,   // selected absolute paths (documentId)
        val canGoUp: Boolean,
        val sortField: SortField = SortField.NAME,
        val sortDirection: SortDirection = SortDirection.ASC,
        val fileFilter: FileFilter = FileFilter.ALL,
        val showHiddenFiles: Boolean = false,
        val breadcrumbs: List<BreadcrumbSegment> = emptyList()
    ) : FileExplorerUiState() {
        val selectedCount: Int get() = selection.size
        val selectedFiles: List<FileNode>
            get() = entries.filter { it.documentId in selection }
        val folderName: String
            get() = breadcrumbs.lastOrNull()?.name ?: "Internal Storage"
    }

    data class RenamePreviewing(
        val previewItems: List<RenamePreviewItem>,
        val rules: List<RenameRule>,
        val selectedCount: Int,
        val globalConflictStrategy: ConflictStrategy = ConflictStrategy.AUTO_RENAME,
        val createCopy: Boolean = false
    ) : FileExplorerUiState() {
        val conflictCount: Int get() = previewItems.count { it.hasConflict }
        val autoRenamedCount: Int get() = previewItems.count { it.hasConflict && it.conflictStrategy == ConflictStrategy.AUTO_RENAME }
        val overwriteCount: Int get() = previewItems.count { it.hasConflict && it.conflictStrategy == ConflictStrategy.OVERWRITE }
        val skippedCount: Int get() = previewItems.count { it.isSkipped }
        val errorCount: Int get() = previewItems.count { it.validationError != null }
        val unchangedCount: Int get() = previewItems.count { it.isUnchanged }
        val canConfirm: Boolean get() = errorCount == 0
    }

    data class RenameInProgress(
        val total: Int,
        val completed: Int,
        val currentFileName: String,
        val batchId: String
    ) : FileExplorerUiState() {
        val progress: Float get() = if (total == 0) 0f else completed.toFloat() / total
    }

    data class RenameResult(
        val successCount: Int,
        val failureCount: Int,
        val errors: List<RenameError>,
        val batchId: String
    ) : FileExplorerUiState()

    data class Error(
        val message: String,
        val recoverable: Boolean
    ) : FileExplorerUiState()
}

data class RenameError(
    val originalName: String,
    val newName: String,
    val reason: String
)
