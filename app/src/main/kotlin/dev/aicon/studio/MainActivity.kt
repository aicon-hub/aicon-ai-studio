package dev.aicon.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.aicon.studio.ui.chat.ChatScreen
import dev.aicon.studio.ui.theme.AiCoNStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiCoNStudioTheme {
                ChatScreen()
            }
        }
    }
}
