package com.photoeditor.engine.document

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Runtime pixel payload for a bitmap layer. The document model stays free of
 * Android objects; this adapter joins stable layer metadata to its pixels.
 */
data class BitmapLayerContent(
    val layer: EditorLayer,
    val bitmap: Bitmap
)

/**
 * CPU compositor for bitmap layers. Layer order follows EditorDocument:
 * index 0 is the bottom layer and the last index is the top layer.
 */
object BitmapLayerRenderer {
    fun render(
        document: EditorDocument,
        contents: Map<String, BitmapLayerContent>
    ): Bitmap {
        val output = Bitmap.createBitmap(
            document.width,
            document.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawColor(document.backgroundArgb)

        document.layers.forEach { layer ->
            if (!layer.visible || layer.opacity <= 0f) return@forEach
            val content = contents[layer.id] ?: return@forEach
            if (content.bitmap.isRecycled) return@forEach

            val transform = layer.transform
            val saveCount = canvas.save()
            val centerX = document.width / 2f + transform.x
            val centerY = document.height / 2f + transform.y
            canvas.rotate(transform.rotationDegrees, centerX, centerY)

            val halfWidth = content.bitmap.width * transform.scaleX / 2f
            val halfHeight = content.bitmap.height * transform.scaleY / 2f
            val destination = RectF(
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (layer.opacity * 255f).toInt().coerceIn(0, 255)
            }
            canvas.drawBitmap(content.bitmap, null, destination, paint)
            canvas.restoreToCount(saveCount)
        }
        return output
    }
}
