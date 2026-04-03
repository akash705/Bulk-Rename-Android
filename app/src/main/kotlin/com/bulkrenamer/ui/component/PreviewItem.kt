package com.bulkrenamer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bulkrenamer.domain.ConflictStrategy
import com.bulkrenamer.domain.RenamePreviewItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewItemRow(
    item: RenamePreviewItem,
    onStrategyChange: ((ConflictStrategy) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasIssue = item.hasConflict || item.validationError != null
    val isUnchanged = item.isUnchanged

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Original name
        Text(
            text = item.fileNode.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUnchanged) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Arrow + proposed name
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = item.proposedName,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    item.validationError != null -> MaterialTheme.colorScheme.error
                    item.isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
                    item.hasConflict && item.conflictStrategy == ConflictStrategy.OVERWRITE ->
                        MaterialTheme.colorScheme.error
                    item.hasConflict -> MaterialTheme.colorScheme.tertiary
                    isUnchanged -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (hasIssue) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = if (item.validationError != null) "Error" else "Conflict",
                    tint = if (item.validationError != null) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.tertiary
                )
            }
        }

        when {
            item.validationError != null -> {
                Text(
                    text = item.validationError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp, start = 32.dp)
                )
            }
            item.hasConflict && onStrategyChange != null -> {
                // Per-item conflict strategy chips
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConflictStrategy.entries.forEach { strategy ->
                        FilterChip(
                            selected = item.conflictStrategy == strategy,
                            onClick = { onStrategyChange(strategy) },
                            label = { Text(strategy.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (strategy) {
                                    ConflictStrategy.OVERWRITE -> MaterialTheme.colorScheme.errorContainer
                                    ConflictStrategy.SKIP -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                selectedLabelColor = when (strategy) {
                                    ConflictStrategy.OVERWRITE -> MaterialTheme.colorScheme.onErrorContainer
                                    ConflictStrategy.SKIP -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
