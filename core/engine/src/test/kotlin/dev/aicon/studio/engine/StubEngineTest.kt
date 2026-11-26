package dev.aicon.studio.engine

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StubEngineTest {

    @Test
    fun loadFailsForMissingFile() {
        val engine = StubEngine()
        try {
            runBlocking { engine.load("/no/such/model.gguf") }
            throw AssertionError("expected EngineException")
        } catch (e: EngineException) {
            assertTrue(e.message!!.contains("not found"))
        }
        assertFalse(engine.isLoaded())
    }

    @Test
    fun loadAcceptsRealFileAndGenerateStreams() {
        val tmp = File.createTempFile("model", ".gguf")
        tmp.writeBytes(byteArrayOf(1, 2, 3))
        try {
            val engine = StubEngine()
            runBlocking { engine.load(tmp.absolutePath) }
            assertTrue(engine.isLoaded())
            val tokens = runBlocking {
                engine.generate("hello", emptyList()).toList()
            }
            assertTrue(tokens.isNotEmpty())
            assertTrue(tokens.joinToString("").contains("Engine wiring works"))
            runBlocking { engine.unload() }
            assertFalse(engine.isLoaded())
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun generateBeforeLoadFails() {
        val engine = StubEngine()
        try {
            runBlocking { engine.generate("hi", emptyList()).toList() }
            throw AssertionError("expected EngineException")
        } catch (e: EngineException) {
            assertEquals("No model loaded", e.message)
        }
    }
}
