package dev.aicon.studio.engine.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallParserTest {

    @Test
    fun `parses a single call and keeps visible text`() {
        val input = "Let me read that.\n<tool_call>\nname: read_file\nfile: notes.txt\n</tool_call>"
        val (visible, calls) = ToolCallParser.parse(input)
        assertEquals("Let me read that.", visible)
        assertEquals(1, calls.size)
        assertEquals("read_file", calls[0].name)
        assertEquals(mapOf("file" to "notes.txt"), calls[0].args)
    }

    @Test
    fun `parses multiple calls`() {
        val input = "<tool_call>\nname: list_files\n</tool_call>\n" +
            "then\n<tool_call>\nname: write_file\nfile: out.txt\n</tool_call>"
        val (visible, calls) = ToolCallParser.parse(input)
        assertEquals(2, calls.size)
        assertEquals("list_files", calls[0].name)
        assertEquals("write_file", calls[1].name)
        assertTrue(visible.contains("then"))
    }

    @Test
    fun `ignores malformed blocks`() {
        val input = "hi\n<tool_call>\nfile: missingname.txt\n</tool_call>"
        val (visible, calls) = ToolCallParser.parse(input)
        assertTrue(calls.isEmpty())
        assertEquals("hi", visible)
    }

    @Test
    fun `content arg swallows the rest of the block`() {
        val input = "<tool_call>\nname: write_file\nfile: out.txt\ncontent:\nline one\nline two\n</tool_call>"
        val (_, calls) = ToolCallParser.parse(input)
        assertEquals(1, calls.size)
        assertEquals("out.txt", calls[0].args["file"])
        assertEquals("line one\nline two", calls[0].args["content"])
    }

    @Test
    fun `unterminated block stays visible and yields no call`() {
        val input = "text <tool_call>\nname: read_file\nfile: a.txt"
        val (visible, calls) = ToolCallParser.parse(input)
        assertTrue(calls.isEmpty())
        assertTrue(visible.contains("<tool_call>"))
    }
}
