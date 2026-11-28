package dev.aicon.studio.engine.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Threat-model acceptance test: audit lines carry tool name, status and a
 * path hash only. Arguments and contents must never leak into the log.
 */
class AuditLogTest {

    @Test
    fun `log line never contains arguments or file contents`() {
        val secretish = "SECRET-TOKEN-123 /home/user/private/report.pdf read entire mailbox"
        val line = AuditLog.line(
            tool = "read_file",
            status = "approved",
            path = "report.txt",
        )
        assertFalse(line.contains(secretish))
        assertFalse(line.contains("report.txt"))
        assertTrue(line.contains("\"tool\":\"read_file\""))
        assertTrue(line.contains("\"status\":\"approved\""))
        assertTrue(line.contains("\"path_hash\":\""))
    }

    @Test
    fun `path hash is stable and truncated`() {
        val hash = AuditLog.pathHash("report.txt")
        assertEquals(16, hash.length)
        assertEquals(hash, AuditLog.pathHash("report.txt"))
        assertTrue(hash != AuditLog.pathHash("other.txt"))
    }

    @Test
    fun `line without path has no hash field`() {
        val line = AuditLog.line("list_files", "approved", null)
        assertFalse(line.contains("path_hash"))
        assertEquals("{\"tool\":\"list_files\",\"status\":\"approved\"}", line)
    }

    @Test
    fun `tool names are escaped`() {
        val line = AuditLog.line("evil\"name\n", "denied", null)
        assertFalse(line.contains("evil\"name"))
        assertTrue(line.contains("\\\""))
    }
}
