package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapLayerCompositorTest {
    @Test
    fun visibleBitmapLayerIdsAreReturnedBottomToTop() {
        val document = EditorDocument(64, 64)
            .addLayer(EditorLayer("background", "Background", LayerKind.BITMAP))
            .addLayer(EditorLayer("hidden", "Hidden", LayerKind.BITMAP, visible = false))
            .addLayer(EditorLayer("text", "Text", LayerKind.TEXT))
            .addLayer(EditorLayer("top", "Top", LayerKind.BITMAP, opacity = 0.5f))

        assertEquals(
            listOf("background", "top"),
            BitmapLayerCompositor.visibleBitmapLayerIds(document)
        )
    }
}
