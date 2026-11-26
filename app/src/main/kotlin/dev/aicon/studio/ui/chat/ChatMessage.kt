package dev.aicon.studio.ui.chat

/** One visible message in the chat list. */
data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val streaming: Boolean = false,
)
