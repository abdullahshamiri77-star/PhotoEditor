package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class EditorSessionTest {

    @Test
    fun newDocumentStartsWithNoLayersOrPixelPayloads() {
        val session = EditorSession.newDocument(1200, 800, dpi = 240)

        assertEquals(1200, session.document.width)
        assertEquals(800, session.document.height)
        assertEquals(240, session.document.dpi)
        assertEquals(emptyList<EditorLayer>(), session.document.layers)
        assertEquals(emptyMap<String, android.graphics.Bitmap>(), session.bitmapLayers)
    }

    @Test
    fun removingLayerAlsoRemovesItsPayloadEntry() {
        val document = EditorDocument(
            width = 100,
            height = 100,
            layers = listOf(
                EditorLayer(id = "photo", name = "Photo", kind = LayerKind.BITMAP),
                EditorLayer(id = "caption", name = "Caption", kind = LayerKind.TEXT)
            ),
            selectedLayerId = "photo"
        )
        val session = EditorSession(document)

        val updated = session.removeLayer("photo")

        assertFalse(updated.document.layers.any { it.id == "photo" })
        assertNull(updated.document.selectedLayerId)
        assertEquals(emptyMap<String, android.graphics.Bitmap>(), updated.bitmapLayers)
    }

    @Test
    fun metadataUpdatesPreserveSessionPayloadMap() {
        val document = EditorDocument(
            width = 100,
            height = 100,
            layers = listOf(EditorLayer(id = "photo", name = "Photo", kind = LayerKind.BITMAP))
        )
        val session = EditorSession(document)
        val updated = session.updateDocument { it.setLayerVisibility("photo", false) }

        assertFalse(updated.document.layers.single().visible)
        assertSame(session.bitmapLayers, updated.bitmapLayers)
    }
}
