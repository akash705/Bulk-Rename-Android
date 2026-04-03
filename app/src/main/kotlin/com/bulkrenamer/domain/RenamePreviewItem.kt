package com.bulkrenamer.domain

import com.bulkrenamer.data.model.FileNode

data class RenamePreviewItem(
    val fileNode: FileNode,
    /** Effective name after the conflict strategy has been applied. */
    val proposedName: String,
    /** Name produced by the rules before any conflict resolution. */
    val rawProposedName: String = proposedName,
    val hasConflict: Boolean = false,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.AUTO_RENAME,
    val isUnchanged: Boolean = false,
    val validationError: String? = null
) {
    /** True when the item has a conflict and the user chose to skip it. */
    val isSkipped: Boolean get() = hasConflict && conflictStrategy == ConflictStrategy.SKIP
}
