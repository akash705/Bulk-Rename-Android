package com.bulkrenamer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.bulkrenamer.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun gf(name: String, weight: FontWeight = FontWeight.Normal) =
    Font(googleFont = GoogleFont(name), fontProvider = provider, weight = weight, style = FontStyle.Normal)

val SpaceGrotesk = FontFamily(
    gf("Space Grotesk", FontWeight.Normal),
    gf("Space Grotesk", FontWeight.Medium),
    gf("Space Grotesk", FontWeight.SemiBold),
    gf("Space Grotesk", FontWeight.Bold)
)

val Inter = FontFamily(
    gf("Inter", FontWeight.Normal),
    gf("Inter", FontWeight.Medium),
    gf("Inter", FontWeight.SemiBold),
    gf("Inter", FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    gf("JetBrains Mono", FontWeight.Normal),
    gf("JetBrains Mono", FontWeight.Medium),
    gf("JetBrains Mono", FontWeight.SemiBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    displaySmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
)
