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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aicon.studio.benchmark.PhoneCheck
import dev.aicon.studio.model.ModelCatalog
import dev.aicon.studio.model.ModelCategory
import dev.aicon.studio.model.ModelInfo
import dev.aicon.studio.model.ModelManager
import dev.aicon.studio.model.ModelStats
import dev.aicon.studio.model.Readiness
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * The model manager, grouped by category tabs. READY entries download and
 * verify here; NEEDS_RUNTIME and NOT_YET entries explain themselves and
 * offer nothing to tap. Downloads start only from a tap here.
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { PhoneCheck.load(context) }

    val categories = remember {
        ModelCategory.entries.filter { ModelCatalog.byCategory(it).isNotEmpty() }
    }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedCategory = categories.getOrElse(tabIndex) { categories.first() }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 12.dp) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(category.label) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PhoneCheckCard(
                        onRun = { scope.launch { PhoneCheck.run(context) } },
                    )
                }
                val models = remember(selectedCategory, PhoneCheck.result) {
                    val base = ModelCatalog.byCategory(selectedCategory)
                    if (PhoneCheck.result == null) {
                        base
                    } else {
                        base.sortedWith(
                            compareBy({ PhoneCheck.fitRank(it.ramMb) }, { it.ramMb ?: Int.MAX_VALUE }),
                        )
                    }
                }
                items(models, key = { it.id }) { info ->
                    ModelCard(info, downloads[info.id], loadedPath, loading, vm)
                }
                if (selectedCategory == ModelCategory.CHAT) {
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
    }
}

@Composable
private fun ReadinessBadge(readiness: Readiness) {
    val (bg, fg) = when (readiness) {
        Readiness.READY ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Readiness.NEEDS_RUNTIME ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        Readiness.NOT_YET ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            readiness.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    info.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                ReadinessBadge(info.readiness)
                PhoneCheck.fitFor(info.ramMb)?.let { fit ->
                    val (bg, fg) = when (fit) {
                        PhoneCheck.Fit.GOOD ->
                            MaterialTheme.colorScheme.primaryContainer to
                                MaterialTheme.colorScheme.onPrimaryContainer
                        PhoneCheck.Fit.TIGHT ->
                            MaterialTheme.colorScheme.tertiaryContainer to
                                MaterialTheme.colorScheme.onTertiaryContainer
                        PhoneCheck.Fit.TOO_BIG ->
                            MaterialTheme.colorScheme.errorContainer to
                                MaterialTheme.colorScheme.onErrorContainer
                    }
                    Surface(color = bg, shape = MaterialTheme.shapes.small) {
                        Text(
                            fit.label,
                            color = fg,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Text(info.summary, style = MaterialTheme.typography.bodySmall)
            val ram = info.ramMb?.let {
                if (it >= 1000) "  ·  RAM: about %.1f GB".format(it / 1000f)
                else "  ·  RAM: about $it MB"
            } ?: ""
            val download = if (info.mmproj != null) {
                "Size: ${formatSize(info.sizeBytes + info.mmproj.sizeBytes)} in 2 files"
            } else {
                "Size: ${formatSize(info.sizeBytes)}"
            }
            Text(
                "$download$ram  ·  License: ${info.license}",
                style = MaterialTheme.typography.bodySmall,
            )
            CostLines(info)
            when {
                !info.downloadable -> {
                    Text(
                        "Not downloadable in this build.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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

@Composable
private fun CostLines(info: ModelInfo) {
    val context = LocalContext.current
    ModelStats.version // recompose when a measurement lands
    val measured = ModelStats.get(context, info.id)
    if (measured != null) {
        Text(
            "Measured on this phone: %.1f tok/s over %d replies".format(
                measured.tokPerSec, measured.replies,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        val check = PhoneCheck.result ?: return
        val ramGb = (info.ramMb ?: return).toDouble() / 1000.0
        // Rough estimate for a 1 GB model on each tier, scaled by model size.
        val base = when (check.tier) {
            PhoneCheck.Tier.ENTRY -> 2.0
            PhoneCheck.Tier.MID -> 5.0
            PhoneCheck.Tier.FLAGSHIP -> 12.0
        }
        val est = base * 1.0 / ramGb.coerceAtLeast(0.5)
        val lo = est * 0.5
        val hi = est * 1.5
        // Battery: about 4 W while generating; energy for a 100-token reply.
        val midTps = (lo + hi) / 2
        val wh = 4.0 * (100.0 / midTps) / 3600.0
        Text(
            "Estimated for this phone: %.1f-%.1f tok/s, about %.3f Wh per " +
                "100-token reply".format(lo, hi, wh),
            style = MaterialTheme.typography.bodySmall,
        )
        if (check.tier == PhoneCheck.Tier.ENTRY ||
            PhoneCheck.fitFor(info.ramMb) == PhoneCheck.Fit.TIGHT
        ) {
            Text(
                "Long sessions can warm the phone and slow it down.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PhoneCheckCard(onRun: () -> Unit) {
    val result = PhoneCheck.result
    val running = PhoneCheck.running
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Check this phone",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    TextButton(onClick = onRun) {
                        Text(if (result == null) "Run" else "Re-check")
                    }
                }
            }
            if (result == null) {
                Text(
                    "Measures this phone (about 2 seconds, fully on-device) and " +
                        "sorts models by how well they fit.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "${result.tier.label} phone  ·  ${result.cores} cores  ·  " +
                        "RAM %.1f GB".format(result.totalRamBytes / 1e9),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Measured: memory %.1f GB/s, compute %.0f GFLOPS".format(
                        result.memoryCopyGBs, result.fpGFLOPS,
                    ) + if (result.soc.isNotBlank()) "  ·  Chip: ${result.soc}" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Models are sorted by fit: Runs well first, Too big last.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1_000_000_000) "%.2f GB".format(bytes / 1e9)
    else "%.0f MB".format(bytes / 1e6)
