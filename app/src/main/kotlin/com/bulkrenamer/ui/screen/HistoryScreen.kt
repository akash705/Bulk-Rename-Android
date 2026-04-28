package com.bulkrenamer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bulkrenamer.domain.UndoResult
import com.bulkrenamer.ui.component.HistoryItem
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet
import com.bulkrenamer.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastUndoResult) {
        val result = uiState.lastUndoResult ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(buildUndoMessage(result))
        viewModel.dismissUndoResult()
    }

    Scaffold(
        containerColor = InkDeep,
        modifier = modifier,
        topBar = {
            HistoryTopBar(
                totalCount = uiState.batches.size,
                onBack = onNavigateUp
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchPill(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::clearSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isLoading -> LoadingState()
                uiState.batches.isEmpty() -> EmptyState(searchQuery = uiState.searchQuery)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.batches, key = { it.batchId }) { batch ->
                            HistoryItem(
                                batch = batch,
                                onUndo = { viewModel.undoBatch(batch.batchId) },
                                undoEnabled = !uiState.undoInProgress && !batch.undone
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(totalCount: Int, onBack: () -> Unit) {
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
                    text = "History",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val noun = if (totalCount == 1) "batch" else "batches"
                Text(
                    text = "$totalCount $noun",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(InkElevated)
                    .border(1.dp, InkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(InkElevated)
            .border(1.dp, InkBorder, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = TextLow,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search filenames…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLow
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Violet),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Violet.copy(alpha = 0.18f))
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear search",
                    tint = Violet,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Violet)
    }
}

@Composable
private fun EmptyState(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✨",
                fontSize = 40.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "No matches" else "No history yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotEmpty())
                    "Nothing matches \"$searchQuery\""
                else
                    "Your rename batches will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
        }
    }
}

private fun buildUndoMessage(result: UndoResult): String {
    val parts = mutableListOf<String>()
    if (result.reversedCount > 0) {
        val noun = if (result.reversedCount == 1) "file" else "files"
        parts.add("${result.reversedCount} $noun reversed")
    }
    if (result.failedCount > 0) {
        parts.add("${result.failedCount} failed")
    }
    val summary = parts.joinToString(" · ").ifEmpty { "Nothing reversed" }
    val warning = result.warningMessage?.let { "\n$it" } ?: ""
    return "$summary.$warning"
}
