package com.bulkrenamer.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.ui.theme.Amber
import com.bulkrenamer.ui.theme.Cyan
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.InkHigh
import com.bulkrenamer.ui.theme.JetBrainsMono
import com.bulkrenamer.ui.theme.Mint
import com.bulkrenamer.ui.theme.Pink
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TypeStyle(val icon: ImageVector, val tint: Color)

private fun typeStyle(file: FileNode): TypeStyle = when {
    file.isDirectory -> TypeStyle(Icons.Filled.Folder, Violet)
    file.mimeType.startsWith("image/") -> TypeStyle(Icons.Filled.Image, Pink)
    file.mimeType.startsWith("video/") -> TypeStyle(Icons.Filled.VideoFile, Cyan)
    file.mimeType.startsWith("audio/") -> TypeStyle(Icons.Filled.AudioFile, Amber)
    file.mimeType.startsWith("text/") || file.mimeType.contains("pdf") || file.mimeType.contains("document") ->
        TypeStyle(Icons.Filled.Description, Mint)
    else -> TypeStyle(Icons.Filled.InsertDriveFile, TextLow)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    file: FileNode,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = typeStyle(file)
    val borderColor by animateColorAsState(
        if (isSelected) Violet else InkBorder,
        label = "border"
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 2.dp else 1.dp,
        label = "borderWidth"
    )
    val bgColor by animateColorAsState(
        if (isSelected) InkHigh else InkElevated,
        label = "bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode && !file.isDirectory) onToggleSelection()
                    else if (file.isDirectory) onNavigate()
                    else onToggleSelection()
                },
                onLongClick = {
                    if (!file.isDirectory) onToggleSelection()
                }
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildSubtitle(file),
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        TrailingIndicator(file = file, isSelected = isSelected)
    }
}

@Composable
private fun TrailingIndicator(file: FileNode, isSelected: Boolean) {
    if (file.isDirectory) {
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextLow
        )
    } else {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isSelected) Violet else Color.Transparent)
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = if (isSelected) Color.Transparent else InkBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = com.bulkrenamer.ui.theme.InkDeep,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun buildSubtitle(file: FileNode): String {
    val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    val date = fmt.format(Date(file.lastModified)).uppercase(Locale.getDefault())
    return if (file.isDirectory) date
    else "${formatFileSize(file.size)} · $date"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024L * 1024 * 1024)} GB"
}
