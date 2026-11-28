package dev.aicon.studio.engine.agent

import java.security.MessageDigest

/**
 * Builds audit log lines for agent tool use. By design a line carries only
 * the tool name, the outcome and a short hash of the path it touched. Tool
 * arguments and file contents never enter the log: anything the model or a
 * file said stays out of the record.
 */
object AuditLog {

    /**
     * One JSON line: {"tool":..,"status":..,"path_hash":..}. [path] is
     * hashed with SHA-256 and truncated; it is never written directly.
     */
    fun line(tool: String, status: String, path: String?): String {
        val build = StringBuilder("{")
        build.append("\"tool\":").append(jsonString(tool))
        build.append(",\"status\":").append(jsonString(status))
        if (path != null) {
            build.append(",\"path_hash\":").append(jsonString(pathHash(path)))
        }
        return build.append("}").toString()
    }

    fun pathHash(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun jsonString(value: String): String {
        val out = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> out.append(c)
            }
        }
        return out.append("\"").toString()
    }
}
