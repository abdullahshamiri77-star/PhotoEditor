package com.photoeditor.engine.canvas

import com.photoeditor.engine.document.EditorDocument
import com.photoeditor.engine.document.EditorLayer
import com.photoeditor.engine.document.LayerKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * Resolves a document-space tap to the uppermost visible bitmap layer.
 * Coordinates and dimensions are expressed in document pixels.
 */
object LayerHitTester {
    fun findTopmost(
        document: EditorDocument,
        bitmapSizes: Map<String, Pair<Int, Int>>,
        documentX: Float,
        documentY: Float
    ): String? {
        if (!documentX.isFinite() || !documentY.isFinite()) return null
        return document.layers.asReversed().firstOrNull { layer ->
            contains(layer, bitmapSizes[layer.id], documentX, documentY)
        }?.id
    }

    private fun contains(
        layer: EditorLayer,
        size: Pair<Int, Int>?,
        pointX: Float,
        pointY: Float
    ): Boolean {
        if (!layer.visible || layer.opacity <= 0f || layer.kind != LayerKind.BITMAP || size == null) {
            return false
        }
        val (width, height) = size
        if (width <= 0 || height <= 0) return false

        val transform = layer.transform
        val translatedX = pointX - transform.x
        val translatedY = pointY - transform.y
        val radians = Math.toRadians(transform.rotationDegrees.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()

        // Invert the compositor's translate -> rotate -> scale sequence.
        val unrotatedX = translatedX * cos + translatedY * sin
        val unrotatedY = -translatedX * sin + translatedY * cos
        val localX = unrotatedX / transform.scaleX
        val localY = unrotatedY / transform.scaleY
        return localX >= 0f && localY >= 0f && localX < width && localY < height
    }
}
