package com.bulkrenamer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.domain.ConflictStrategy
import com.bulkrenamer.domain.RenamePreviewItem
import com.bulkrenamer.ui.theme.Amber
import com.bulkrenamer.ui.theme.Coral
import com.bulkrenamer.ui.theme.Cyan
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.JetBrainsMono
import com.bulkrenamer.ui.theme.Mint
import com.bulkrenamer.ui.theme.Pink
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet

private data class PreviewTypeStyle(val icon: ImageVector, val tint: Color)

private fun typeStyle(file: FileNode): PreviewTypeStyle = when {
    file.isDirectory -> PreviewTypeStyle(Icons.Filled.Folder, Violet)
    file.mimeType.startsWith("image/") -> PreviewTypeStyle(Icons.Filled.Image, Pink)
    file.mimeType.startsWith("video/") -> PreviewTypeStyle(Icons.Filled.VideoFile, Cyan)
    file.mimeType.startsWith("audio/") -> PreviewTypeStyle(Icons.Filled.AudioFile, Amber)
    file.mimeType.startsWith("text/") || file.mimeType.contains("pdf") || file.mimeType.contains("document") ->
        PreviewTypeStyle(Icons.Filled.Description, Mint)
    else -> PreviewTypeStyle(Icons.Filled.InsertDriveFile, TextLow)
}

@Composable
fun PreviewItemRow(
    item: RenamePreviewItem,
    onStrategyChange: ((ConflictStrategy) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val style = typeStyle(item.fileNode)
    val status = statusFor(item)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(InkElevated)
            .border(1.dp, InkBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(style.tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileNode.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMono,
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = TextLow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.proposedName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium
                    ),
                    color = newNameColor(item),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            StatusChip(status)
        }

        if (item.validationError != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.validationError,
                style = MaterialTheme.typography.labelSmall,
                color = Coral,
                modifier = Modifier.padding(start = 52.dp)
            )
        } else if (item.hasConflict && onStrategyChange != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(start = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConflictStrategy.entries.forEach { strategy ->
                    MiniChip(
                        text = strategy.label,
                        selected = item.conflictStrategy == strategy,
                        onClick = { onStrategyChange(strategy) }
                    )
                }
            }
        }
    }
}

private enum class PreviewStatus(val label: String, val color: Color) {
    OK("OK", Mint),
    AUTO("AUTO", Amber),
    OVERWRITE("OVERWRITE", Coral),
    SKIP("SKIP", TextMid),
    ERROR("ERROR", Coral),
    UNCHANGED("UNCHANGED", TextLow)
}

private fun statusFor(item: RenamePreviewItem): PreviewStatus = when {
    item.validationError != null -> PreviewStatus.ERROR
    item.isSkipped -> PreviewStatus.SKIP
    item.hasConflict && item.conflictStrategy == ConflictStrategy.OVERWRITE -> PreviewStatus.OVERWRITE
    item.hasConflict -> PreviewStatus.AUTO
    item.isUnchanged -> PreviewStatus.UNCHANGED
    else -> PreviewStatus.OK
}

private fun newNameColor(item: RenamePreviewItem): Color = when {
    item.validationError != null -> Coral
    item.isSkipped -> TextLow
    item.hasConflict && item.conflictStrategy == ConflictStrategy.OVERWRITE -> Coral
    item.hasConflict -> Amber
    item.isUnchanged -> TextLow
    else -> Color(0xFFF5F5FA)
}

@Composable
private fun StatusChip(status: PreviewStatus) {
    Row(
        modifier = Modifier
            .height(22.dp)
            .clip(CircleShape)
            .background(status.color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(status.color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = status.color
        )
    }
}

@Composable
private fun MiniChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(if (selected) Violet.copy(alpha = 0.22f) else InkElevated)
            .border(
                1.dp,
                if (selected) Violet else InkBorder,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) Violet else TextMid
        )
    }
}

