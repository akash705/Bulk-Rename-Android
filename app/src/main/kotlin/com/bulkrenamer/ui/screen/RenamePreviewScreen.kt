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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bulkrenamer.ui.component.PreviewItemRow
import com.bulkrenamer.ui.state.FileExplorerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenamePreviewScreen(
    state: FileExplorerUiState.RenamePreviewing,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
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

            // Preview list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.previewItems, key = { it.fileNode.documentId }) { item ->
                    PreviewItemRow(item = item)
                }
            }

            // Confirm / Back row
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
                    Text("Rename ${state.selectedCount} files")
                }
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
        val renamedCount = state.selectedCount - state.unchangedCount
        val parts = buildList {
            if (renamedCount > 0) add("$renamedCount will be renamed")
            if (state.unchangedCount > 0) add("${state.unchangedCount} unchanged")
            if (state.conflictCount > 0) add("${state.conflictCount} conflicts resolved")
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
