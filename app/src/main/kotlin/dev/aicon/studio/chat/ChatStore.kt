package dev.aicon.studio.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Session metadata shown in the drawer list. */
data class ChatSessionMeta(val id: String, val title: String, val updatedAt: Long)

/** One persisted message: fromUser, text, optional attachment label. */
data class StoredMessage(val fromUser: Boolean, val text: String, val attachment: String?)

/**
 * Saves chat sessions as small JSON files under the app's private storage.
 * No account, no sync: the files never leave the phone.
 */
class ChatStore(context: Context) {

    private val dir: File = File(context.filesDir, "chats")

    fun listSessions(): List<ChatSessionMeta> {
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { readMeta(it) }.sortedByDescending { it.updatedAt }
    }

    fun newSessionMeta(): ChatSessionMeta =
        ChatSessionMeta("c" + System.currentTimeMillis().toString(36), "New chat", System.currentTimeMillis())

    fun load(id: String): List<StoredMessage> {
        val file = File(dir, "$id.json")
        if (!file.isFile) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText())
            val arr = root.getJSONArray("messages")
            (0 until arr.length()).map { i ->
                val m = arr.getJSONObject(i)
                StoredMessage(
                    fromUser = m.getBoolean("u"),
                    text = m.getString("t"),
                    attachment = if (m.has("a")) m.getString("a") else null,
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(meta: ChatSessionMeta, messages: List<StoredMessage>) {
        dir.mkdirs()
        val arr = JSONArray()
        messages.forEach { m ->
            val entry = JSONObject().put("u", m.fromUser).put("t", m.text)
            m.attachment?.let { entry.put("a", it) }
            arr.put(entry)
        }
        val root = JSONObject()
            .put("id", meta.id)
            .put("title", meta.title)
            .put("updatedAt", meta.updatedAt)
        root.put("messages", arr)
        File(dir, "${meta.id}.json").writeText(root.toString())
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }

    private fun readMeta(file: File): ChatSessionMeta? = runCatching {
        val root = JSONObject(file.readText())
        ChatSessionMeta(
            id = root.getString("id"),
            title = root.getString("title"),
            updatedAt = root.getLong("updatedAt"),
        )
    }.getOrNull()
}
