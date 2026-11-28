package dev.aicon.studio.ui.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aicon.studio.model.ModelCatalog
import dev.aicon.studio.model.ModelInfo
import dev.aicon.studio.model.ModelManager
import java.io.File
import java.io.FileOutputStream

/**
 * The model manager: download, verify, load, delete, or import a GGUF from
 * storage. Downloads start only from a tap here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(vm: ModelsViewModel = viewModel(), onBack: () -> Unit) {
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val loadedPath by vm.loadedModelPath.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val loadError by vm.loadError.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // SAF gives us a content URI; copy into app storage so the engine sees a real file.
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val dest = File(context.filesDir, "models/imported/imported-model.gguf")
            dest.parentFile?.mkdirs()
            runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Could not open the picked file" }
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
            }.onSuccess { vm.loadImported(dest.absolutePath) }
        }
    }

    LaunchedEffect(loadError) {
        loadError?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ModelCatalog.models, key = { it.id }) { info ->
                ModelCard(info, downloads[info.id], loadedPath, loading, vm)
            }
            item {
                OutlinedButton(
                    onClick = { importPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import a GGUF from storage")
                }
            }
            item {
                Text(
                    "Downloads start only when you tap. Chat never uses the network.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    info: ModelInfo,
    state: ModelManager.DownloadState?,
    loadedPath: String?,
    loading: Boolean,
    vm: ModelsViewModel,
) {
    val installed = vm.isInstalled(info)
    val isLoaded = loadedPath?.endsWith("/models/${info.id}/${info.fileName}") == true

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(info.title, style = MaterialTheme.typography.titleMedium)
            Text(info.summary, style = MaterialTheme.typography.bodySmall)
            Text(
                "Size: ${formatSize(info.sizeBytes)}  ·  License: ${info.license}",
                style = MaterialTheme.typography.bodySmall,
            )
            when {
                state?.status == ModelManager.Status.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${(state.progress * 100).toInt()}% of ${formatSize(state.bytesTotal)}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { vm.cancel(info.id) }) { Text("Cancel") }
                    }
                }
                state?.status == ModelManager.Status.VERIFYING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text(
                            "  Checking integrity...",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                isLoaded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Loaded and ready",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { vm.unload() }, enabled = !loading) { Text("Unload") }
                    }
                }
                installed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state?.error ?: "Downloaded",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { vm.delete(info) }, enabled = !loading) { Text("Delete") }
                        Button(onClick = { vm.load(info) }, enabled = !loading) {
                            Text(if (loading) "Loading..." else "Load")
                        }
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state?.error ?: "Not downloaded",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = { vm.download(info) }, enabled = !loading) {
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1_000_000_000) "%.2f GB".format(bytes / 1e9)
    else "%.0f MB".format(bytes / 1e6)
