package dev.aicon.studio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aicon.studio.savings.Savings
import dev.aicon.studio.settings.AppSettings

/** Appearance and about. Changes apply immediately and persist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                AppSettings.ThemeMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = AppSettings.themeMode == mode,
                            onClick = { AppSettings.setThemeMode(context, mode) },
                        )
                        Text(
                            when (mode) {
                                AppSettings.ThemeMode.SYSTEM -> "Follow system"
                                AppSettings.ThemeMode.LIGHT -> "Light"
                                AppSettings.ThemeMode.DARK -> "Dark"
                            },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accent", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppSettings.accentChoices.forEach { argb ->
                        val selected = AppSettings.accentArgb == argb
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { AppSettings.setAccent(context, argb) },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Speed", style = MaterialTheme.typography.titleMedium)
                Text("Threads", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Auto", 2 to "2", 4 to "4", 6 to "6", 8 to "8").forEach { (value, label) ->
                        FilterChip(
                            selected = AppSettings.perfThreads == value,
                            onClick = { AppSettings.setPerfThreads(context, value) },
                            label = { Text(label) },
                        )
                    }
                }
                Text("Context size", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSettings.contextSizeChoices.forEach { size ->
                        FilterChip(
                            selected = AppSettings.perfContextSize == size,
                            onClick = { AppSettings.setPerfContextSize(context, size) },
                            label = { Text(size.toString()) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quantized KV cache (q8_0)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Faster on long chats, small quality cost",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = AppSettings.perfKvQ8,
                        onCheckedChange = { AppSettings.setPerfKvQ8(context, it) },
                    )
                }
                Text(
                    "Applies the next time a model loads.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Impact tracking", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Count on-device tokens, streaks and estimated savings",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Fully local, estimates only. Off stops counting; totals stay until reset.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = Savings.enabled,
                        onCheckedChange = { Savings.setEnabled(context, it) },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("AiCoN AI Studio $version", style = MaterialTheme.typography.bodyMedium)
                Text("Engine: llama.cpp v1.1.0, fully on-device", style = MaterialTheme.typography.bodySmall)
                Text("Open source under Apache-2.0", style = MaterialTheme.typography.bodySmall)
                Text("github.com/aicon-hub/aicon-ai-studio", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

