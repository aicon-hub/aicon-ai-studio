package dev.aicon.studio.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aicon.studio.engine.ChatTurn
import dev.aicon.studio.engine.EngineException
import dev.aicon.studio.engine.InferenceEngine
import dev.aicon.studio.engine.Role
import dev.aicon.studio.engine.llamacpp.LlamaCppEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the conversation and drives the engine. UI state only; no storage
 * yet (sessions land with Room in a later phase).
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    // Built inside the class: a default constructor argument hides the
    // single-Application constructor that AndroidViewModelFactory looks up.
    private val engine: InferenceEngine = LlamaCppEngine(app.applicationContext)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _modelPath = MutableStateFlow<String?>(null)
    val modelPath: StateFlow<String?> = _modelPath.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private var nextId = 1L

    /** User picked a model file; try to load it. */
    fun loadModel(path: String) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                engine.load(path)
                _modelPath.value = path
            } catch (e: EngineException) {
                _error.value = e.message
            } finally {
                _busy.value = false
            }
        }
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _busy.value) return
        if (_modelPath.value == null) {
            _error.value = "Pick a model file first."
            return
        }
        val userMsg = ChatMessage(nextId++, fromUser = true, text = prompt)
        val replyId = nextId++
        _messages.update { it + userMsg + ChatMessage(replyId, fromUser = false, text = "", streaming = true) }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            val history = _messages.value
                .filter { !it.streaming }
                .map { ChatTurn(if (it.fromUser) Role.USER else Role.ASSISTANT, it.text) }
            try {
                engine.generate(prompt, history).collect { token ->
                    _messages.update { list ->
                        list.map { if (it.id == replyId) it.copy(text = it.text + token) else it }
                    }
                }
                _messages.update { list ->
                    list.map { if (it.id == replyId) it.copy(streaming = false) else it }
                }
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
}
