package dev.aicon.studio.engine.agent

/**
 * Decides which file names the agent may touch. The agent only ever sees
 * names inside the user's approved share folder; anything that could walk
 * out of it (slashes, parent segments, absolute paths, odd characters) is
 * rejected here, before any file API runs.
 */
object PathPolicy {

    private val SAFE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._ -]{0,99}")

    fun isAllowed(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        if (name != name.trim()) return false
        if (name.contains('/') || name.contains('\\')) return false
        if (name.contains("..")) return false
        return SAFE_NAME.matches(name)
    }

    /** Normalize a model-provided name to a candidate, or null if unusable. */
    fun normalize(name: String?): String? = name?.trim()?.takeIf { isAllowed(it) }
}
