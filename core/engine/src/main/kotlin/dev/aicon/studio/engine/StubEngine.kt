package dev.aicon.studio.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Placeholder engine used until the real backends (llama.cpp, LiteRT-LM)
 * land. It validates that the model file exists and fits a size sanity
 * check, then streams a fixed notice. It lets the full app wiring
 * (load, chat loop, unload) be exercised and tested end to end.
 */
class StubEngine : InferenceEngine {

    override val name: String = "Stub (no inference yet)"

    private var model: File? = null

    override suspend fun load(modelPath: String) {
        val f = File(modelPath)
        if (!f.exists() || !f.isFile) {
            throw EngineException("Model file not found: $modelPath")
        }
        if (f.length() == 0L) {
            throw EngineException("Model file is empty: $modelPath")
        }
        model = f
    }

    override fun isLoaded(): Boolean = model != null

    override fun generate(prompt: String, history: List<ChatTurn>): Flow<String> = flow {
        val loaded = model ?: throw EngineException("No model loaded")
        val reply = "Engine wiring works. ${loaded.name} is loaded, but real " +
            "inference arrives with the llama.cpp backend in Phase 1."
        for (token in reply.split(" ")) {
            emit("$token ")
            delay(30)
        }
    }

    override suspend fun unload() {
        model = null
    }
}
