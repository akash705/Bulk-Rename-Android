package com.bulkrenamer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulkrenamer.domain.ConflictStrategy
import com.bulkrenamer.ui.component.GradientButton
import com.bulkrenamer.ui.component.PreviewItemRow
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.theme.Amber
import com.bulkrenamer.ui.theme.Coral
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet

@Composable
fun RenamePreviewScreen(
    state: FileExplorerUiState.RenamePreviewing,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onCopyModeChange: (Boolean) -> Unit,
    onGlobalStrategyChange: (ConflictStrategy) -> Unit,
    onItemStrategyChange: (documentId: String, ConflictStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = InkDeep,
        modifier = modifier,
        topBar = {
            PreviewTopBar(
                fileCount = state.selectedCount,
                conflictCount = state.conflictCount,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SummaryChipRow(state)
            CopyModeCard(
                createCopy = state.createCopy,
                onToggle = onCopyModeChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (state.conflictCount > 0) {
                Text(
                    text = "CONFLICTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Violet.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 6.dp)
                )
                SegmentedPills(
                    options = ConflictStrategy.entries.toList(),
                    selected = state.globalConflictStrategy,
                    label = { it.label },
                    onSelect = onGlobalStrategyChange,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.previewItems, key = { it.fileNode.documentId }) { item ->
                    PreviewItemRow(
                        item = item,
                        onStrategyChange = if (item.hasConflict) {
                            { strategy -> onItemStrategyChange(item.fileNode.documentId, strategy) }
                        } else null
                    )
                }
            }
            BottomConfirm(state, onConfirm)
        }
    }
}

@Composable
private fun PreviewTopBar(fileCount: Int, conflictCount: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkDeep)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(InkElevated)
                    .border(1.dp, InkBorder, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMid,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val sub = buildString {
                    append(fileCount)
                    append(" files")
                    if (conflictCount > 0) append(" · $conflictCount conflicts")
                }
                Text(text = sub, style = MaterialTheme.typography.labelSmall, color = TextLow)
            }
        }
    }
}

@Composable
private fun SummaryChipRow(state: FileExplorerUiState.RenamePreviewing) {
    val chips = buildList {
        val willAct = state.selectedCount - state.unchangedCount - state.skippedCount - state.errorCount
        if (willAct > 0) add(Violet to "$willAct ${if (state.createCopy) "copied" else "renamed"}")
        if (state.autoRenamedCount > 0) add(Amber to "${state.autoRenamedCount} auto-renamed")
        if (state.overwriteCount > 0) add(Coral to "${state.overwriteCount} will overwrite")
        if (state.skippedCount > 0) add(TextMid to "${state.skippedCount} skipped")
        if (state.unchangedCount > 0) add(TextLow to "${state.unchangedCount} unchanged")
        if (state.errorCount > 0) add(Coral to "${state.errorCount} errors")
    }
    if (chips.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (color, label) ->
            SummaryChip(color = color, label = label)
        }
    }
}

@Composable
private fun SummaryChip(color: Color, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}

@Composable
private fun CopyModeCard(
    createCopy: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InkElevated)
            .border(1.dp, InkBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (createCopy) "Create copies" else "Rename in place",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (createCopy) "Originals stay untouched"
                else "Files get their new names directly",
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )
        }
        Switch(
            checked = createCopy,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = InkDeep,
                checkedTrackColor = Violet,
                checkedBorderColor = Violet,
                uncheckedThumbColor = TextMid,
                uncheckedTrackColor = InkElevated,
                uncheckedBorderColor = InkBorder
            )
        )
    }
}

@Composable
private fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(InkElevated)
            .border(1.dp, InkBorder, CircleShape)
            .padding(4.dp)
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (isSel) Violet else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (isSel) InkDeep else TextMid
                )
            }
        }
    }
}

@Composable
private fun BottomConfirm(state: FileExplorerUiState.RenamePreviewing, onConfirm: () -> Unit) {
    val actionCount = (state.selectedCount - state.unchangedCount).coerceAtLeast(0)
    val verb = if (state.createCopy) "Copy" else "Rename"
    val enabled = state.canConfirm && actionCount > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!state.canConfirm) {
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Coral)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Fix errors before continuing",
                    style = MaterialTheme.typography.labelMedium,
                    color = Coral
                )
            }
        }
        GradientButton(
            text = "$verb $actionCount ${if (actionCount == 1) "file" else "files"}",
            icon = Icons.Filled.AutoAwesome,
            onClick = onConfirm,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
