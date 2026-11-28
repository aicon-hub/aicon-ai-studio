package dev.aicon.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AiCoNStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = Iris,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = SurfaceDark,
            onBackground = InkDark,
            surface = SurfaceDark,
            onSurface = InkDark,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = SurfaceLight,
            onBackground = InkLight,
            surface = SurfaceLight,
            onSurface = InkLight,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
