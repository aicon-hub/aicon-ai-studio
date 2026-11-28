package dev.aicon.studio.engine

import kotlinx.coroutines.flow.Flow

/**
 * A local inference backend (llama.cpp, LiteRT-LM, ...).
 *
 * Implementations run fully on device. The interface is deliberately small:
 * load a model file, stream a reply, release resources. Backends never see
 * the network; model files arrive through the model manager, not the engine.
 */
interface InferenceEngine {

    /** Human-readable backend name, shown in engine settings. */
    val name: String

    /**
     * Optional jinja chat template used when the model file carries none.
     * Set before [load]. Default: ignored by the backend.
     */
    fun setChatTemplate(template: String?) {}

    /**
     * Optional vision adapter (mmproj GGUF) for multimodal models.
     * Set before [load]. Default: ignored by the backend.
     */
    fun setMmproj(mmprojPath: String?) {}

    /** True when the loaded model can accept images. */
    fun hasVision(): Boolean = false

    /**
     * Load [modelPath] (a local file the user picked or downloaded).
     * @throws EngineException if the file is missing, unsupported, or does
     *         not fit the device's memory.
     */
    suspend fun load(modelPath: String)

    /** True after [load] succeeded and before [unload]. */
    fun isLoaded(): Boolean

    /**
     * Stream a reply to [prompt] as tokens. The flow completes when the
     * reply is done and throws [EngineException] on failure.
     */
    fun generate(prompt: String, history: List<ChatTurn>): Flow<String>

    /**
     * Same as [generate], with one image attached. Backends without vision
     * fall back to the text-only path; callers should check [hasVision].
     */
    fun generate(prompt: String, history: List<ChatTurn>, imagePath: String?): Flow<String> =
        generate(prompt, history)

    /** Release the loaded model and native resources. */
    suspend fun unload()

    /**
     * Speed preferences applied at the next [load]. threads <= 0 means auto.
     * Default: ignored by the backend.
     */
    fun setPerformance(threads: Int, contextSize: Int, kvCacheQ8: Boolean) {}

    /** Measured speed of the current session; zeroes when nothing ran yet. */
    fun stats(): EngineStats = EngineStats()

    /**
     * Indicative device benchmark (memory copy and FP throughput, core
     * count). Null when the backend has no benchmark.
     */
    fun runBenchmark(): DeviceBenchmark? = null
}

/** Measured inference speed of the current session. */
data class EngineStats(
    val prefillTokPerSec: Double = 0.0,
    val decodeTokPerSec: Double = 0.0,
    val generatedTokens: Int = 0,
)

/** Indicative micro-benchmark result; not an inference measurement. */
data class DeviceBenchmark(
    val memoryCopyGBs: Double,
    val fpGFLOPS: Double,
    val cores: Int,
)

/** One message in the running conversation. */
data class ChatTurn(val role: Role, val text: String)

enum class Role { USER, ASSISTANT }

/** Failure reported by an engine in terms the UI can show as-is. */
class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause)
