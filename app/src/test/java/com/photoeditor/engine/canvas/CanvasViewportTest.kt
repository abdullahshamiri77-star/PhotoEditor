package com.photoeditor.engine.canvas

import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasViewportTest {
    @Test
    fun zoomClampsToSupportedRange() {
        assertEquals(CanvasViewport.MAX_SCALE, CanvasViewport().zoomBy(100f).scale)
        assertEquals(CanvasViewport.MIN_SCALE, CanvasViewport().zoomBy(0.01f).scale)
    }

    @Test
    fun zoomAroundPivotKeepsThatPointStationary() {
        val next = CanvasViewport().transform(
            zoomFactor = 2f,
            panX = 0f,
            panY = 0f,
            pivotX = 100f,
            pivotY = 80f
        )

        assertEquals(2f, next.scale)
        assertEquals(-100f, next.offsetX)
        assertEquals(-80f, next.offsetY)
    }

    @Test
    fun panChangesOnlyViewportOffsets() {
        val next = CanvasViewport(scale = 1.5f).panBy(24f, -12f)

        assertEquals(1.5f, next.scale)
        assertEquals(24f, next.offsetX)
        assertEquals(-12f, next.offsetY)
    }

    @Test
    fun gestureZoomClampsWithoutLosingPanDelta() {
        val next = CanvasViewport(scale = 7.5f, offsetX = 10f, offsetY = 20f).transform(
            zoomFactor = 4f,
            panX = 5f,
            panY = -3f,
            pivotX = 0f,
            pivotY = 0f
        )

        assertEquals(CanvasViewport.MAX_SCALE, next.scale)
        assertEquals(15.666667f, next.offsetX, 0.001f)
        assertEquals(10.333333f, next.offsetY, 0.001f)
    }

    @Test
    fun resetRestoresFitTransform() {
        val next = CanvasViewport(scale = 4f, offsetX = 20f, offsetY = -30f).reset()

        assertEquals(CanvasViewport(), next)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidZoomFactor() {
        CanvasViewport().zoomBy(Float.NaN)
    }
}
