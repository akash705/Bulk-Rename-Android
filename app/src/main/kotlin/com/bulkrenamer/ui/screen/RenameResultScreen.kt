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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulkrenamer.ui.component.GradientButton
import com.bulkrenamer.ui.state.FileExplorerUiState
import com.bulkrenamer.ui.state.RenameError
import com.bulkrenamer.ui.theme.BrandGradient
import com.bulkrenamer.ui.theme.Coral
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.JetBrainsMono
import com.bulkrenamer.ui.theme.Mint
import com.bulkrenamer.ui.theme.Pink
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet

@Composable
fun RenameResultScreen(
    state: FileExplorerUiState.RenameResult,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSucceeded = state.failureCount == 0
    Scaffold(
        containerColor = InkDeep,
        modifier = modifier,
        topBar = { ResultTopBar(onBack = onDone) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            HeroCheck(allSucceeded = allSucceeded)
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (allSucceeded) "Nice, all done!" else "Finished with hiccups",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = summaryLine(state),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMid
            )
            Spacer(Modifier.height(22.dp))
            StatRow(successCount = state.successCount, failureCount = state.failureCount)
            Spacer(Modifier.height(16.dp))

            if (state.errors.isNotEmpty()) {
                ErrorsSection(errors = state.errors, modifier = Modifier.weight(1f))
            } else {
                TipCard()
                Spacer(Modifier.weight(1f))
            }

            BottomActions(onDone = onDone, onUndo = onUndo)
        }
    }
}

@Composable
private fun ResultTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkDeep)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    }
}

@Composable
private fun HeroCheck(allSucceeded: Boolean) {
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            (if (allSucceeded) Violet else Coral).copy(alpha = 0.35f),
            Color.Transparent
        )
    )
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(glowBrush, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(if (allSucceeded) BrandGradient else Brush.linearGradient(listOf(Coral, Pink))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (allSucceeded) Icons.Filled.Check else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = InkDeep,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

private fun summaryLine(state: FileExplorerUiState.RenameResult): String = when {
    state.failureCount == 0 && state.successCount == 1 -> "1 file renamed successfully"
    state.failureCount == 0 -> "${state.successCount} files renamed successfully"
    state.successCount == 0 -> "${state.failureCount} failed, nothing was renamed"
    else -> "${state.successCount} renamed · ${state.failureCount} failed"
}

@Composable
private fun StatRow(successCount: Int, failureCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "RENAMED",
            value = successCount,
            color = Mint,
            icon = Icons.Filled.Check,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "FAILED",
            value = failureCount,
            color = Coral,
            icon = Icons.Filled.Close,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(InkElevated)
            .border(1.dp, InkBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Violet.copy(alpha = 0.08f))
            .border(1.dp, Violet.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Violet,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Tip: You can undo this batch any time from History",
            style = MaterialTheme.typography.labelMedium,
            color = TextMid
        )
    }
}

@Composable
private fun ErrorsSection(errors: List<RenameError>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FAILED FILES",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Coral.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(errors) { err -> ErrorRow(err) }
        }
    }
}

@Composable
private fun ErrorRow(error: RenameError) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InkElevated)
            .border(1.dp, Coral.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = error.originalName,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = error.reason,
            style = MaterialTheme.typography.labelSmall,
            color = Coral
        )
    }
}

@Composable
private fun BottomActions(onDone: () -> Unit, onUndo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GradientButton(
            text = "Done",
            icon = Icons.Filled.Check,
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedUndoButton(onClick = onUndo)
    }
}

@Composable
private fun OutlinedUndoButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .border(1.dp, Violet.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Undo",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Violet
        )
    }
}
