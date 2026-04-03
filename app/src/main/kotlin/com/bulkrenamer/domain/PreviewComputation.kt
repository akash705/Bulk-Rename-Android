package com.bulkrenamer.domain

import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.model.RenameRule

/**
 * Computes rename preview as a pure function — no I/O.
 *
 * Rules are applied in order as a chain: the output of rule N becomes the input to rule N+1.
 * The file's position index (0-based) is passed to each rule so that [RenameRule.AddNumbering]
 * can produce sequential numbers across the batch.
 *
 * Conflict detection checks against:
 * 1. Existing names of files NOT being renamed (non-selected files in the same folder)
 * 2. Names already produced by earlier items in this batch (within-batch duplicates)
 *
 * How conflicts are resolved depends on [defaultStrategy] and [perItemStrategies]:
 * - AUTO_RENAME: append _1, _2, … to the base name (before extension)
 * - OVERWRITE:   keep the raw proposed name; the existing file will be deleted at rename time
 * - SKIP:        mark the item as skipped; it will not be renamed
 *
 * [perItemStrategies] is keyed by [FileNode.documentId] and overrides [defaultStrategy] for that item.
 */
fun computePreview(
    selectedFiles: List<FileNode>,
    rules: List<RenameRule>,
    existingNamesInFolder: Set<String> = emptySet(),
    defaultStrategy: ConflictStrategy = ConflictStrategy.AUTO_RENAME,
    perItemStrategies: Map<String, ConflictStrategy> = emptyMap()
): List<RenamePreviewItem> {
    // Names that must not be collided with by any item in this batch
    val occupiedNames = existingNamesInFolder.toMutableSet()
    // Names already claimed by earlier items in this batch
    val batchNames = mutableSetOf<String>()

    return selectedFiles.mapIndexed { fileIndex, node ->
        val rawProposed = rules.fold(node.name) { acc, rule ->
            rule.apply(acc, node.isDirectory, fileIndex)
        }
        val unchanged = rawProposed == node.name

        val validationError = when {
            rawProposed.isEmpty() -> "Name cannot be empty"
            rawProposed.contains('/') || rawProposed.contains('\u0000') -> "Name contains invalid characters"
            rawProposed.length > 255 -> "Name too long (max 255 characters)"
            else -> null
        }

        val hasNamingConflict = !unchanged && validationError == null &&
            (rawProposed in occupiedNames || rawProposed in batchNames)

        val effectiveStrategy = perItemStrategies[node.documentId] ?: defaultStrategy

        val (proposedName, hasConflict, resolvedStrategy) = when {
            validationError != null || unchanged || !hasNamingConflict ->
                Triple(rawProposed, false, ConflictStrategy.AUTO_RENAME)
            else -> when (effectiveStrategy) {
                ConflictStrategy.AUTO_RENAME -> Triple(
                    resolveConflict(rawProposed, node, occupiedNames, batchNames),
                    true,
                    ConflictStrategy.AUTO_RENAME
                )
                ConflictStrategy.OVERWRITE -> Triple(rawProposed, true, ConflictStrategy.OVERWRITE)
                ConflictStrategy.SKIP -> Triple(rawProposed, true, ConflictStrategy.SKIP)
            }
        }

        // Track names so subsequent items can detect conflicts correctly.
        when {
            resolvedStrategy == ConflictStrategy.SKIP ->
                // File stays at its original name — block others from claiming it
                occupiedNames.add(node.name)
            !unchanged && validationError == null ->
                batchNames.add(proposedName)
        }

        RenamePreviewItem(
            fileNode = node,
            proposedName = proposedName,
            rawProposedName = rawProposed,
            hasConflict = hasConflict,
            conflictStrategy = if (hasConflict) resolvedStrategy else ConflictStrategy.AUTO_RENAME,
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
