package dev.aicon.studio.agent

import android.content.Context
import dev.aicon.studio.engine.agent.PathPolicy
import java.io.File
import java.io.InputStream

/**
 * The only files the agent can touch: a share folder the user fills by
 * picking files, one by one, through the system picker. Every name goes
 * through PathPolicy before any file API runs.
 */
class FileGateway(context: Context) {

    private val dir: File = File(context.filesDir, "shared")

    fun list(): List<String> =
        dir.listFiles()?.filter { it.isFile }?.map { it.name }?.sorted() ?: emptyList()

    fun sizeOf(name: String): Long? = resolve(name)?.length()

    /** Copy a user-picked stream into the share folder. */
    fun add(source: InputStream, name: String): Boolean {
        val safe = PathPolicy.normalize(name) ?: return false
        dir.mkdirs()
        return runCatching {
            File(dir, safe).outputStream().use { out -> source.copyTo(out) }
        }.isSuccess
    }

    fun read(name: String, maxChars: Int = 12_000): String? {
        val file = resolve(name) ?: return null
        return runCatching {
            file.bufferedReader().use { reader ->
                val buf = CharArray(maxChars)
                var off = 0
                while (off < buf.size) {
                    val n = reader.read(buf, off, buf.size - off)
                    if (n < 0) break
                    off += n
                }
                String(buf, 0, off)
            }
        }.getOrNull()
    }

    enum class WriteResult { CREATED, OVERWRITTEN, REJECTED }

    fun write(name: String, content: String): WriteResult {
        val safe = PathPolicy.normalize(name) ?: return WriteResult.REJECTED
        dir.mkdirs()
        val file = File(dir, safe)
        val existed = file.exists()
        return runCatching {
            file.writeText(content)
            if (existed) WriteResult.OVERWRITTEN else WriteResult.CREATED
        }.getOrDefault(WriteResult.REJECTED)
    }

    fun delete(name: String): Boolean = resolve(name)?.delete() == true

    fun resolve(name: String): File? {
        val safe = PathPolicy.normalize(name) ?: return null
        return File(dir, safe)
    }
}
