package com.photoeditor.engine.document

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Produces a flattened preview/export bitmap from the document's bitmap layers.
 *
 * The document stores layer metadata while this renderer receives pixel payloads
 * separately, keyed by stable layer id. Layers are composited bottom-to-top.
 * Non-bitmap layer kinds are intentionally skipped until their renderers exist.
 */
object BitmapLayerCompositor {

    /** Stable ids of visible bitmap layers in bottom-to-top compositing order. */
    fun visibleBitmapLayerIds(document: EditorDocument): List<String> =
        document.layers
            .filter { it.kind == LayerKind.BITMAP && it.visible && it.opacity > 0f }
            .map { it.id }

    fun render(
        document: EditorDocument,
        bitmapLayers: Map<String, Bitmap>
    ): Bitmap {
        val output = Bitmap.createBitmap(
            document.width,
            document.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawColor(document.backgroundArgb, PorterDuff.Mode.SRC)

        document.layers.forEach { layer ->
            if (!layer.visible || layer.opacity <= 0f || layer.kind != LayerKind.BITMAP) {
                return@forEach
            }
            val bitmap = bitmapLayers[layer.id] ?: return@forEach
            if (bitmap.isRecycled) return@forEach

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (layer.opacity * 255f).toInt().coerceIn(0, 255)
                xfermode = when (layer.blendMode) {
                    LayerBlendMode.NORMAL -> null
                    LayerBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    LayerBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    LayerBlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                }
            }

            val transform = layer.transform
            val saveCount = canvas.save()
            canvas.translate(transform.x, transform.y)
            canvas.rotate(transform.rotationDegrees)
            canvas.scale(transform.scaleX, transform.scaleY)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            canvas.restoreToCount(saveCount)
            paint.xfermode = null
        }

        return output
    }
}
