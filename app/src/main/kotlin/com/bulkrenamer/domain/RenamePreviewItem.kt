package com.bulkrenamer.domain

import com.bulkrenamer.data.model.FileNode

data class RenamePreviewItem(
    val fileNode: FileNode,
    val proposedName: String,
    val hasConflict: Boolean = false,
    val isUnchanged: Boolean = false,
    val validationError: String? = null
)
