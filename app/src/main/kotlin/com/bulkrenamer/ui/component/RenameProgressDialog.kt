package com.bulkrenamer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bulkrenamer.ui.state.FileExplorerUiState

@Composable
fun RenameProgressDialog(
    state: FileExplorerUiState.RenameInProgress,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissable */ },
        title = { Text("Renaming files...") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${state.completed} of ${state.total}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.currentFileName.isNotEmpty()) {
                    Text(
                        text = state.currentFileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
