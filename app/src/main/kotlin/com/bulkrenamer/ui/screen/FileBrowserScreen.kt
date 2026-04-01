package com.bulkrenamer.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.ui.component.FileItem
import com.bulkrenamer.ui.component.SelectionToolbar
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.viewmodel.FileExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    uiState: FileExplorerUiState,
    onNavigateUp: () -> Unit,
    onNavigateTo: (Uri) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onPreviewRename: (RenameRule) -> Unit,
    onFolderGranted: (Uri) -> Unit,
    onNavigateToHistory: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRuleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            when (uiState) {
                is FileExplorerUiState.Browsing -> TopAppBar(
                    title = {
                        Text(
                            text = uiState.displayPath.ifEmpty { "Files" },
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        if (uiState.canGoUp) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                )
                else -> TopAppBar(title = { Text("Bulk Renamer") })
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                FileExplorerUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                FileExplorerUiState.PermissionRequired -> PermissionScreen(
                    onFolderGranted = onFolderGranted,
                    modifier = Modifier.fillMaxSize()
                )

                is FileExplorerUiState.Browsing -> BrowsingContent(
                    state = uiState,
                    onNavigateTo = onNavigateTo,
                    onToggleSelection = onToggleSelection,
                    onSelectAll = onSelectAll,
                    onDeselectAll = onDeselectAll,
                    onRename = { showRuleDialog = true }
                )

                is FileExplorerUiState.Error -> ErrorContent(
                    message = uiState.message,
                    onRetry = if (uiState.recoverable) onRetry else null
                )

                else -> Unit // Other states handled by parent NavGraph
            }

            // URI quota warning
            if (uiState is FileExplorerUiState.Browsing && uiState.isNearUriQuota) {
                UriQuotaWarning(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        if (showRuleDialog) {
            RenameRuleDialog(
                onConfirm = { rule ->
                    showRuleDialog = false
                    onPreviewRename(rule)
                },
                onDismiss = { showRuleDialog = false }
            )
        }
    }
}

@Composable
private fun BrowsingContent(
    state: FileExplorerUiState.Browsing,
    onNavigateTo: (Uri) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRename: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "This folder is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.entries, key = { it.documentId }) { file ->
                    FileItem(
                        file = file,
                        isSelected = file.documentId in state.selection,
                        isSelectionMode = state.selectedCount > 0,
                        onToggleSelection = { onToggleSelection(file.documentId) },
                        onNavigate = { onNavigateTo(file.uri) }
                    )
                }
            }
        }

        if (state.selectedCount > 0) {
            SelectionToolbar(
                selectedCount = state.selectedCount,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                onRename = onRename
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun UriQuotaWarning(modifier: Modifier = Modifier) {
    MaterialTheme.colorScheme.let { colors ->
        androidx.compose.material3.Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = colors.errorContainer
            )
        ) {
            Text(
                text = "You're approaching the folder grant limit (400+). " +
                        "Consider removing unused folder access from Settings.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onErrorContainer
            )
        }
    }
}
