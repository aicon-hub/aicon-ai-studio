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

    /** Decode threads; 0 means auto (scales with the core count). */
    var perfThreads by mutableStateOf(0)
        private set

    /** Context size in tokens; one of 1024, 2048, 4096. */
    var perfContextSize by mutableStateOf(2048)
        private set

    /** Quantize the KV cache to q8_0 (faster long chats, small quality cost). */
    var perfKvQ8 by mutableStateOf(false)
        private set

    private const val PREFS = "aicon_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_ACCENT = "accent_argb"
    private const val KEY_PERF_THREADS = "perf_threads"
    private const val KEY_PERF_CTX = "perf_context_size"
    private const val KEY_PERF_KV_Q8 = "perf_kv_q8"
    val contextSizeChoices: List<Int> = listOf(1024, 2048, 4096)
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
        perfThreads = prefs.getInt(KEY_PERF_THREADS, 0)
        perfContextSize = prefs.getInt(KEY_PERF_CTX, 2048)
        perfKvQ8 = prefs.getBoolean(KEY_PERF_KV_Q8, false)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setAccent(context: Context, argb: Long) {
        accentArgb = argb
        prefs(context).edit().putLong(KEY_ACCENT, argb).apply()
    }

    fun setPerfThreads(context: Context, threads: Int) {
        perfThreads = threads
        prefs(context).edit().putInt(KEY_PERF_THREADS, threads).apply()
    }

    fun setPerfContextSize(context: Context, size: Int) {
        perfContextSize = size
        prefs(context).edit().putInt(KEY_PERF_CTX, size).apply()
    }

    fun setPerfKvQ8(context: Context, enabled: Boolean) {
        perfKvQ8 = enabled
        prefs(context).edit().putBoolean(KEY_PERF_KV_Q8, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
