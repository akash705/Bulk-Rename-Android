package com.bulkrenamer.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BrandGradient: Brush
    get() = Brush.linearGradient(listOf(Violet, Pink))

val BrandGradientRadial: Brush
    get() = Brush.radialGradient(
        colors = listOf(Color(0x66A78BFA), Color(0x00A78BFA)),
        radius = 600f
    )
