package dev.aicon.studio.ui.chat

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aicon.studio.EngineHolder
import dev.aicon.studio.agent.AgentRunner
import dev.aicon.studio.agent.AuditStore
import dev.aicon.studio.agent.FileGateway
import dev.aicon.studio.chat.ChatSessionMeta
import dev.aicon.studio.chat.ChatStore
import dev.aicon.studio.chat.StoredMessage
import dev.aicon.studio.engine.ChatTurn
import dev.aicon.studio.engine.EngineException
import dev.aicon.studio.model.ModelStats
import dev.aicon.studio.engine.Role
import dev.aicon.studio.engine.agent.ToolCallParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A file the user picked to send with the next message. */
data class PendingAttachment(val name: String, val text: String)

/** A reply the user chose to save through the system file picker. */
data class ExportRequest(val suggestedName: String, val text: String)

/** A tool call waiting for the user's decision. */
data class ToolApproval(val id: Long, val tool: String, val detail: String)

/**
 * Holds the conversation and drives the shared engine. Sessions persist as
 * JSON files through ChatStore; the engine itself lives in EngineHolder so
 * the Models screen can load and unload too. Agent mode routes prompts
 * through AgentRunner, where every tool call waits for approval. When a
 * vision model is loaded, one photo can ride along with a message.
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ChatStore(app.applicationContext)
    private val runner = AgentRunner(
        EngineHolder.engine(app.applicationContext),
        FileGateway(app.applicationContext),
        AuditStore(app.applicationContext),
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val modelPath: StateFlow<String?> = EngineHolder.loadedModelPath
    val visionReady: StateFlow<Boolean> = EngineHolder.visionReady

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Live speed while a reply streams, e.g. "8.3 tok/s · 340 words/min". */
    private val _liveSpeed = MutableStateFlow<String?>(null)
    val liveSpeed: StateFlow<String?> = _liveSpeed.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSessionMeta>>(emptyList())
    val sessions: StateFlow<List<ChatSessionMeta>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSessionMeta?>(null)
    val currentSession: StateFlow<ChatSessionMeta?> = _currentSession.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<PendingAttachment?>(null)
    val pendingAttachment: StateFlow<PendingAttachment?> = _pendingAttachment.asStateFlow()

    private val _pendingImage = MutableStateFlow<String?>(null)
    val pendingImage: StateFlow<String?> = _pendingImage.asStateFlow()

    private val _exportRequest = MutableStateFlow<ExportRequest?>(null)
    val exportRequest: StateFlow<ExportRequest?> = _exportRequest.asStateFlow()

    private val _agentMode = MutableStateFlow(false)
    val agentMode: StateFlow<Boolean> = _agentMode.asStateFlow()

    private val _approval = MutableStateFlow<ToolApproval?>(null)
    val approval: StateFlow<ToolApproval?> = _approval.asStateFlow()

    private var approvalDeferred: CompletableDeferred<Boolean>? = null

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
        _pendingImage.value = null
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

    /** Stage an image (copied into app storage by the caller) for the next message. */
    fun setPendingImage(path: String) {
        if (!EngineHolder.hasVision()) {
            _error.value = "This model cannot see images. Load a vision model from the Models screen."
            return
        }
        _pendingImage.value = path
    }

    fun clearPendingImage() {
        _pendingImage.value = null
    }

    fun setAgentMode(on: Boolean) {
        _agentMode.value = on
    }

    fun decideApproval(allow: Boolean) {
        approvalDeferred?.complete(allow)
        approvalDeferred = null
        _approval.value = null
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
        _pendingAttachment.value = null
        val image = _pendingImage.value
        _pendingImage.value = null
        if (image != null) {
            if (_agentMode.value) {
                _error.value = "Agent mode cannot use images yet. Turn Agent off to send a photo."
                return
            }
            if (!EngineHolder.hasVision()) {
                _error.value = "This model cannot see images. Load a vision model from the Models screen."
                return
            }
        }
        if (_agentMode.value) {
            sendAgent(prompt, attachment)
        } else {
            sendChat(prompt, attachment, image)
        }
    }

    private fun addUserMessage(prompt: String, attachment: PendingAttachment?, imagePath: String? = null) {
        _messages.update {
            it + ChatMessage(
                nextId++,
                fromUser = true,
                text = prompt,
                attachmentName = attachment?.name ?: imagePath?.substringAfterLast('/'),
                imagePath = imagePath,
            )
        }
    }

    private fun enginePrompt(prompt: String, attachment: PendingAttachment?): String =
        if (attachment != null) {
            "File \"${attachment.name}\":\n" + attachment.text + "\n\n" + prompt
        } else {
            prompt
        }

    private fun sendChat(prompt: String, attachment: PendingAttachment?, imagePath: String? = null) {
        addUserMessage(prompt, attachment, imagePath)
        val replyId = nextId++
        _messages.update { it + ChatMessage(replyId, fromUser = false, text = "", streaming = true) }

        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            _liveSpeed.value = null
            var words = 0
            val startedAt = SystemClock.elapsedRealtime()
            val history = _messages.value
                .filter { !it.streaming }
                .map { ChatTurn(if (it.fromUser) Role.USER else Role.ASSISTANT, it.text) }
            try {
                val engine = EngineHolder.engine(getApplication())
                engine
                    .generate(enginePrompt(prompt, attachment), history, imagePath)
                    .collect { token ->
                        words += token.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
                        _messages.update { list ->
                            list.map { if (it.id == replyId) it.copy(text = it.text + token) else it }
                        }
                        val elapsedSec = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
                        val tps = engine.stats().decodeTokPerSec
                        if (tps > 0 && elapsedSec > 0.5 && words > 0) {
                            _liveSpeed.value = speedText(tps, words, elapsedSec)
                        }
                    }
                val elapsedSec = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
                val stats = engine.stats()
                val caption = when {
                    stats.generatedTokens <= 0 -> null
                    stats.decodeTokPerSec > 0 && words > 0 ->
                        speedText(stats.decodeTokPerSec, words, elapsedSec) +
                            " · " + stats.generatedTokens + " tokens"
                    else -> stats.generatedTokens.toString() + " tokens"
                }
                _messages.update { list ->
                    list.map { if (it.id == replyId) it.copy(streaming = false, speedText = caption) else it }
                }
                EngineHolder.loadedModelPath.value
                    ?.let { Regex("/models/([^/]+)/").find(it)?.groupValues?.get(1) }
                    ?.let { modelId ->
                        ModelStats.record(
                            getApplication(),
                            modelId,
                            stats.decodeTokPerSec,
                            stats.generatedTokens,
                        )
                    }
                persist()
            } catch (e: EngineException) {
                _error.value = e.message
                _messages.update { list -> list.filterNot { it.id == replyId } }
            } finally {
                _busy.value = false
                _liveSpeed.value = null
            }
        }
    }

    private fun sendAgent(prompt: String, attachment: PendingAttachment?) {
        addUserMessage(prompt, attachment)
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                runner.run(
                    goal = enginePrompt(prompt, attachment),
                    requestApproval = { call -> awaitApproval(call) },
                    onEvent = { event -> onAgentEvent(event) },
                )
                persist()
            } catch (e: EngineException) {
                _error.value = e.message
            } finally {
                _busy.value = false
                _approval.value = null
                approvalDeferred = null
            }
        }
    }

    private suspend fun awaitApproval(call: ToolCallParser.Call): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        approvalDeferred = deferred
        _approval.value = ToolApproval(nextId++, call.name, AgentRunner.describe(call))
        return deferred.await()
    }

    private fun onAgentEvent(event: AgentRunner.Event) {
        when (event) {
            is AgentRunner.Event.Reply ->
                _messages.update { it + ChatMessage(nextId++, fromUser = false, text = event.text) }
            is AgentRunner.Event.ToolDone ->
                _messages.update {
                    it + ChatMessage(
                        nextId++,
                        fromUser = false,
                        text = "Agent: ${event.detail} (${event.status})",
                    )
                }
            AgentRunner.Event.RoundLimit ->
                _messages.update {
                    it + ChatMessage(
                        nextId++,
                        fromUser = false,
                        text = "Agent stopped after the step limit. Ask it to continue if needed.",
                    )
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun speedText(tps: Double, words: Int, elapsedSec: Double): String {
        val wpm = if (elapsedSec > 0) words / elapsedSec * 60 else 0.0
        return String.format("%.1f tok/s · %.0f words/min", tps, wpm)
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
