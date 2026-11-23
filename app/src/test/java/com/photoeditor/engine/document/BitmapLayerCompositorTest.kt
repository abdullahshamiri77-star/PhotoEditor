package com.photoeditor.engine.document

import android.graphics.Bitmap
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

        assertEquals(
            listOf("background", "top"),
            BitmapLayerCompositor.visibleBitmapLayerIds(document)
        )
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

        val output = BitmapLayerCompositor.render(
            document,
            mapOf("background" to background, "hidden" to hidden)
        )
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

        val output = BitmapLayerCompositor.render(
            document,
            mapOf("background" to background, "foreground" to foreground)
        )
        try {
            val pixel = output.getPixel(0, 0)
            assertEquals(127, Color.red(pixel), 2)
            assertEquals(0, Color.green(pixel), 2)
            assertEquals(128, Color.blue(pixel), 2)
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
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, color)
        }
}
