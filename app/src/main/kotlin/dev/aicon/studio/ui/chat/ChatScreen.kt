package dev.aicon.studio.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val MAX_ATTACH_CHARS = 8000

/**
 * The chat screen. Original AiCoN AI Studio design: iris accents, rounded
 * bubbles, sessions in the drawer, model management on the Models screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {},
    onOpenModels: () -> Unit = {},
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val modelPath by vm.modelPath.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val pendingAttachment by vm.pendingAttachment.collectAsStateWithLifecycle()
    val exportRequest by vm.exportRequest.collectAsStateWithLifecycle()
    val agentMode by vm.agentMode.collectAsStateWithLifecycle()
    val approval by vm.approval.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Explicit file pick only: the SAF picker is the consent step.
    val attachPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val name = queryDisplayName(context.contentResolver, uri) ?: "file"
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val buf = CharArray(MAX_ATTACH_CHARS)
                    var off = 0
                    while (off < buf.size) {
                        val n = reader.read(buf, off, buf.size - off)
                        if (n < 0) break
                        off += n
                    }
                    String(buf, 0, off)
                }
                if (!text.isNullOrEmpty()) vm.setAttachment(name, text)
            }
        }
    }

    // Export goes through the system picker, so the user chooses where.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val req = vm.exportRequest.value
        if (uri != null && req != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(req.text.toByteArray()) }
            }
        }
        vm.clearExport()
    }

    LaunchedEffect(exportRequest) {
        exportRequest?.let { exportLauncher.launch(it.suggestedName) }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = { Text("AiCoN AI Studio") },
                actions = {
                    TextButton(onClick = { vm.setAgentMode(!agentMode) }) {
                        Text(
                            "Agent",
                            color = if (agentMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (modelPath == null) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("No model loaded", modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenModels) { Text("Open Models") }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    Bubble(
                        msg,
                        onSave = if (!msg.fromUser && !msg.streaming && msg.text.isNotBlank()) {
                            { vm.requestExport(msg.id) }
                        } else {
                            null
                        },
                    )
                }
            }
            approval?.let { pending ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Agent wants to ${pending.detail}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { vm.decideApproval(false) }) { Text("Deny") }
                        TextButton(onClick = { vm.decideApproval(true) }) { Text("Approve") }
                    }
                }
            }
            pendingAttachment?.let { attachment ->
                AssistChip(
                    onClick = { vm.clearAttachment() },
                    label = { Text("Attached: ${attachment.name}") },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = { attachPicker.launch(arrayOf("text/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = "Attach a file")
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4,
                )
                Button(
                    onClick = {
                        vm.send(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && !busy,
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage, onSave: (() -> Unit)?) {
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (msg.fromUser) 18.dp else 4.dp,
        bottomEnd = if (msg.fromUser) 4.dp else 18.dp,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start,
        ) {
            Surface(
                color = if (msg.fromUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                shape = shape,
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    msg.attachmentName?.let {
                        Text(
                            text = "Attached: $it",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = if (msg.streaming && msg.text.isEmpty()) "…" else msg.text,
                    )
                }
            }
        }
        if (onSave != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onSave) { Text("Save to file") }
            }
        }
    }
}

private fun queryDisplayName(resolver: android.content.ContentResolver, uri: android.net.Uri): String? =
    runCatching {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
