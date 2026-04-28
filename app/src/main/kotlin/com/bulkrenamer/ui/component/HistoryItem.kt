package com.bulkrenamer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulkrenamer.data.db.BatchSummary
import com.bulkrenamer.ui.theme.BrandGradient
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.InkHigh
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable

@Composable
fun HistoryItem(
    batch: BatchSummary,
    onUndo: () -> Unit,
    undoEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(InkElevated)
            .border(1.dp, InkBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateBadge(timestamp = batch.timestamp, undone = batch.undone)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildFileCountLabel(batch.fileCount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textDecoration = if (batch.undone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (batch.undone) TextLow else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = relativeTime(batch.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextMid
            )
        }
        Spacer(Modifier.width(10.dp))
        if (batch.undone) {
            UndoneChip()
        } else {
            UndoPill(enabled = undoEnabled, onClick = onUndo)
        }
    }
}

@Composable
private fun DateBadge(timestamp: Long, undone: Boolean) {
    val day = SimpleDateFormat("dd", Locale.getDefault()).format(Date(timestamp))
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(timestamp)).uppercase(Locale.getDefault())
    Column(
        modifier = Modifier
            .size(width = 44.dp, height = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (undone) InkHigh else Violet.copy(alpha = 0.14f))
            .border(
                1.dp,
                if (undone) InkBorder else Violet.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (undone) TextLow else Violet
        )
        Text(
            text = month,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (undone) TextLow else Violet.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun UndoPill(enabled: Boolean, onClick: () -> Unit) {
    val enabledBg: Brush = BrandGradient
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (enabled) enabledBg else Brush.linearGradient(listOf(InkHigh, InkHigh)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Undo",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) InkDeep else TextLow
        )
    }
}

@Composable
private fun UndoneChip() {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(InkHigh)
            .border(1.dp, InkBorder, CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "UNDONE",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextLow
        )
    }
}

private fun buildFileCountLabel(count: Int): String {
    val noun = if (count == 1) "file" else "files"
    return "$count $noun renamed"
}

private fun relativeTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Today · $timeFmt"
    val yesterday = now.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Yesterday · $timeFmt"
    val dateFmt = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(timestamp))
    return dateFmt
}
