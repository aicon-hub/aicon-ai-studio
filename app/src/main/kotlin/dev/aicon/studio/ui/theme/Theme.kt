package dev.aicon.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Iris,
    onPrimary = InkDark,
    background = SurfaceLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
)

private val DarkColors = darkColorScheme(
    primary = IrisDark,
    onPrimary = InkLight,
    background = SurfaceDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
)

@Composable
fun AiCoNStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
