package com.photoeditor.engine.canvas

import com.photoeditor.engine.document.EditorDocument
import com.photoeditor.engine.document.EditorLayer
import com.photoeditor.engine.document.LayerKind
import com.photoeditor.engine.document.LayerTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayerHitTesterTest {
    private fun layer(
        id: String,
        transform: LayerTransform = LayerTransform(),
        visible: Boolean = true,
        opacity: Float = 1f
    ) = EditorLayer(id, id, LayerKind.BITMAP, visible, opacity, transform = transform)

    @Test
    fun choosesTopmostVisibleLayerAtOverlappingPoint() {
        val document = EditorDocument(200, 200)
            .addLayer(layer("bottom"))
            .addLayer(layer("top"))
        assertEquals(
            "top",
            LayerHitTester.findTopmost(document, mapOf("bottom" to (100 to 100), "top" to (100 to 100)), 25f, 25f)
        )
    }

    @Test
    fun skipsHiddenOrFullyTransparentTopLayer() {
        val document = EditorDocument(200, 200)
            .addLayer(layer("bottom"))
            .addLayer(layer("hidden", visible = false))
        assertEquals(
            "bottom",
            LayerHitTester.findTopmost(document, mapOf("bottom" to (100 to 100), "hidden" to (100 to 100)), 25f, 25f)
        )
    }

    @Test
    fun mapsPointThroughTranslationScaleAndRotation() {
        val transformed = layer(
            "photo",
            LayerTransform(x = 40f, y = 30f, scaleX = 2f, scaleY = 2f, rotationDegrees = 90f)
        )
        val document = EditorDocument(300, 300).addLayer(transformed)
        // Local point (10, 20) -> scale (20, 40) -> rotate (-40, 20) -> translate (0, 50).
        assertEquals(
            "photo",
            LayerHitTester.findTopmost(document, mapOf("photo" to (100 to 100)), 0f, 50f)
        )
        assertNull(
            LayerHitTester.findTopmost(document, mapOf("photo" to (100 to 100)), 41f, 31f)
        )
    }

    @Test
    fun ignoresPointsOutsideLayerAndNonFiniteCoordinates() {
        val document = EditorDocument(100, 100).addLayer(layer("photo"))
        val sizes = mapOf("photo" to (20 to 20))
        assertNull(LayerHitTester.findTopmost(document, sizes, 20f, 10f))
        assertNull(LayerHitTester.findTopmost(document, sizes, Float.NaN, 10f))
    }
}
