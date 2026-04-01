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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bulkrenamer.domain.RenamePreviewItem

@Composable
fun PreviewItemRow(
    item: RenamePreviewItem,
    modifier: Modifier = Modifier
) {
    val hasIssue = item.hasConflict || item.validationError != null
    val isUnchanged = item.isUnchanged

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Original name
        Text(
            text = item.fileNode.name,
            style = MaterialTheme.typography.bodyMedium.let {
                if (isUnchanged) it.copy(textDecoration = TextDecoration.None) else it
            },
            color = if (isUnchanged) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Proposed name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.proposedName,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    item.validationError != null -> MaterialTheme.colorScheme.error
                    item.hasConflict -> MaterialTheme.colorScheme.tertiary
                    isUnchanged -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.validationError != null) {
                Text(
                    text = item.validationError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (item.hasConflict) {
                Text(
                    text = "Conflict resolved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

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
}
