package com.photoeditor.engine.document

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Isolates source-over alpha behavior on transparent destinations. */
@RunWith(AndroidJUnit4::class)
class AlphaCompositingMatrixInstrumentedTest {

    @Test
    fun directCanvasHalfAlphaOnTransparentDestinationPreservesRedAndSetsAlpha() {
        val output = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val source = solidBitmap(Color.RED)
        try {
            Canvas(output).drawBitmap(source, 0f, 0f, Paint().apply { alpha = 128 })
            val pixel = output.getPixel(0, 0)
            logPixel("direct-transparent", pixel)

            assertNear("red", 255, Color.red(pixel), 1)
            assertNear("green", 0, Color.green(pixel), 1)
            assertNear("blue", 0, Color.blue(pixel), 1)
            assertNear("alpha", 128, Color.alpha(pixel), 1)
        } finally {
            output.recycle()
            source.recycle()
        }
    }

    @Test
    fun compositorHalfOpacityOnTransparentDestinationPreservesRedAndSetsAlpha() {
        val source = solidBitmap(Color.RED)
        try {
            val document = EditorDocument(1, 1)
                .addLayer(EditorLayer("red", "Red", LayerKind.BITMAP, opacity = 0.5f))
            val output = BitmapLayerCompositor.render(document, mapOf("red" to source))
            try {
                val pixel = output.getPixel(0, 0)
                logPixel("compositor-transparent", pixel)

                // 0.5f is currently converted to Paint alpha 127 by truncation.
                assertNear("red", 255, Color.red(pixel), 1)
                assertNear("green", 0, Color.green(pixel), 1)
                assertNear("blue", 0, Color.blue(pixel), 1)
                assertNear("alpha", 127, Color.alpha(pixel), 1)
            } finally {
                output.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    private fun solidBitmap(color: Int): Bitmap =
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, color)
        }

    private fun assertNear(channel: String, expected: Int, actual: Int, tolerance: Int) {
        assertTrue(
            "$channel expected near $expected but was $actual (tolerance=$tolerance)",
            kotlin.math.abs(expected - actual) <= tolerance
        )
    }

    private fun logPixel(label: String, pixel: Int) {
        Log.i(
            "AlphaMatrix",
            "$label ARGB=(" + Color.alpha(pixel) + "," + Color.red(pixel) + "," +
                Color.green(pixel) + "," + Color.blue(pixel) + ")"
        )
    }
}
