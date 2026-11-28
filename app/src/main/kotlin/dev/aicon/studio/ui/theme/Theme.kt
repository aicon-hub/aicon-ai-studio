package dev.aicon.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.aicon.studio.settings.AppSettings

private val AiCoNShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun AiCoNStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = BrandIris,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.mix(SurfaceDark, 0.7f),
            onPrimaryContainer = InkDark,
            secondary = SecondaryDark,
            onSecondary = InkLight,
            secondaryContainer = SecondaryContainerDark,
            onSecondaryContainer = OnSecondaryContainerDark,
            tertiaryContainer = TertiaryContainerDark,
            onTertiaryContainer = OnTertiaryContainerDark,
            background = SurfaceDark,
            onBackground = InkDark,
            surface = SurfaceDark,
            onSurface = InkDark,
            surfaceVariant = SurfaceVariantDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            outline = OutlineDark,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.mix(Color.White, 0.86f),
            onPrimaryContainer = accent.mix(Color.Black, 0.45f),
            secondary = SecondaryLight,
            onSecondary = Color.White,
            secondaryContainer = SecondaryContainerLight,
            onSecondaryContainer = OnSecondaryContainerLight,
            tertiaryContainer = TertiaryContainerLight,
            onTertiaryContainer = OnTertiaryContainerLight,
            background = SurfaceLight,
            onBackground = InkLight,
            surface = SurfaceLight,
            onSurface = InkLight,
            surfaceVariant = SurfaceVariantLight,
            onSurfaceVariant = OnSurfaceVariantLight,
            outline = OutlineLight,
        )
    }
    // Display settings: layout density scales dp, text size scales sp.
    val base = LocalDensity.current
    val scaled = Density(
        density = base.density * AppSettings.densityScale,
        fontScale = base.fontScale * AppSettings.textScale,
    )
    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AiCoNTypography,
            shapes = AiCoNShapes,
            content = content,
        )
    }
}
