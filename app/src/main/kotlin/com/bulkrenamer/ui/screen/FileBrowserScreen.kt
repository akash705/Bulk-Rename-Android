package com.bulkrenamer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.ui.component.FileItem
import com.bulkrenamer.ui.component.GradientButton
import com.bulkrenamer.ui.component.SelectionToolbar
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.state.FileFilter
import com.bulkrenamer.ui.state.SortDirection
import com.bulkrenamer.ui.state.SortField
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.InkHigh
import com.bulkrenamer.ui.theme.Pink
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

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

    Scaffold(
        containerColor = InkDeep,
        modifier = modifier,
        topBar = {
            when (uiState) {
                is FileExplorerUiState.Browsing -> PlayfulTopBar(
                    folderName = uiState.folderName,
                    canGoUp = uiState.canGoUp,
                    filterActive = uiState.fileFilter != FileFilter.ALL,
                    sortField = uiState.sortField,
                    sortDirection = uiState.sortDirection,
                    fileFilter = uiState.fileFilter,
                    showHiddenFiles = uiState.showHiddenFiles,
                    onNavigateUp = onNavigateUp,
                    onNavigateToHistory = onNavigateToHistory,
                    onSortFieldSelected = onSortFieldSelected,
                    onFilterSelected = onFilterSelected,
                    onToggleHiddenFiles = onToggleHiddenFiles
                )
                else -> PlaceholderTopBar(title = "Bulk Renamer")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(InkDeep)
        ) {
            when (uiState) {
                FileExplorerUiState.Loading -> LoadingState(Modifier.align(Alignment.Center))

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
private fun PlayfulTopBar(
    folderName: String,
    canGoUp: Boolean,
    filterActive: Boolean,
    sortField: SortField,
    sortDirection: SortDirection,
    fileFilter: FileFilter,
    showHiddenFiles: Boolean,
    onNavigateUp: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSortFieldSelected: (SortField) -> Unit,
    onFilterSelected: (FileFilter) -> Unit,
    onToggleHiddenFiles: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkDeep)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canGoUp) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDesc = "Back",
                    onClick = onNavigateUp
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = folderName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Box {
                CircleIconButton(
                    icon = Icons.Filled.Sort,
                    contentDesc = "Sort",
                    onClick = { showSortMenu = true }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(field.label)
                                    if (sortField == field) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (sortDirection == SortDirection.ASC) "↑" else "↓",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Violet
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSortFieldSelected(field)
                                showSortMenu = false
                            },
                            leadingIcon = if (sortField == field) {
                                { Icon(Icons.Default.Check, null, tint = Violet) }
                            } else null
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Box {
                CircleIconButton(
                    icon = Icons.Filled.FilterList,
                    contentDesc = "Filter",
                    onClick = { showFilterMenu = true },
                    badgeDot = filterActive
                )
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
                            leadingIcon = if (fileFilter == filter) {
                                { Icon(Icons.Default.Check, null, tint = Violet) }
                            } else null
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Show hidden files") },
                        onClick = { onToggleHiddenFiles() },
                        leadingIcon = if (showHiddenFiles) {
                            { Icon(Icons.Default.Check, null, tint = Violet) }
                        } else null
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            CircleIconButton(
                icon = Icons.Filled.History,
                contentDesc = "History",
                onClick = onNavigateToHistory
            )
        }
    }
}

@Composable
private fun PlaceholderTopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkDeep)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    badgeDot: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(InkElevated)
            .border(1.dp, InkBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = TextMid,
            modifier = Modifier.size(18.dp)
        )
        if (badgeDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Pink)
                    .align(Alignment.TopEnd)
                    .padding(0.dp)
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
        if (state.breadcrumbs.size > 1) {
            BreadcrumbRow(
                crumbs = state.breadcrumbs,
                onNavigateToBreadcrumb = onNavigateToBreadcrumb
            )
        }

        if (state.entries.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
private fun BreadcrumbRow(
    crumbs: List<FileExplorerUiState.BreadcrumbSegment>,
    onNavigateToBreadcrumb: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            if (isLast) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Violet.copy(alpha = 0.18f))
                        .border(1.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = crumb.name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Violet
                    )
                }
            } else {
                Text(
                    text = crumb.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMid,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToBreadcrumb(crumb.path) }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextLow,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(InkElevated)
                .border(1.dp, InkBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "This folder is empty.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(48.dp)) {
        androidx.compose.material3.CircularProgressIndicator(color = Violet)
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
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(com.bulkrenamer.ui.theme.Coral.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠️", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(text = "Try again", onClick = onRetry)
        }
    }
}
