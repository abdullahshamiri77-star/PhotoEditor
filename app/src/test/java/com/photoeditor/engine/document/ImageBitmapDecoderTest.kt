package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageBitmapDecoderTest {

    @Test
    fun rejectsZeroAndNegativeDimensions() {
        assertNull(ImageBitmapDecoder.calculateTargetSize(0, 100))
        assertNull(ImageBitmapDecoder.calculateTargetSize(100, 0))
        assertNull(ImageBitmapDecoder.calculateTargetSize(-1, 100))
    }

    @Test
    fun preservesDimensionsForSmallImages() {
        assertEquals(1200 to 800, ImageBitmapDecoder.calculateTargetSize(1200, 800))
    }

    @Test
    fun constrainsVeryLargeImageByPixelBudgetAndPreservesAspectRatio() {
        val target = ImageBitmapDecoder.calculateTargetSize(12000, 8000)!!
        assertTrue(target.first <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
        assertTrue(target.second <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
        assertTrue(target.first.toLong() * target.second.toLong() <= ImageBitmapDecoder.MAX_IMPORT_PIXELS)
        assertTrue(kotlin.math.abs(target.first.toDouble() / target.second - 1.5) < 0.001)
    }

    @Test
    fun constrainsPanoramicImageByMaximumDimension() {
        val target = ImageBitmapDecoder.calculateTargetSize(30000, 1000)!!
        assertTrue(target.first <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
        assertTrue(target.second <= ImageBitmapDecoder.MAX_IMPORT_DIMENSION)
        assertTrue(target.first.toLong() * target.second.toLong() <= ImageBitmapDecoder.MAX_IMPORT_PIXELS)
    }
}
