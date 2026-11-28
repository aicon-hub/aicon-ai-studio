package dev.aicon.studio.ui.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aicon.studio.EngineHolder
import dev.aicon.studio.model.ModelInfo
import dev.aicon.studio.model.ModelManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Drives the Models screen: catalog downloads and engine load state. */
class ModelsViewModel(app: Application) : AndroidViewModel(app) {

    val downloads: StateFlow<Map<String, ModelManager.DownloadState>> = ModelManager.downloads
    val loadedModelPath: StateFlow<String?> = EngineHolder.loadedModelPath
    val loading: StateFlow<Boolean> = EngineHolder.loading
    val loadError: StateFlow<String?> = EngineHolder.loadError

    fun download(info: ModelInfo) = ModelManager.download(info)

    fun cancel(id: String) = ModelManager.cancel(id)

    fun delete(info: ModelInfo) = ModelManager.delete(info)

    fun isInstalled(info: ModelInfo): Boolean = ModelManager.isInstalled(info)

    fun load(info: ModelInfo) {
        val file = ModelManager.installedFile(info) ?: return
        viewModelScope.launch {
            EngineHolder.loadModel(getApplication(), file.absolutePath, info.templateAsset)
        }
    }

    fun loadImported(path: String) {
        viewModelScope.launch {
            EngineHolder.loadModel(getApplication(), path, null)
        }
    }

    fun unload() {
        viewModelScope.launch { EngineHolder.unload() }
    }

    fun clearError() = EngineHolder.clearError()
}
