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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.state.RenameError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameResultScreen(
    state: FileExplorerUiState.RenameResult,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rename Complete") }) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Summary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (state.failureCount == 0) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (state.failureCount == 0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = buildString {
                        append("${state.successCount} renamed")
                        if (state.failureCount > 0) append(", ${state.failureCount} failed")
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Failed files:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.errors) { error ->
                        ErrorRow(error = error)
                        HorizontalDivider()
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onUndo) { Text("Undo") }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onDone) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ErrorRow(error: RenameError) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = error.originalName, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = error.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
