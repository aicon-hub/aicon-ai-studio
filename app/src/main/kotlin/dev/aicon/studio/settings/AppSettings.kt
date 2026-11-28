package dev.aicon.studio.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * User-visible settings, kept in SharedPreferences. Theme and accent apply
 * app-wide through Compose state. The default accent is the AiCoN AI Studio
 * iris; the other swatches are alternates the user can pick.
 */
object AppSettings {

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var accentArgb by mutableLongStateOf(BRAND_IRIS)
        private set

    private const val PREFS = "aicon_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_ACCENT = "accent_argb"
    private const val BRAND_IRIS = 0xFF7C5CFFL

    /** Brand iris first, then a few readable alternates. */
    val accentChoices: List<Long> = listOf(
        BRAND_IRIS, // AiCoN iris (brand)
        0xFFFF682F,   // AiCoN orange
        0xFF3B82F6,   // Blue
        0xFF14B8A6,   // Teal
        0xFFF43F5E,   // Rose
    )

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        accentArgb = prefs.getLong(KEY_ACCENT, BRAND_IRIS)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setAccent(context: Context, argb: Long) {
        accentArgb = argb
        prefs(context).edit().putLong(KEY_ACCENT, argb).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
