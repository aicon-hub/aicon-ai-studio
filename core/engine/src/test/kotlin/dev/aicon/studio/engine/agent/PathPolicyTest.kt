package dev.aicon.studio.engine.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Threat-model acceptance test (synthetic): the file gateway must refuse
 * every name that could reach outside the user's approved share folder,
 * including app-private files the user never picked.
 */
class PathPolicyTest {

    @Test
    fun `plain names are allowed`() {
        assertTrue(PathPolicy.isAllowed("notes.txt"))
        assertTrue(PathPolicy.isAllowed("My Report 2026.md"))
        assertTrue(PathPolicy.isAllowed("a"))
    }

    @Test
    fun `traversal and absolute paths are rejected`() {
        assertFalse(PathPolicy.isAllowed("../secret.txt"))
        assertFalse(PathPolicy.isAllowed(".."))
        assertFalse(PathPolicy.isAllowed("a/../../b"))
        assertFalse(PathPolicy.isAllowed("/data/data/dev.aicon.studio/databases/private.db"))
        assertFalse(PathPolicy.isAllowed("folder/inner.txt"))
        assertFalse(PathPolicy.isAllowed("folder\\inner.txt"))
        assertFalse(PathPolicy.isAllowed("..\\windows"))
    }

    @Test
    fun `blank and padded names are rejected`() {
        assertFalse(PathPolicy.isAllowed(""))
        assertFalse(PathPolicy.isAllowed("   "))
        assertFalse(PathPolicy.isAllowed(null))
        assertFalse(PathPolicy.isAllowed(" notes.txt"))
        assertFalse(PathPolicy.isAllowed("notes.txt "))
    }

    @Test
    fun `control characters and long names are rejected`() {
        assertFalse(PathPolicy.isAllowed("bad\u0000name"))
        assertFalse(PathPolicy.isAllowed("a".repeat(200)))
    }

    @Test
    fun `normalize returns null for rejected names`() {
        assertNull(PathPolicy.normalize("../etc/passwd"))
        assertTrue(PathPolicy.normalize(" ok.txt ") == null)
        assertTrue(PathPolicy.normalize("ok.txt") == "ok.txt")
    }
}
