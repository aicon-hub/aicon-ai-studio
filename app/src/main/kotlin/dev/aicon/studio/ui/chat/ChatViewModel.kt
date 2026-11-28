package dev.aicon.studio.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aicon.studio.EngineHolder
import dev.aicon.studio.chat.ChatSessionMeta
import dev.aicon.studio.chat.ChatStore
import dev.aicon.studio.chat.StoredMessage
import dev.aicon.studio.engine.ChatTurn
import dev.aicon.studio.engine.EngineException
import dev.aicon.studio.engine.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A file the user picked to send with the next message. */
data class PendingAttachment(val name: String, val text: String)

/** A reply the user chose to save through the system file picker. */
data class ExportRequest(val suggestedName: String, val text: String)

/**
 * Holds the conversation and drives the shared engine. Sessions persist as
 * JSON files through ChatStore; the engine itself lives in EngineHolder so
 * the Models screen can load and unload too.
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ChatStore(app.applicationContext)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val modelPath: StateFlow<String?> = EngineHolder.loadedModelPath

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSessionMeta>>(emptyList())
    val sessions: StateFlow<List<ChatSessionMeta>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSessionMeta?>(null)
    val currentSession: StateFlow<ChatSessionMeta?> = _currentSession.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<PendingAttachment?>(null)
    val pendingAttachment: StateFlow<PendingAttachment?> = _pendingAttachment.asStateFlow()

    private val _exportRequest = MutableStateFlow<ExportRequest?>(null)
    val exportRequest: StateFlow<ExportRequest?> = _exportRequest.asStateFlow()

    private var nextId = 1L

    init {
        refreshSessions()
        val latest = _sessions.value.firstOrNull()
        if (latest != null) openSession(latest.id) else newChat()
    }

    fun refreshSessions() {
        _sessions.value = store.listSessions()
    }

    fun newChat() {
        _currentSession.value = store.newSessionMeta()
        _messages.value = emptyList()
        _pendingAttachment.value = null
    }

    fun openSession(id: String) {
        val meta = _sessions.value.firstOrNull { it.id == id } ?: return
        _currentSession.value = meta
        _messages.value = store.load(id).map {
            ChatMessage(nextId++, fromUser = it.fromUser, text = it.text, attachmentName = it.attachment)
        }
    }

    fun deleteSession(id: String) {
        store.delete(id)
        if (_currentSession.value?.id == id) newChat()
        refreshSessions()
    }

    /** Load a model file directly (used by the storage import flow). */
    fun loadModel(path: String) {
        viewModelScope.launch {
            EngineHolder.loadModel(getApplication(), path, null)
        }
    }

    fun setAttachment(name: String, text: String) {
        _pendingAttachment.value = PendingAttachment(name, text)
    }

    fun clearAttachment() {
        _pendingAttachment.value = null
    }

    fun requestExport(messageId: Long) {
        val msg = _messages.value.firstOrNull { it.id == messageId } ?: return
        if (msg.fromUser || msg.streaming || msg.text.isBlank()) return
        _exportRequest.value = ExportRequest("aicon-reply-${msg.id}.txt", msg.text)
    }

    fun clearExport() {
        _exportRequest.value = null
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _busy.value) return
        if (!EngineHolder.isLoaded()) {
            _error.value = "Load a model first from the Models screen."
            return
        }
        val attachment = _pendingAttachment.value
        val userMsg = ChatMessage(
            nextId++, fromUser = true, text = prompt, attachmentName = attachment?.name,
        )
        val replyId = nextId++
        _messages.update { it + userMsg + ChatMessage(replyId, fromUser = false, text = "", streaming = true) }
        _pendingAttachment.value = null

        val enginePrompt = if (attachment != null) {
            "File \"${attachment.name}\":\n" + attachment.text + "\n\n" + prompt
        } else {
            prompt
        }

        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            val history = _messages.value
                .filter { !it.streaming }
                .map { ChatTurn(if (it.fromUser) Role.USER else Role.ASSISTANT, it.text) }
            try {
                EngineHolder.engine(getApplication()).generate(enginePrompt, history).collect { token ->
                    _messages.update { list ->
                        list.map { if (it.id == replyId) it.copy(text = it.text + token) else it }
                    }
                }
                _messages.update { list ->
                    list.map { if (it.id == replyId) it.copy(streaming = false) else it }
                }
                persist()
            } catch (e: EngineException) {
                _error.value = e.message
                _messages.update { list -> list.filterNot { it.id == replyId } }
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun persist() {
        val meta = _currentSession.value ?: return
        val done = _messages.value.filter { !it.streaming }
        if (done.isEmpty()) return
        val title = if (meta.title == "New chat") {
            done.firstOrNull { it.fromUser }?.text?.trim()?.take(40)?.ifEmpty { null } ?: meta.title
        } else {
            meta.title
        }
        val updated = meta.copy(title = title, updatedAt = System.currentTimeMillis())
        _currentSession.value = updated
        store.save(updated, done.map { StoredMessage(it.fromUser, it.text, it.attachmentName) })
        refreshSessions()
    }
}
