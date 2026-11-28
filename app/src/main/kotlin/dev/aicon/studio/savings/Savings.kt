package dev.aicon.studio.savings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Impact tracking: tokens generated on-device, day streaks and levels, and
 * an estimated range of electricity and water saved versus running the same
 * tokens on a cloud model. Fully local (SharedPreferences only), no account,
 * no network. On by default, a single switch turns it off.
 *
 * Method (see the "How we estimate" page in the app):
 * a 2025 Google measurement puts a median Gemini Apps text prompt at about
 * 0.24 Wh and 0.26 mL of water; Li et al. (2023, "Making AI Less Thirsty")
 * estimate several Wh and 7-48 mL per medium GPT-3-class request. Scaled to
 * tokens, we count 0.8-33 Wh and 0.9-100 mL per 1,000 tokens, and we show
 * the whole range instead of one tidy number. These are rough estimates,
 * not measurements of any datacenter.
 */
object Savings {

    data class Level(val number: Int, val name: String, val nextAt: Int?)

    var enabled by mutableStateOf(true)
        private set

    var totalTokens by mutableIntStateOf(0)
        private set

    /** Bumped on every record so Compose screens recompose. */
    var version by mutableIntStateOf(0)
        private set

    private const val PREFS = "aicon_savings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TOKENS = "total_tokens"
    private const val KEY_DAYS = "active_days"

    private val LEVELS = listOf(
        0 to "Seed",
        2_000 to "Sprout",
        10_000 to "Tree",
        30_000 to "Grove",
        80_000 to "Forest",
    )

    fun load(context: Context) {
        val p = prefs(context)
        enabled = p.getBoolean(KEY_ENABLED, true)
        totalTokens = p.getInt(KEY_TOKENS, 0)
    }

    fun setEnabled(context: Context, on: Boolean) {
        enabled = on
        prefs(context).edit().putBoolean(KEY_ENABLED, on).apply()
        version++
    }

    fun record(context: Context, tokens: Int) {
        if (!enabled || tokens <= 0) return
        val p = prefs(context)
        totalTokens = p.getInt(KEY_TOKENS, 0) + tokens
        val days = p.getStringSet(KEY_DAYS, emptySet())!!.toMutableSet()
        days.add(dayKey(System.currentTimeMillis()))
        // Keep the set bounded; 400 days is far beyond any streak lookup.
        val trimmed = days.sorted().takeLast(400).toSet()
        p.edit()
            .putInt(KEY_TOKENS, totalTokens)
            .putStringSet(KEY_DAYS, trimmed)
            .apply()
        version++
    }

    fun reset(context: Context) {
        totalTokens = 0
        prefs(context).edit()
            .putInt(KEY_TOKENS, 0)
            .putStringSet(KEY_DAYS, emptySet())
            .apply()
        version++
    }

    /** Consecutive days with replies, ending today or yesterday. */
    fun streak(context: Context): Int {
        val days = prefs(context).getStringSet(KEY_DAYS, emptySet()) ?: return 0
        var cursor = Calendar.getInstance()
        if (!days.contains(dayKey(cursor.timeInMillis))) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
            if (!days.contains(dayKey(cursor.timeInMillis))) return 0
        }
        var count = 0
        while (days.contains(dayKey(cursor.timeInMillis))) {
            count++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    fun level(): Level {
        var current = 0 to LEVELS.first().second
        for (entry in LEVELS) {
            if (totalTokens >= entry.first) current = entry
        }
        val index = LEVELS.indexOf(current)
        val next = LEVELS.getOrNull(index + 1)?.first
        return Level(index + 1, current.second, next)
    }

    /** Estimated electricity saved, Wh, low and high end. */
    fun savedWh(): Pair<Double, Double> =
        totalTokens / 1000.0 * 0.8 to totalTokens / 1000.0 * 33.0

    /** Estimated water saved, mL, low and high end. */
    fun savedMl(): Pair<Double, Double> =
        totalTokens / 1000.0 * 0.9 to totalTokens / 1000.0 * 100.0

    private fun dayKey(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
