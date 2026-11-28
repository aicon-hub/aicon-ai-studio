package dev.aicon.studio.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Speed measured on this phone per model, stored locally. Only real
 * measurements from finished replies land here; everything else the UI
 * shows stays labeled "estimated".
 */
object ModelStats {

    data class Measured(val tokPerSec: Double, val replies: Int, val tokens: Int)

    /** Bumped on every record so Compose screens recompose. */
    var version by mutableIntStateOf(0)
        private set

    private const val PREFS = "aicon_model_stats"

    fun record(context: Context, modelId: String, tokPerSec: Double, tokens: Int) {
        if (tokPerSec <= 0 || tokens <= 0) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = get(context, modelId)
        val replies = (old?.replies ?: 0) + 1
        val totalTokens = (old?.tokens ?: 0) + tokens
        // Running average weighted by tokens.
        val tps = if (old == null) {
            tokPerSec
        } else {
            (old.tokPerSec * old.tokens + tokPerSec * tokens) / totalTokens
        }
        p.edit().putString(key(modelId), "$tps;$replies;$totalTokens").apply()
        version++
    }

    fun get(context: Context, modelId: String): Measured? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(modelId), null) ?: return null
        val parts = raw.split(";")
        if (parts.size != 3) return null
        return Measured(
            tokPerSec = parts[0].toDoubleOrNull() ?: return null,
            replies = parts[1].toIntOrNull() ?: return null,
            tokens = parts[2].toIntOrNull() ?: return null,
        )
    }

    private fun key(modelId: String) = "stats_$modelId"
}
