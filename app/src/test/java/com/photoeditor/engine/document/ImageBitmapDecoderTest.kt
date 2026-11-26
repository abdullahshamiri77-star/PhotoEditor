package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageBitmapDecoderTest {
    @Test
    fun smallImageKeepsFullResolution() {
        assertEquals(1, ImageBitmapDecoder.calculateSampleSize(1000, 800))
    }

    @Test
    fun largeImageIsSampledToPixelLimit() {
        val sample = ImageBitmapDecoder.calculateSampleSize(12000, 9000)
        assertTrue(sample > 1)
        val width = (12000L + sample - 1) / sample
        val height = (9000L + sample - 1) / sample
        assertTrue(width * height <= ImageBitmapDecoder.MAX_IMPORT_PIXELS)
        assertTrue(width <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
        assertTrue(height <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
    }

    @Test
    fun longDimensionIsBounded() {
        val sample = ImageBitmapDecoder.calculateSampleSize(20000, 1000)
        val width = (20000L + sample - 1) / sample
        assertTrue(sample > 1)
        assertTrue(width <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
    }
}
