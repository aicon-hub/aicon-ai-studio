package dev.aicon.studio.engine.llamacpp

import android.app.ActivityManager
import android.content.Context
import dev.aicon.studio.engine.ChatTurn
import dev.aicon.studio.engine.DeviceBenchmark
import dev.aicon.studio.engine.EngineException
import dev.aicon.studio.engine.EngineStats
import dev.aicon.studio.engine.InferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Locale

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

    /** Speed settings applied at load time; threads <= 0 means auto. */
    @Volatile
    private var pendingThreads = 0

    @Volatile
    private var pendingContextSize = 0

    @Volatile
    private var pendingKvQ8 = false

    private external fun nativeInit(nativeLibDir: String)
    private external fun nativeLoadModel(modelPath: String): Int
    private external fun nativeLoadMmproj(mmprojPath: String): Int
    private external fun nativeHasVision(): Boolean
    private external fun nativeSetChatTemplate(template: String)
    private external fun nativeSetPerformance(threads: Int, contextSize: Int, kvQ8: Boolean)
    private external fun nativePrepare(): Int
    private external fun nativeProcessSystemPrompt(prompt: String): Int
    private external fun nativeProcessUserPrompt(prompt: String, nPredict: Int): Int
    private external fun nativeProcessUserPromptWithImage(
        prompt: String,
        imagePath: String,
        nPredict: Int,
    ): Int
    private external fun nativeGenerateTokens(maxPieces: Int): Array<String>
    private external fun nativeGetPerfStats(): DoubleArray
    private external fun nativeRunBenchmark(): DoubleArray
    private external fun nativeUnload()
    private external fun nativeShutdown()

    override fun setChatTemplate(template: String?) {
        pendingTemplate = template
    }

    override fun setMmproj(mmprojPath: String?) {
        pendingMmproj = mmprojPath
    }

    override fun setPerformance(threads: Int, contextSize: Int, kvCacheQ8: Boolean) {
        pendingThreads = threads
        pendingContextSize = contextSize
        pendingKvQ8 = kvCacheQ8
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
        guardAvailableMemory(f.length())
        if (!loaded) {
            nativeInit(appContext.applicationInfo.nativeLibraryDir)
        }
        if (nativeLoadModel(modelPath) != 0) {
            throw EngineException("llama.cpp could not load this file. Is it a GGUF model?")
        }
        pendingTemplate?.let { nativeSetChatTemplate(it) }
        nativeSetPerformance(pendingThreads, pendingContextSize, pendingKvQ8)
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

    /**
     * Refuse to start a model that clearly cannot fit in free memory: paging
     * a mmap'd model in and out is far slower than any tuning can fix.
     */
    private fun guardAvailableMemory(modelBytes: Long) {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        if (info.availMem < modelBytes) {
            throw EngineException(
                "Not enough free memory: this phone has about " + gb(info.availMem) +
                    " GB free and this model is " + gb(modelBytes) +
                    " GB before the chat context. Try a smaller model.",
            )
        }
    }

    private fun gb(bytes: Long): String =
        String.format(Locale.US, "%.1f", bytes / 1_000_000_000.0)

    override fun isLoaded(): Boolean = loaded

    override fun stats(): EngineStats {
        if (!loaded) return EngineStats()
        val s = nativeGetPerfStats()
        return EngineStats(
            prefillTokPerSec = s[0],
            decodeTokPerSec = s[1],
            generatedTokens = s[2].toInt(),
        )
    }

    override fun runBenchmark(): DeviceBenchmark {
        val b = nativeRunBenchmark()
        return DeviceBenchmark(
            memoryCopyGBs = b[0],
            fpGFLOPS = b[1],
            cores = b[2].toInt(),
        )
    }

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
            val pieces = nativeGenerateTokens(GENERATE_BATCH_PIECES)
            if (pieces.isEmpty()) break
            for (piece in pieces) {
                if (piece.isNotEmpty()) emit(piece)
            }
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
        private const val GENERATE_BATCH_PIECES = 8
        private const val DEFAULT_SYSTEM_PROMPT =
            "You are a helpful assistant running fully on the user's phone. Answer plainly."

        init {
            System.loadLibrary("aicon-llama")
        }
    }
}
