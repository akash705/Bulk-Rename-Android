package com.bulkrenamer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulkrenamer.ui.theme.BrandGradient
import com.bulkrenamer.ui.theme.InkBorder
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.InkHigh
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid

@Composable
fun SelectionToolbar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(InkElevated)
            .border(1.dp, InkBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ready to rename",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = TextLow
            )
        }
        CircleIconAction(icon = Icons.Filled.SelectAll, contentDesc = "Select all", onClick = onSelectAll)
        Spacer(Modifier.width(8.dp))
        CircleIconAction(icon = Icons.Filled.Close, contentDesc = "Clear", onClick = onDeselectAll)
        Spacer(Modifier.width(12.dp))
        RenameGradientButton(onClick = onRename)
    }
}

@Composable
private fun CircleIconAction(icon: ImageVector, contentDesc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(InkHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = TextMid,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RenameGradientButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(BrandGradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = InkDeep,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Rename",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = InkDeep
        )
    }
}
