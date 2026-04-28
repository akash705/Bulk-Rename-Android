package com.bulkrenamer.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val PlayfulDark = darkColorScheme(
    primary = Violet,
    onPrimary = InkDeep,
    primaryContainer = Color(0xFF3A2B6B),
    onPrimaryContainer = Color(0xFFE8DEFF),
    secondary = Pink,
    onSecondary = InkDeep,
    secondaryContainer = Color(0xFF5A2A4D),
    onSecondaryContainer = Color(0xFFFFD9EC),
    tertiary = Cyan,
    onTertiary = InkDeep,
    background = InkDeep,
    onBackground = TextHigh,
    surface = InkDeep,
    onSurface = TextHigh,
    surfaceVariant = InkElevated,
    onSurfaceVariant = TextMid,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHigh,
    outline = InkBorder,
    outlineVariant = Color(0xFF1F1F30),
    error = Coral,
    onError = InkDeep
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BulkRenamerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PlayfulDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
