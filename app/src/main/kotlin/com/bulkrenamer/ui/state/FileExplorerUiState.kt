package com.bulkrenamer.ui.state

import android.net.Uri
import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.domain.RenamePreviewItem

sealed class FileExplorerUiState {

    object Loading : FileExplorerUiState()

    object PermissionRequired : FileExplorerUiState()

    data class Browsing(
        val currentUri: Uri,
        val displayPath: String,
        val entries: List<FileNode>,
        val selection: Set<String>, // selected document IDs
        val canGoUp: Boolean,
        val isNearUriQuota: Boolean = false
    ) : FileExplorerUiState() {
        val selectedCount: Int get() = selection.size
        val selectedFiles: List<FileNode>
            get() = entries.filter { it.documentId in selection }
    }

    data class RenamePreviewing(
        val previewItems: List<RenamePreviewItem>,
        val rule: RenameRule,
        val selectedCount: Int
    ) : FileExplorerUiState() {
        val conflictCount: Int get() = previewItems.count { it.hasConflict }
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
