package dev.aicon.studio.engine.llamacpp

import android.content.Context
import dev.aicon.studio.engine.ChatTurn
import dev.aicon.studio.engine.EngineException
import dev.aicon.studio.engine.InferenceEngine
import dev.aicon.studio.engine.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * On-device GGUF inference through llama.cpp (v1.1.0, built from source in
 * CI). Runs fully offline; the only files it touches are the model and the
 * mmproj vision adapter the user picked. Images are handled through
 * llama.cpp's mtmd library.
 */
class LlamaCppEngine(private val appContext: Context) : InferenceEngine {

    override val name: String = "llama.cpp v1.1.0"

    @Volatile
    private var loaded = false

    /** Jinja template applied at load time for models without an embedded one. */
    @Volatile
    private var pendingTemplate: String? = null

    /** Vision adapter applied at load time for multimodal models. */
    @Volatile
    private var pendingMmproj: String? = null

    private external fun nativeInit(nativeLibDir: String)
    private external fun nativeLoadModel(modelPath: String): Int
    private external fun nativeLoadMmproj(mmprojPath: String): Int
    private external fun nativeHasVision(): Boolean
    private external fun nativeSetChatTemplate(template: String)
    private external fun nativePrepare(): Int
    private external fun nativeProcessSystemPrompt(prompt: String): Int
    private external fun nativeProcessUserPrompt(prompt: String, nPredict: Int): Int
    private external fun nativeProcessUserPromptWithImage(
        prompt: String,
        imagePath: String,
        nPredict: Int,
    ): Int
    private external fun nativeGenerateNextToken(): String?
    private external fun nativeUnload()
    private external fun nativeShutdown()

    override fun setChatTemplate(template: String?) {
        pendingTemplate = template
    }

    override fun setMmproj(mmprojPath: String?) {
        pendingMmproj = mmprojPath
    }

    override fun hasVision(): Boolean = loaded && nativeHasVision()

    override suspend fun load(modelPath: String) {
        val f = File(modelPath)
        if (!f.exists() || !f.isFile) {
            throw EngineException("Model file not found: $modelPath")
        }
        if (f.length() == 0L) {
            throw EngineException("Model file is empty: $modelPath")
        }
        if (!loaded) {
            nativeInit(appContext.applicationInfo.nativeLibraryDir)
        }
        if (nativeLoadModel(modelPath) != 0) {
            throw EngineException("llama.cpp could not load this file. Is it a GGUF model?")
        }
        pendingTemplate?.let { nativeSetChatTemplate(it) }
        if (nativePrepare() != 0) {
            throw EngineException("The model loaded but the context could not start. It may be too big for this phone.")
        }
        pendingMmproj?.let { mmproj ->
            when (nativeLoadMmproj(mmproj)) {
                0 -> Unit
                2 -> throw EngineException("The vision adapter file could not be loaded.")
                else -> throw EngineException("This model does not work with its vision adapter.")
            }
        }
        if (nativeProcessSystemPrompt(DEFAULT_SYSTEM_PROMPT) != 0) {
            throw EngineException("Could not seed the chat template.")
        }
        loaded = true
    }

    override fun isLoaded(): Boolean = loaded

    override fun generate(prompt: String, history: List<ChatTurn>): Flow<String> =
        generate(prompt, history, null)

    override fun generate(prompt: String, history: List<ChatTurn>, imagePath: String?): Flow<String> = flow {
        if (!loaded) throw EngineException("No model loaded")
        if (imagePath == null) {
            if (nativeProcessUserPrompt(prompt, MAX_PREDICT_TOKENS) != 0) {
                throw EngineException("Prompt processing failed.")
            }
        } else {
            when (nativeProcessUserPromptWithImage(prompt, imagePath, MAX_PREDICT_TOKENS)) {
                0 -> Unit
                1 -> throw EngineException("This model cannot see images. Load a vision model from the Models screen.")
                2 -> throw EngineException("The image could not be read.")
                4 -> throw EngineException("The image does not fit the remaining chat context. Start a new chat.")
                else -> throw EngineException("The image could not be processed.")
            }
        }
        while (true) {
            val piece = nativeGenerateNextToken() ?: break
            if (piece.isNotEmpty()) emit(piece)
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun unload() {
        if (loaded) {
            nativeUnload()
            loaded = false
        }
        pendingMmproj = null
    }

    companion object {
        private const val MAX_PREDICT_TOKENS = 512
        private const val DEFAULT_SYSTEM_PROMPT =
            "You are a helpful assistant running fully on the user's phone. Answer plainly."

        init {
            System.loadLibrary("aicon-llama")
        }
    }
}
