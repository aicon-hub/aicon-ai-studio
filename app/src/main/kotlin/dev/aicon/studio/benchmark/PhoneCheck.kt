package dev.aicon.studio.benchmark

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.aicon.studio.engine.llamacpp.LlamaCppEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Check this phone": an indicative CPU benchmark (no model needed) plus
 * device facts. Runs fully on the phone, is stored locally, and sorts the
 * catalog by how well each model fits. The tier is a rough bucket, not a
 * promise of any tok/s.
 */
object PhoneCheck {

    enum class Tier(val label: String) { ENTRY("Entry"), MID("Mid-range"), FLAGSHIP("Flagship") }

    data class Result(
        val tier: Tier,
        val memoryCopyGBs: Double,
        val fpGFLOPS: Double,
        val cores: Int,
        val totalRamBytes: Long,
        val soc: String,
        val measuredAtEpochMs: Long,
    )

    var result by mutableStateOf<Result?>(null)
        private set

    var running by mutableStateOf(false)
        private set

    private const val PREFS = "aicon_phone_check"
    private const val KEY_MEM = "mem_gbs"
    private const val KEY_FP = "fp_gflops"
    private const val KEY_CORES = "cores"
    private const val KEY_RAM = "total_ram"
    private const val KEY_SOC = "soc"
    private const val KEY_AT = "measured_at"

    fun load(context: Context) {
        if (result != null) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(KEY_MEM)) return
        val ram = p.getLong(KEY_RAM, 0)
        val mem = p.getFloat(KEY_MEM, 0f).toDouble()
        result = Result(
            tier = classify(mem, ram),
            memoryCopyGBs = mem,
            fpGFLOPS = p.getFloat(KEY_FP, 0f).toDouble(),
            cores = p.getInt(KEY_CORES, 0),
            totalRamBytes = ram,
            soc = p.getString(KEY_SOC, null) ?: "",
            measuredAtEpochMs = p.getLong(KEY_AT, 0),
        )
    }

    suspend fun run(context: Context) {
        if (running) return
        running = true
        try {
            withContext(Dispatchers.Default) {
                val b = LlamaCppEngine(context.applicationContext).runBenchmark()
                val ram = totalRam(context)
                val r = Result(
                    tier = classify(b.memoryCopyGBs, ram),
                    memoryCopyGBs = b.memoryCopyGBs,
                    fpGFLOPS = b.fpGFLOPS,
                    cores = b.cores,
                    totalRamBytes = ram,
                    soc = Build.HARDWARE ?: "",
                    measuredAtEpochMs = System.currentTimeMillis(),
                )
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putFloat(KEY_MEM, r.memoryCopyGBs.toFloat())
                    .putFloat(KEY_FP, r.fpGFLOPS.toFloat())
                    .putInt(KEY_CORES, r.cores)
                    .putLong(KEY_RAM, r.totalRamBytes)
                    .putString(KEY_SOC, r.soc)
                    .putLong(KEY_AT, r.measuredAtEpochMs)
                    .apply()
                result = r
            }
        } finally {
            running = false
        }
    }

    private fun totalRam(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }

    /** Indicative bucket from measured memory bandwidth and total RAM. */
    private fun classify(memGBs: Double, totalRamBytes: Long): Tier {
        val ramGb = totalRamBytes / 1e9
        return when {
            memGBs >= 18.0 && ramGb >= 7.0 -> Tier.FLAGSHIP
            memGBs >= 8.0 && ramGb >= 3.5 -> Tier.MID
            else -> Tier.ENTRY
        }
    }

    enum class Fit(val label: String) { GOOD("Runs well"), TIGHT("Tight fit"), TOO_BIG("Too big") }

    /**
     * Fit of a model on this phone, from the catalog RAM estimate against
     * the share of RAM a model plus context can reasonably use. Null until
     * a check has run on this phone.
     */
    fun fitFor(ramMb: Int?): Fit? {
        val r = result ?: return null
        val need = (ramMb ?: return null).toLong() * 1_000_000L
        val usable = (r.totalRamBytes * 0.55).toLong()
        return when {
            need * 4 <= usable * 3 -> Fit.GOOD
            need <= usable -> Fit.TIGHT
            else -> Fit.TOO_BIG
        }
    }

    /** Sort rank: best fit first, unknown after tight, too big last. */
    fun fitRank(ramMb: Int?): Int = when (fitFor(ramMb)) {
        Fit.GOOD -> 0
        Fit.TIGHT -> 1
        Fit.TOO_BIG -> 3
        null -> 2
    }
}
