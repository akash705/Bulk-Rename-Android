package com.bulkrenamer.domain

import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.model.RenameRule

/**
 * Computes rename preview as a pure function — no I/O.
 *
 * Conflict detection checks against:
 * 1. Existing names of files NOT being renamed (non-selected files in the same folder)
 * 2. Names already produced by earlier items in this batch (within-batch duplicates)
 *
 * Conflicts are resolved by appending _1, _2, etc. to the base name (before extension).
 */
fun computePreview(
    selectedFiles: List<FileNode>,
    rule: RenameRule,
    existingNamesInFolder: Set<String> = emptySet()
): List<RenamePreviewItem> {
    // Names of non-selected files that we must not collide with
    val occupiedNames = existingNamesInFolder.toMutableSet()
    // Names already produced in this batch
    val batchNames = mutableSetOf<String>()

    return selectedFiles.map { node ->
        val rawProposed = rule.apply(node)
        val unchanged = rawProposed == node.name

        val validationError = when {
            rawProposed.isEmpty() -> "Name cannot be empty"
            rawProposed.contains('/') || rawProposed.contains('\u0000') -> "Name contains invalid characters"
            rawProposed.length > 255 -> "Name too long (max 255 characters)"
            else -> null
        }

        val proposedName = if (validationError != null || unchanged) {
            rawProposed
        } else {
            resolveConflict(rawProposed, node, occupiedNames, batchNames)
        }

        val hasConflict = proposedName != rawProposed

        // After naming this file, add its final name to the occupied set so later
        // files in the batch don't collide with it.
        batchNames.add(proposedName)

        RenamePreviewItem(
            fileNode = node,
            proposedName = proposedName,
            hasConflict = hasConflict,
            isUnchanged = unchanged,
            validationError = validationError
        )
    }
}

private fun resolveConflict(
    proposed: String,
    node: FileNode,
    occupiedNames: Set<String>,
    batchNames: Set<String>
): String {
    if (proposed !in occupiedNames && proposed !in batchNames) return proposed

    val ext = if (!node.isDirectory) {
        val lastDot = proposed.lastIndexOf('.')
        if (lastDot > 0) proposed.substring(lastDot) else ""
    } else ""

    val base = if (ext.isNotEmpty()) proposed.dropLast(ext.length) else proposed

    var counter = 1
    while (true) {
        val candidate = "${base}_$counter$ext"
        if (candidate !in occupiedNames && candidate !in batchNames) return candidate
        counter++
    }
}
