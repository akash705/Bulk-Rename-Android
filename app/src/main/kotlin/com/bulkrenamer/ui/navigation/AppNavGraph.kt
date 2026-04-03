package com.bulkrenamer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bulkrenamer.ui.component.RenameProgressDialog
import com.bulkrenamer.ui.screen.FileBrowserScreen
import com.bulkrenamer.ui.screen.HistoryScreen
import com.bulkrenamer.ui.screen.RenamePreviewScreen
import com.bulkrenamer.ui.screen.RenameResultScreen
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.viewmodel.FileExplorerViewModel
import com.bulkrenamer.ui.viewmodel.HistoryViewModel

private const val ROUTE_BROWSER = "browser"
private const val ROUTE_HISTORY = "history"

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_BROWSER,
        modifier = modifier
    ) {
        composable(ROUTE_BROWSER) {
            val viewModel: FileExplorerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            FileExplorerRoot(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate(ROUTE_HISTORY) }
            )
        }

        composable(ROUTE_HISTORY) {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}

@Composable
private fun FileExplorerRoot(
    uiState: FileExplorerUiState,
    viewModel: FileExplorerViewModel,
    onNavigateToHistory: () -> Unit
) {
    when (uiState) {
        is FileExplorerUiState.RenamePreviewing -> {
            RenamePreviewScreen(
                state = uiState,
                onConfirm = { viewModel.confirmRename(uiState) },
                onBack = { viewModel.backToBrowsing() },
                onCopyModeChange = { viewModel.setCopyMode(it) },
                onGlobalStrategyChange = { viewModel.setGlobalConflictStrategy(it) },
                onItemStrategyChange = { docId, strategy -> viewModel.setItemConflictStrategy(docId, strategy) }
            )
        }

        is FileExplorerUiState.RenameResult -> {
            RenameResultScreen(
                state = uiState,
                onDone = { viewModel.backToBrowsing() },
                onUndo = { viewModel.undoLastBatch(uiState.batchId) }
            )
        }

        else -> {
            FileBrowserScreen(
                uiState = uiState,
                onNavigateUp = { viewModel.navigateUp() },
                onNavigateTo = { viewModel.navigateTo(it) },
                onToggleSelection = { viewModel.toggleSelection(it) },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() },
                onPreviewRename = { viewModel.previewRename(it) },
                onPermissionGranted = { viewModel.onPermissionGranted() },
                onNavigateToHistory = onNavigateToHistory,
                onSortFieldSelected = { viewModel.setSortField(it) },
                onFilterSelected = { viewModel.setFilter(it) },
                onNavigateToBreadcrumb = { viewModel.navigateToBreadcrumb(it) },
                onToggleHiddenFiles = { viewModel.toggleShowHiddenFiles() },
                onRetry = { viewModel.retry() }
            )

            if (uiState is FileExplorerUiState.RenameInProgress) {
                RenameProgressDialog(
                    state = uiState,
                    onCancel = { viewModel.cancelRename() }
                )
            }
        }
    }
}
