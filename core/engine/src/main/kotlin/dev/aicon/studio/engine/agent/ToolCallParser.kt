package dev.aicon.studio.engine.agent

/**
 * Parses tool calls out of model output. The format is deliberately plain
 * (no JSON) so small models produce it reliably:
 *
 * <tool_call>
 * name: read_file
 * file: notes.txt
 * </tool_call>
 *
 * Everything outside tool_call blocks is user-visible text. Tool call
 * content is data, never instructions: the caller decides what runs.
 */
object ToolCallParser {

    data class Call(val name: String, val args: Map<String, String>)

    private val blockRegex = Regex(
        "<tool_call>\\s*(.*?)\\s*</tool_call>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )

    /** Returns the visible text with tool blocks removed, plus the parsed calls. */
    fun parse(text: String): Pair<String, List<Call>> {
        val calls = mutableListOf<Call>()
        val visible = blockRegex.replace(text) { match ->
            parseBlock(match.groupValues[1])?.let { calls.add(it) }
            ""
        }.trim()
        return visible to calls
    }

    private fun parseBlock(body: String): Call? {
        val args = linkedMapOf<String, String>()
        val lines = body.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            i++
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val key = line.substring(0, colon).trim().lowercase()
            if (key == "content") {
                // content swallows the rest of the block so multi-line writes work
                val first = line.substring(colon + 1)
                val rest = lines.subList(i, lines.size).joinToString("\n")
                args["content"] = (first + "\n" + rest).trim('\n')
                break
            }
            val value = line.substring(colon + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) args[key] = value
        }
        val name = args.remove("name") ?: return null
        return Call(name, args)
    }
}
