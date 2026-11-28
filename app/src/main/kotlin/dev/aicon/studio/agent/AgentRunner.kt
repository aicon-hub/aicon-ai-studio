package dev.aicon.studio.agent

import dev.aicon.studio.engine.InferenceEngine
import dev.aicon.studio.engine.agent.AuditLog
import dev.aicon.studio.engine.agent.PathPolicy
import dev.aicon.studio.engine.agent.ToolCallParser

/**
 * The bounded agent loop. The model may ask for three tools: list_files,
 * read_file and write_file, all confined to the user's share folder by
 * FileGateway. Every tool call pauses for the user's approval, and every
 * decision lands in the audit log as tool, status and path hash only.
 *
 * Tool output is data, never new instructions: results go back to the model
 * as plain text, and only the parsed tool block can start an action.
 */
class AgentRunner(
    private val engine: InferenceEngine,
    private val gateway: FileGateway,
    private val audit: AuditStore,
) {

    sealed interface Event {
        /** A finished, user-visible chunk of the model's reply for one round. */
        data class Reply(val text: String) : Event

        /** A tool finished; status is approved, denied, rejected_path or rejected_unknown. */
        data class ToolDone(val tool: String, val detail: String, val status: String) : Event

        /** The loop stopped after the round cap without a final answer. */
        data object RoundLimit : Event
    }

    suspend fun run(
        goal: String,
        requestApproval: suspend (ToolCallParser.Call) -> Boolean,
        onEvent: (Event) -> Unit,
        maxRounds: Int = 3,
    ) {
        var prompt = AGENT_PREAMBLE + goal
        var round = 0
        while (round < maxRounds) {
            round++
            val reply = StringBuilder()
            engine.generate(prompt, emptyList()).collect { piece -> reply.append(piece) }
            val (visible, calls) = ToolCallParser.parse(reply.toString())
            if (visible.isNotBlank()) onEvent(Event.Reply(visible))
            if (calls.isEmpty()) return

            val results = StringBuilder()
            for (call in calls) {
                if (call.name !in KNOWN_TOOLS) {
                    audit.append(AuditLog.line(call.name, "rejected_unknown", null))
                    results.append("Tool result for ").append(call.name)
                        .append(": unknown tool. Only list_files, read_file and write_file exist.\n")
                    continue
                }
                val approved = requestApproval(call)
                if (!approved) {
                    audit.append(AuditLog.line(call.name, "denied", call.args["file"]))
                    onEvent(Event.ToolDone(call.name, describe(call), "denied"))
                    results.append("Tool result for ").append(call.name)
                        .append(": the user denied this action.\n")
                    continue
                }
                val outcome = execute(call)
                onEvent(Event.ToolDone(call.name, describe(call), outcome.status))
                results.append("Tool result for ").append(call.name).append(":\n")
                    .append(outcome.message).append('\n')
            }
            prompt = results.toString()
        }
        onEvent(Event.RoundLimit)
    }

    private data class Outcome(val status: String, val message: String)

    private fun execute(call: ToolCallParser.Call): Outcome = when (call.name) {
        "list_files" -> {
            audit.append(AuditLog.line(call.name, "approved", null))
            val files = gateway.list()
            Outcome("approved", if (files.isEmpty()) "(no files shared)" else files.joinToString("\n"))
        }
        "read_file" -> {
            val name = PathPolicy.normalize(call.args["file"])
            if (name == null) {
                audit.append(AuditLog.line(call.name, "rejected_path", null))
                Outcome("rejected_path", "rejected: that file name is not allowed")
            } else {
                val text = gateway.read(name)
                audit.append(AuditLog.line(call.name, "approved", name))
                Outcome("approved", text ?: "not found: $name")
            }
        }
        "write_file" -> {
            val name = PathPolicy.normalize(call.args["file"])
            val content = call.args["content"]
            if (name == null || content == null) {
                audit.append(AuditLog.line(call.name, "rejected_path", null))
                Outcome("rejected_path", "rejected: bad file name or missing content")
            } else {
                val result = gateway.write(name, content)
                audit.append(AuditLog.line(call.name, "approved", name))
                Outcome(
                    "approved",
                    when (result) {
                        FileGateway.WriteResult.CREATED -> "created $name"
                        FileGateway.WriteResult.OVERWRITTEN -> "overwrote $name"
                        FileGateway.WriteResult.REJECTED -> "rejected: could not write $name"
                    },
                )
            }
        }
        else -> Outcome("rejected_unknown", "unknown tool")
    }

    companion object {
        private val KNOWN_TOOLS = setOf("list_files", "read_file", "write_file")

        fun describe(call: ToolCallParser.Call): String = when (call.name) {
            "list_files" -> "list shared files"
            "read_file" -> "read ${call.args["file"] ?: "?"}"
            "write_file" -> "write ${call.args["file"] ?: "?"} (${call.args["content"]?.length ?: 0} chars)"
            else -> call.name
        }

        private const val AGENT_PREAMBLE = """You can use these tools by replying with a tool block and nothing else:
<tool_call>
name: list_files
</tool_call>
or
<tool_call>
name: read_file
file: NAME
</tool_call>
or
<tool_call>
name: write_file
file: NAME
content:
...file content...
</tool_call>
Only these tools exist. Only use names returned by list_files. After a tool result, either call one more tool or answer the user in plain text. Task: """
    }
}
