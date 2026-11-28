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

    /** Release the loaded model and native resources. */
    suspend fun unload()
}

/** One message in the running conversation. */
data class ChatTurn(val role: Role, val text: String)

enum class Role { USER, ASSISTANT }

/** Failure reported by an engine in terms the UI can show as-is. */
class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause)
