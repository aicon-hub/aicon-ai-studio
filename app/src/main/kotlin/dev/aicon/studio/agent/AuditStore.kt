package dev.aicon.studio.agent

import android.content.Context
import java.io.File

/**
 * Appends agent audit lines to a small rolling log on device. Lines are
 * built by AuditLog in core:engine, which never writes arguments or file
 * contents, only tool, status and a path hash.
 */
class AuditStore(context: Context) {

    private val file: File = File(context.filesDir, "audit.log")

    @Synchronized
    fun append(line: String) {
        runCatching {
            file.appendText(line + "\n")
            trimIfNeeded()
        }
    }

    fun tail(count: Int): List<String> =
        runCatching { file.readLines().takeLast(count) }.getOrDefault(emptyList())

    private fun trimIfNeeded() {
        if (file.length() > MAX_BYTES) {
            val lines = file.readLines().takeLast(KEEP_LINES)
            file.writeText(lines.joinToString("\n") + "\n")
        }
    }

    private companion object {
        const val MAX_BYTES = 256 * 1024
        const val KEEP_LINES = 500
    }
}
