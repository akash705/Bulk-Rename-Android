package com.bulkrenamer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.ui.component.FileItem
import com.bulkrenamer.ui.component.SelectionToolbar
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.state.FileFilter
import com.bulkrenamer.ui.state.SortDirection
import com.bulkrenamer.ui.state.SortField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    uiState: FileExplorerUiState,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onPreviewRename: (List<RenameRule>) -> Unit,
    onPermissionGranted: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSortFieldSelected: (SortField) -> Unit,
    onFilterSelected: (FileFilter) -> Unit,
    onNavigateToBreadcrumb: (String) -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChainBuilder by remember { mutableStateOf(false) }
    var pendingChain by remember { mutableStateOf(listOf<RenameRule>()) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            when (uiState) {
                is FileExplorerUiState.Browsing -> TopAppBar(
                    title = {
                        Text(
                            text = uiState.folderName,
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
                        // Sort button
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortField.entries.forEach { field ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(field.label)
                                                if (uiState.sortField == field) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = if (uiState.sortDirection == SortDirection.ASC) "↑" else "↓",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSortFieldSelected(field)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (uiState.sortField == field) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }
                        // Filter button
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (uiState.fileFilter != FileFilter.ALL) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                FileFilter.entries.forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label) },
                                        onClick = {
                                            onFilterSelected(filter)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = if (uiState.fileFilter == filter) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("Show hidden files") },
                                    onClick = { onToggleHiddenFiles() },
                                    leadingIcon = if (uiState.showHiddenFiles) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
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
                    onPermissionGranted = onPermissionGranted,
                    modifier = Modifier.fillMaxSize()
                )

                is FileExplorerUiState.Browsing -> BrowsingContent(
                    state = uiState,
                    onNavigateTo = onNavigateTo,
                    onToggleSelection = onToggleSelection,
                    onSelectAll = onSelectAll,
                    onDeselectAll = onDeselectAll,
                    onNavigateToBreadcrumb = onNavigateToBreadcrumb,
                    onRename = {
                        pendingChain = emptyList()
                        showChainBuilder = true
                        showAddRuleDialog = true
                    }
                )

                is FileExplorerUiState.Error -> ErrorContent(
                    message = uiState.message,
                    onRetry = if (uiState.recoverable) onRetry else null
                )

                else -> Unit
            }
        }

        if (showAddRuleDialog) {
            RenameRuleDialog(
                onConfirm = { rule ->
                    showAddRuleDialog = false
                    pendingChain = pendingChain + rule
                },
                onDismiss = {
                    showAddRuleDialog = false
                    if (pendingChain.isEmpty()) showChainBuilder = false
                }
            )
        } else if (showChainBuilder) {
            RuleChainBuilderDialog(
                rules = pendingChain,
                onAddRule = { showAddRuleDialog = true },
                onRemoveRule = { index -> pendingChain = pendingChain.toMutableList().also { it.removeAt(index) } },
                onConfirm = {
                    val chain = pendingChain.toList()
                    showChainBuilder = false
                    onPreviewRename(chain)
                },
                onDismiss = { showChainBuilder = false }
            )
        }
    }
}

@Composable
private fun BrowsingContent(
    state: FileExplorerUiState.Browsing,
    onNavigateTo: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onNavigateToBreadcrumb: (String) -> Unit,
    onRename: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Breadcrumb row
        if (state.breadcrumbs.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.breadcrumbs.forEachIndexed { index, segment ->
                    val isLast = index == state.breadcrumbs.lastIndex
                    Text(
                        text = segment.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLast) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary,
                        modifier = if (isLast) Modifier
                        else Modifier.clickable { onNavigateToBreadcrumb(segment.path) }
                    )
                    if (!isLast) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        if (state.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This folder is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
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
                        onNavigate = { onNavigateTo(file.absolutePath) }
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
