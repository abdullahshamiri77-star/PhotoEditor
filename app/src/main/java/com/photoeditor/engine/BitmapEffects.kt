package com.photoeditor.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.roundToInt

/**
 * Bitmap operations used by the editor. Each operation returns a new bitmap,
 * leaving the source image unchanged so history and undo remain reliable.
 */
object BitmapEffects {
    fun adjust(
        source: Bitmap,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f
    ): Bitmap {
        val safeContrast = contrast.coerceIn(0f, 3f)
        val safeSaturation = saturation.coerceIn(0f, 3f)
        val translate = brightness.coerceIn(-1f, 1f) * 255f
        val matrix = ColorMatrix().apply {
            setSaturation(safeSaturation)
            val values = floatArrayOf(
                safeContrast, 0f, 0f, 0f, translate,
                0f, safeContrast, 0f, 0f, translate,
                0f, 0f, safeContrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
            postConcat(ColorMatrix(values))
        }
        return render(source, ColorMatrixColorFilter(matrix))
    }

    fun grayscale(source: Bitmap): Bitmap = adjust(source, saturation = 0f)

    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val normalized = ((degrees % 360f) + 360f) % 360f
        val matrix = android.graphics.Matrix().apply { postRotate(normalized) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipHorizontal(source: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { setScale(-1f, 1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipVertical(source: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { setScale(1f, -1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun crop(source: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        require(width > 0 && height > 0) { "Crop dimensions must be positive" }
        val x = left.coerceIn(0, source.width - 1)
        val y = top.coerceIn(0, source.height - 1)
        val w = width.coerceAtMost(source.width - x)
        val h = height.coerceAtMost(source.height - y)
        return Bitmap.createBitmap(source, x, y, w, h)
    }

    private fun render(source: Bitmap, filter: ColorMatrixColorFilter): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = filter
        }
        Canvas(output).drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
