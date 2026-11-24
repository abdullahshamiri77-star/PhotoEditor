package com.photoeditor.engine.document

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BitmapLayerCompositorTest {

    @Test
    fun visibleBitmapLayerIdsAreReturnedBottomToTop() {
        val document = EditorDocument(64, 64)
            .addLayer(EditorLayer("background", "Background", LayerKind.BITMAP))
            .addLayer(EditorLayer("hidden", "Hidden", LayerKind.BITMAP, visible = false))
            .addLayer(EditorLayer("text", "Text", LayerKind.TEXT))
            .addLayer(EditorLayer("top", "Top", LayerKind.BITMAP, opacity = 0.5f))

        assertEquals(listOf("background", "top"), BitmapLayerCompositor.visibleBitmapLayerIds(document))
    }

    @Test
    fun isolatedCanvasDrawBitmapWithPaintAlphaBlends() {
        val output = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val blue = solidBitmap(Color.BLUE)
        val red = solidBitmap(Color.RED)
        val canvas = Canvas(output)
        canvas.drawBitmap(blue, 0f, 0f, Paint())
        val paint = Paint().apply { alpha = 128 }
        canvas.drawBitmap(red, 0f, 0f, paint)
        val pixel = output.getPixel(0, 0)
        try {
            assertEquals("isolated Canvas red channel", 128, Color.red(pixel))
            assertEquals("isolated Canvas green channel", 0, Color.green(pixel))
            assertEquals("isolated Canvas blue channel", 127, Color.blue(pixel))
            assertEquals("isolated Canvas alpha channel", 255, Color.alpha(pixel))
        } finally {
            output.recycle()
            blue.recycle()
            red.recycle()
        }
    }

    @Test
    fun opaqueTopLayerCoversBottomLayerPixel() {
        val bottom = solidBitmap(Color.BLUE)
        val top = solidBitmap(Color.RED)
        val document = EditorDocument(1, 1)
            .addLayer(EditorLayer("bottom", "Bottom", LayerKind.BITMAP))
            .addLayer(EditorLayer("top", "Top", LayerKind.BITMAP))
        val output = BitmapLayerCompositor.render(document, mapOf("bottom" to bottom, "top" to top))
        try {
            assertEquals(Color.RED, output.getPixel(0, 0))
        } finally {
            output.recycle()
            bottom.recycle()
            top.recycle()
        }
    }

    @Test
    fun hiddenLayerDoesNotChangeRenderedPixel() {
        val background = solidBitmap(Color.BLUE)
        val hidden = solidBitmap(Color.RED)
        val document = EditorDocument(1, 1)
            .addLayer(EditorLayer("background", "Background", LayerKind.BITMAP))
            .addLayer(EditorLayer("hidden", "Hidden", LayerKind.BITMAP, visible = false))
        val output = BitmapLayerCompositor.render(document, mapOf("background" to background, "hidden" to hidden))
        try {
            assertEquals(Color.BLUE, output.getPixel(0, 0))
        } finally {
            output.recycle()
            background.recycle()
            hidden.recycle()
        }
    }

    @Test
    fun halfOpacityBlendsTopLayerWithOpaqueBackground() {
        val background = solidBitmap(Color.BLUE)
        val foreground = solidBitmap(Color.RED)
        val document = EditorDocument(1, 1, backgroundArgb = Color.BLACK)
            .addLayer(EditorLayer("background", "Background", LayerKind.BITMAP))
            .addLayer(EditorLayer("foreground", "Foreground", LayerKind.BITMAP, opacity = 0.5f))
        val output = BitmapLayerCompositor.render(document, mapOf("background" to background, "foreground" to foreground))
        try {
            val pixel = output.getPixel(0, 0)
            assertTrue("Expected red near 127, actual=${Color.red(pixel)}, pixel=$pixel", kotlin.math.abs(Color.red(pixel) - 127) <= 2)
            assertTrue("Expected green near 0, actual=${Color.green(pixel)}, pixel=$pixel", kotlin.math.abs(Color.green(pixel)) <= 2)
            assertTrue("Expected blue near 128, actual=${Color.blue(pixel)}, pixel=$pixel", kotlin.math.abs(Color.blue(pixel) - 128) <= 2)
            assertEquals(255, Color.alpha(pixel))
        } finally {
            output.recycle()
            background.recycle()
            foreground.recycle()
        }
    }

    @Test
    fun lockedLayerStillRendersWithoutChangingPixelOutput() {
        val bitmap = solidBitmap(Color.GREEN)
        val unlocked = EditorDocument(1, 1)
            .addLayer(EditorLayer("layer", "Layer", LayerKind.BITMAP, locked = false))
        val locked = unlocked.setLayerLocked("layer", true)
        val unlockedOutput = BitmapLayerCompositor.render(unlocked, mapOf("layer" to bitmap))
        val lockedOutput = BitmapLayerCompositor.render(locked, mapOf("layer" to bitmap))
        try {
            assertEquals(Color.GREEN, unlockedOutput.getPixel(0, 0))
            assertEquals(unlockedOutput.getPixel(0, 0), lockedOutput.getPixel(0, 0))
            assertTrue(locked.layers.single().locked)
        } finally {
            unlockedOutput.recycle()
            lockedOutput.recycle()
            bitmap.recycle()
        }
    }

    private fun solidBitmap(color: Int): Bitmap =
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { setPixel(0, 0, color) }
}
