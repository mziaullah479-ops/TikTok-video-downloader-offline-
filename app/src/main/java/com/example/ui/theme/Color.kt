package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Authentic TikTok-inspired Modern Dark & High Contrast Palette
val PureBlack = Color(0xFF000000)
val DarkBackground = Color(0xFF0F0F14)
val SurfaceDark = Color(0xFF1A1A24)
val SurfaceVariantDark = Color(0xFF242432)
val SurfaceElevatedDark = Color(0xFF2E2E3F)

// Signature TikTok Accents (Red-Pink & Cyan)
val NeonPink = Color(0xFFFE2C55)          // TikTok Signature Red/Pink
val NeonPinkLight = Color(0xFFFF5277)
val NeonPurple = Color(0xFF7B2CBF)
val NeonPurpleDeep = Color(0xFF5A189A)
val NeonCyan = Color(0xFF25F4EE)          // TikTok Signature Cyan/Teal
val NeonBlue = Color(0xFF00B4D8)

// Clean High-Contrast Text & Borders
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFDCDCE8)
val TextMuted = Color(0xFF9090A6)
val DividerDark = Color(0xFF323246)
val HeartRed = Color(0xFFFE2C55)

// Gradient Brushes for Accents
val BrandGradient = Brush.horizontalGradient(
    colors = listOf(NeonPink, NeonPurple, NeonCyan)
)

val AccentGradientVertical = Brush.verticalGradient(
    colors = listOf(NeonPink, NeonPurple)
)

val CyanGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, NeonBlue)
)

val CardGlowBrush = Brush.linearGradient(
    colors = listOf(NeonPurple.copy(alpha = 0.35f), NeonPink.copy(alpha = 0.25f), Color.Transparent)
)
