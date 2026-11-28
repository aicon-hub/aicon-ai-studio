package dev.aicon.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aicon.studio.model.ModelManager
import dev.aicon.studio.settings.AppSettings
import dev.aicon.studio.ui.chat.ChatScreen
import dev.aicon.studio.ui.chat.ChatViewModel
import dev.aicon.studio.ui.files.FilesScreen
import dev.aicon.studio.ui.models.ModelsScreen
import dev.aicon.studio.ui.settings.SettingsScreen
import dev.aicon.studio.ui.theme.AiCoNStudioTheme
import kotlinx.coroutines.launch

private enum class AppScreen { CHAT, MODELS, FILES, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)
        ModelManager.init(this)
        enableEdgeToEdge()
        setContent {
            val dark = when (AppSettings.themeMode) {
                AppSettings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppSettings.ThemeMode.LIGHT -> false
                AppSettings.ThemeMode.DARK -> true
            }
            AiCoNStudioTheme(darkTheme = dark, accent = Color(AppSettings.accentArgb)) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    var screenName by rememberSaveable { mutableStateOf(AppScreen.CHAT.name) }
    val screen = AppScreen.valueOf(screenName)
    val chatVm: ChatViewModel = viewModel()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    chatVm = chatVm,
                    onOpenModels = {
                        screenName = AppScreen.MODELS.name
                        closeDrawer()
                    },
                    onOpenFiles = {
                        screenName = AppScreen.FILES.name
                        closeDrawer()
                    },
                    onOpenSettings = {
                        screenName = AppScreen.SETTINGS.name
                        closeDrawer()
                    },
                    onClose = closeDrawer,
                )
            }
        },
    ) {
        when (screen) {
            AppScreen.CHAT -> ChatScreen(
                vm = chatVm,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenModels = { screenName = AppScreen.MODELS.name },
            )
            AppScreen.MODELS -> ModelsScreen(onBack = { screenName = AppScreen.CHAT.name })
            AppScreen.FILES -> FilesScreen(onBack = { screenName = AppScreen.CHAT.name })
            AppScreen.SETTINGS -> SettingsScreen(onBack = { screenName = AppScreen.CHAT.name })
        }
    }
}

@Composable
private fun DrawerContent(
    chatVm: ChatViewModel,
    onOpenModels: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val sessions by chatVm.sessions.collectAsStateWithLifecycle()
    val current by chatVm.currentSession.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Chats",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            chatVm.newChat()
            onClose()
        }) { Text("New chat") }
    }
    HorizontalDivider()
    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
        items(sessions, key = { it.id }) { session ->
            NavigationDrawerItem(
                label = {
                    Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                badge = {
                    IconButton(onClick = { chatVm.deleteSession(session.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete chat")
                    }
                },
                selected = session.id == current?.id,
                onClick = {
                    chatVm.openSession(session.id)
                    onClose()
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
    HorizontalDivider()
    NavigationDrawerItem(
        label = { Text("Models") },
        selected = false,
        onClick = onOpenModels,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
    NavigationDrawerItem(
        label = { Text("Files") },
        selected = false,
        onClick = onOpenFiles,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
    NavigationDrawerItem(
        label = { Text("Settings") },
        selected = false,
        onClick = onOpenSettings,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
