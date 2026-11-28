package dev.aicon.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catalog integrity: a READY entry must be downloadable and verifiable, a
 * not-ready entry must never offer a download, and every tab stays honest.
 */
class ModelCatalogTest {

    private val hex64 = Regex("[0-9a-f]{64}")

    @Test
    fun `ids are unique`() {
        val ids = ModelCatalog.models.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every category has at least one entry`() {
        for (category in ModelCategory.entries) {
            assertTrue(
                "category $category has no entries",
                ModelCatalog.byCategory(category).isNotEmpty(),
            )
        }
    }

    @Test
    fun `ready entries carry a download and a 64-hex sha`() {
        for (info in ModelCatalog.models.filter { it.readiness == Readiness.READY }) {
            assertTrue(info.id, info.downloadable)
            assertTrue(info.id, info.downloadUrl!!.startsWith("https://"))
            assertTrue(info.id, hex64.matches(info.sha256!!))
            assertTrue(info.id, info.sizeBytes > 0)
            assertNotNull(info.id, info.ramMb)
        }
    }

    @Test
    fun `vision ready entries carry a verified mmproj`() {
        for (info in ModelCatalog.models.filter {
            it.category == ModelCategory.VISION && it.readiness == Readiness.READY
        }) {
            val m = info.mmproj
            assertNotNull(info.id, m)
            assertTrue(info.id, m!!.downloadUrl.startsWith("https://"))
            assertTrue(info.id, hex64.matches(m.sha256))
            assertTrue(info.id, m.sizeBytes > 0)
        }
    }

    @Test
    fun `not-ready entries are not downloadable`() {
        for (info in ModelCatalog.models.filter { it.readiness != Readiness.READY }) {
            assertNull(info.id, info.downloadUrl)
            assertNull(info.id, info.sha256)
            assertTrue(info.id, !info.downloadable)
        }
    }
}
