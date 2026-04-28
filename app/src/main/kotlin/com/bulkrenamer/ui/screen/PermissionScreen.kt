package com.bulkrenamer.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulkrenamer.ui.component.GradientButton
import com.bulkrenamer.ui.theme.BrandGradient
import com.bulkrenamer.ui.theme.BrandGradientRadial
import com.bulkrenamer.ui.theme.Cyan
import com.bulkrenamer.ui.theme.InkDeep
import com.bulkrenamer.ui.theme.InkElevated
import com.bulkrenamer.ui.theme.InkHigh
import com.bulkrenamer.ui.theme.Mint
import com.bulkrenamer.ui.theme.Pink
import com.bulkrenamer.ui.theme.TextDim
import com.bulkrenamer.ui.theme.TextLow
import com.bulkrenamer.ui.theme.TextMid
import com.bulkrenamer.ui.theme.Violet

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InkDeep)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        BrandHeader()
        Spacer(Modifier.height(24.dp))
        HeroIllustration()
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Let's unlock your files",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Grant file access so we can rename, organize, and work our magic on your files. Everything stays on your device — nothing uploaded, ever.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp)
        )
        Spacer(Modifier.height(24.dp))
        WhyCard()
        Spacer(Modifier.weight(1f))
        GradientButton(
            text = "Grant file access",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    settingsLauncher.launch(intent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        PrivacyFooter()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = InkDeep,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "BULK RENAMER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = Violet
        )
    }
}

@Composable
private fun HeroIllustration() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .widthIn(max = 320.dp)
    ) {
        // Soft glow halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandGradientRadial)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(InkElevated)
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Stacked "file cards" illusion
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0x55A78BFA), Color(0x22F472B6))
                        )
                    )
                    .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(16.dp))
            )
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 130.dp)
                    .padding(top = 48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0x99A78BFA), Color(0x66F472B6))
                        )
                    )
                    .border(1.dp, Color(0x55A78BFA), RoundedCornerShape(18.dp))
            )
            // Magic wand badge (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = InkDeep,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun WhyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(InkElevated)
            .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "WHY WE NEED THIS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = Violet.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(14.dp))
        WhyRow(
            icon = Icons.Filled.FolderOpen,
            tint = Cyan,
            title = "Browse",
            subtitle = "See folders and files"
        )
        Spacer(Modifier.height(14.dp))
        WhyRow(
            icon = Icons.Filled.Rule,
            tint = Pink,
            title = "Rename",
            subtitle = "Apply rules to your selection"
        )
        Spacer(Modifier.height(14.dp))
        WhyRow(
            icon = Icons.Filled.History,
            tint = Mint,
            title = "Undo",
            subtitle = "Revert any batch instantly"
        )
    }
}

@Composable
private fun WhyRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InkHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )
        }
    }
}

@Composable
private fun PrivacyFooter() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = TextDim,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "100% LOCAL",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            ),
            color = TextDim
        )
        Dot()
        Text(
            text = "NO CLOUD",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = TextDim
        )
        Dot()
        Text(
            text = "NO TRACKING",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = TextDim
        )
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(TextDim)
    )
}
