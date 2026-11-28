package dev.aicon.studio.ui.theme

import androidx.compose.ui.graphics.Color

/** AiCoN AI Studio iris, from the picked logo (#7C5CFF, gradient to #6A4DFF). */
val BrandIris = Color(0xFF7C5CFF)
val BrandIrisDeep = Color(0xFF6A4DFF)
val InkLight = Color(0xFF16131C)
val InkDark = Color(0xFFF4F1FB)
val SurfaceLight = Color(0xFFFBFAFF)
val SurfaceDark = Color(0xFF17131F)

// Neutral purple-grey steps used across both schemes.
val SurfaceVariantLight = Color(0xFFEDE8F7)
val SurfaceVariantDark = Color(0xFF2B2536)
val OnSurfaceVariantLight = Color(0xFF575064)
val OnSurfaceVariantDark = Color(0xFFBDB3CC)
val OutlineLight = Color(0xFFC9C1D9)
val OutlineDark = Color(0xFF4A4159)
val SecondaryLight = Color(0xFF6B6480)
val SecondaryDark = Color(0xFFA79FBA)
val SecondaryContainerLight = Color(0xFFEAE4F6)
val SecondaryContainerDark = Color(0xFF35304A)
val OnSecondaryContainerLight = Color(0xFF2A2438)
val OnSecondaryContainerDark = Color(0xFFE4DEF3)

// Warm accents for "needs attention" surfaces (needs-runtime, live chip).
val TertiaryContainerLight = Color(0xFFF9ECD4)
val TertiaryContainerDark = Color(0xFF3A2F16)
val OnTertiaryContainerLight = Color(0xFF4A3300)
val OnTertiaryContainerDark = Color(0xFFEFD9A7)

/** Blend two colors; t = 1 returns other. */
fun Color.mix(other: Color, t: Float): Color = Color(
    red = red + (other.red - red) * t,
    green = green + (other.green - green) * t,
    blue = blue + (other.blue - blue) * t,
    alpha = alpha + (other.alpha - alpha) * t,
)
