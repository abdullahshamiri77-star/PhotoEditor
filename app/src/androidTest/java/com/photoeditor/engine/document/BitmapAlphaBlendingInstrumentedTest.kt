package com.photoeditor.engine.document

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapAlphaBlendingInstrumentedTest {

    @Test
    fun androidCanvasDrawBitmapHonorsPaintAlpha() {
        val output = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val blue = solidBitmap(Color.BLUE)
        val red = solidBitmap(Color.RED)
        try {
            val canvas = Canvas(output)
            canvas.drawBitmap(blue, 0f, 0f, Paint())
            canvas.drawBitmap(red, 0f, 0f, Paint().apply { alpha = 128 })

            val pixel = output.getPixel(0, 0)
            assertEquals(128, Color.red(pixel))
            assertEquals(0, Color.green(pixel))
            assertEquals(127, Color.blue(pixel))
            assertEquals(255, Color.alpha(pixel))
        } finally {
            output.recycle()
            blue.recycle()
            red.recycle()
        }
    }

    @Test
    fun compositorBlendsHalfOpacityLayerOnAndroidCanvas() {
        val background = solidBitmap(Color.BLUE)
        val foreground = solidBitmap(Color.RED)
        val document = EditorDocument(1, 1, backgroundArgb = Color.BLACK)
            .addLayer(EditorLayer("background", "Background", LayerKind.BITMAP))
            .addLayer(EditorLayer("foreground", "Foreground", LayerKind.BITMAP, opacity = 0.5f))
        try {
            val output = BitmapLayerCompositor.render(
                document,
                mapOf("background" to background, "foreground" to foreground)
            )
            try {
                val pixel = output.getPixel(0, 0)
                assertEquals(127, Color.red(pixel))
                assertEquals(0, Color.green(pixel))
                assertEquals(128, Color.blue(pixel))
                assertEquals(255, Color.alpha(pixel))
            } finally {
                output.recycle()
            }
        } finally {
            background.recycle()
            foreground.recycle()
        }
    }

    private fun solidBitmap(color: Int): Bitmap =
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, color)
        }
}
