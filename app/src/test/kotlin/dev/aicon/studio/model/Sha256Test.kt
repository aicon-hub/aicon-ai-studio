package dev.aicon.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sha256Test {

    @Test
    fun `known vector hashes correctly`() {
        // SHA-256("abc") from the FIPS 180-4 examples.
        val hash = Sha256.ofStream("abc".byteInputStream())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash,
        )
    }

    @Test
    fun `matches accepts expected hash and rejects others`() {
        val tmp = File.createTempFile("sha256", ".bin")
        try {
            tmp.writeText("hello")
            assertTrue(
                Sha256.matches(
                    tmp,
                    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                ),
            )
            assertFalse(Sha256.matches(tmp, "0".repeat(64)))
        } finally {
            tmp.delete()
        }
    }
}
