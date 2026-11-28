package dev.aicon.studio

import android.content.Context
import dev.aicon.studio.engine.EngineException
import dev.aicon.studio.engine.llamacpp.LlamaCppEngine
import dev.aicon.studio.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The one engine instance for the app process. The chat screen and the
 * models screen both drive it; a single model is loaded at a time.
 */
object EngineHolder {

    private var engineInstance: LlamaCppEngine? = null

    private val _loadedModelPath = MutableStateFlow<String?>(null)
    val loadedModelPath: StateFlow<String?> = _loadedModelPath.asStateFlow()

    private val _visionReady = MutableStateFlow(false)
    val visionReady: StateFlow<Boolean> = _visionReady.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun engine(context: Context): LlamaCppEngine =
        engineInstance ?: LlamaCppEngine(context.applicationContext).also { engineInstance = it }

    suspend fun loadModel(
        context: Context,
        path: String,
        templateAsset: String?,
        mmprojPath: String? = null,
    ) {
        _loading.value = true
        _loadError.value = null
        try {
            withContext(Dispatchers.Default) {
                val template = templateAsset?.let { asset ->
                    context.assets.open(asset).bufferedReader().use { it.readText() }
                }
                val engine = engine(context)
                engine.setChatTemplate(template)
                engine.setMmproj(mmprojPath)
                engine.setPerformance(
                    AppSettings.perfThreads,
                    AppSettings.perfContextSize,
                    AppSettings.perfKvQ8,
                )
                engine.load(path)
            }
            _loadedModelPath.value = path
            _visionReady.value = mmprojPath != null && engine(context).hasVision()
        } catch (e: EngineException) {
            _loadError.value = e.message
        } finally {
            _loading.value = false
        }
    }

    suspend fun unload() {
        withContext(Dispatchers.Default) { engineInstance?.unload() }
        _loadedModelPath.value = null
        _visionReady.value = false
    }

    fun isLoaded(): Boolean = engineInstance?.isLoaded() == true

    fun hasVision(): Boolean = engineInstance?.let { it.isLoaded() && it.hasVision() } == true

    fun clearError() {
        _loadError.value = null
    }
}
