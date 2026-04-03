package com.bulkrenamer.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bulkrenamer.domain.ConflictStrategy
import com.bulkrenamer.ui.component.PreviewItemRow
import com.bulkrenamer.ui.state.FileExplorerUiState

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Preview Rename") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary header
            PreviewSummaryHeader(state = state, modifier = Modifier.padding(16.dp))

            // Copy mode toggle — always visible
            CopyModeRow(
                createCopy = state.createCopy,
                onToggle = onCopyModeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Global conflict strategy picker — only shown when there are conflicts
            if (state.conflictCount > 0) {
                GlobalConflictStrategyPicker(
                    selected = state.globalConflictStrategy,
                    onSelect = onGlobalStrategyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Preview list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.previewItems, key = { it.fileNode.documentId }) { item ->
                    PreviewItemRow(
                        item = item,
                        onStrategyChange = if (item.hasConflict) {
                            { strategy -> onItemStrategyChange(item.fileNode.documentId, strategy) }
                        } else null
                    )
                }
            }

            // Confirm row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onConfirm,
                    enabled = state.canConfirm
                ) {
                    val label = if (state.createCopy) "Copy ${state.selectedCount} files"
                                else "Rename ${state.selectedCount} files"
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun CopyModeRow(
    createCopy: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (createCopy) "Create copies (keep originals)" else "Rename files",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (createCopy) "Original files will not be changed"
                       else "Files will be renamed in place",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = createCopy,
            onCheckedChange = onToggle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalConflictStrategyPicker(
    selected: ConflictStrategy,
    onSelect: (ConflictStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Conflicts",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ConflictStrategy.entries.forEachIndexed { index, strategy ->
                SegmentedButton(
                    selected = strategy == selected,
                    onClick = { onSelect(strategy) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ConflictStrategy.entries.size
                    ),
                    label = { Text(strategy.label) }
                )
            }
        }
    }
}

@Composable
private fun PreviewSummaryHeader(
    state: FileExplorerUiState.RenamePreviewing,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val actionCount = state.selectedCount - state.unchangedCount
        val actionWord = if (state.createCopy) "copied" else "renamed"
        val parts = buildList {
            if (actionCount > 0) add("$actionCount will be $actionWord")
            if (state.unchangedCount > 0) add("${state.unchangedCount} unchanged")
            if (state.autoRenamedCount > 0) add("${state.autoRenamedCount} auto-renamed")
            if (state.overwriteCount > 0) add("${state.overwriteCount} will overwrite")
            if (state.skippedCount > 0) add("${state.skippedCount} skipped")
            if (state.errorCount > 0) add("${state.errorCount} errors")
        }
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.errorCount > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!state.canConfirm) {
            Text(
                text = "Fix errors before continuing",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
