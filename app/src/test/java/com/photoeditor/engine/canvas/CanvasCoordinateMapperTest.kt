package com.photoeditor.engine.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanvasCoordinateMapperTest {
    @Test
    fun mapsCanvasCenterToDocumentCenterWhenFitWithoutViewportTransform() {
        val result = CanvasCoordinateMapper.screenToDocument(
            canvasWidth = 400f, canvasHeight = 300f,
            documentWidth = 200, documentHeight = 100,
            screenX = 200f, screenY = 150f,
            viewportScale = 1f, viewportOffsetX = 0f, viewportOffsetY = 0f
        )
        assertEquals(100f, result!!.first, 0.0001f)
        assertEquals(50f, result.second, 0.0001f)
    }

    @Test
    fun accountsForViewportZoomPanAndLetterboxing() {
        // A 200x100 document fits a 400x300 canvas at 2x with 50px vertical margin.
        // Document point (20,25) maps to fit-space (40,100), then zoom/pan to (-90,40).
        val result = CanvasCoordinateMapper.screenToDocument(
            canvasWidth = 400f, canvasHeight = 300f,
            documentWidth = 200, documentHeight = 100,
            screenX = -90f, screenY = 40f,
            viewportScale = 2f, viewportOffsetX = 30f, viewportOffsetY = -10f
        )
        assertEquals(20f, result!!.first, 0.0001f)
        assertEquals(25f, result.second, 0.0001f)
    }

    @Test
    fun rejectsInvalidDimensionsAndViewportValues() {
        assertNull(CanvasCoordinateMapper.screenToDocument(
            canvasWidth = 0f, canvasHeight = 300f,
            documentWidth = 200, documentHeight = 100,
            screenX = 0f, screenY = 0f,
            viewportScale = 1f, viewportOffsetX = 0f, viewportOffsetY = 0f
        ))
        assertNull(CanvasCoordinateMapper.screenToDocument(
            canvasWidth = 400f, canvasHeight = 300f,
            documentWidth = 200, documentHeight = 100,
            screenX = Float.NaN, screenY = 0f,
            viewportScale = 1f, viewportOffsetX = 0f, viewportOffsetY = 0f
        ))
        assertNull(CanvasCoordinateMapper.screenToDocument(
            canvasWidth = 400f, canvasHeight = 300f,
            documentWidth = 200, documentHeight = 100,
            screenX = 0f, screenY = 0f,
            viewportScale = 0f, viewportOffsetX = 0f, viewportOffsetY = 0f
        ))
    }
}
