package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentManagerTest {
    @Test
    fun createsBlankSquareDocumentFromPreset() {
        val session = DocumentManager.create(DocumentPreset.SQUARE_1080, 0xFFFFFFFF.toInt())

        assertEquals(1080, session.document.width)
        assertEquals(1080, session.document.height)
        assertEquals(300, session.document.dpi)
        assertEquals(0xFFFFFFFF.toInt(), session.document.backgroundArgb)
        assertEquals(emptyList<EditorLayer>(), session.document.layers)
        assertNull(session.document.selectedLayerId)
        assertEquals(emptyMap<String, android.graphics.Bitmap>(), session.bitmapLayers)
    }

    @Test
    fun everyBuiltInPresetFitsTheDocumentSafetyLimits() {
        DocumentPreset.values().forEach { preset ->
            val spec = DocumentSpec.fromPreset(preset)
            assertEquals(preset.widthPx, spec.widthPx)
            assertEquals(preset.heightPx, spec.heightPx)
            assertEquals(preset.dpi, spec.dpi)
            assertEquals(true, spec.widthPx.toLong() * spec.heightPx <= DocumentSpec.MAX_PIXELS)
        }
    }

    @Test
    fun convertsMillimetersToPixelsAtRequestedDpi() {
        val spec = DocumentSpec.fromPhysicalSize(
            width = 210.0,
            height = 297.0,
            unit = DocumentSizeUnit.MILLIMETERS,
            dpi = 300
        )

        assertEquals(2480, spec.widthPx)
        assertEquals(3508, spec.heightPx)
    }

    @Test
    fun convertsInchesToPixelsAtRequestedDpi() {
        val spec = DocumentSpec.fromPhysicalSize(
            width = 8.5,
            height = 11.0,
            unit = DocumentSizeUnit.INCHES,
            dpi = 300
        )

        assertEquals(2550, spec.widthPx)
        assertEquals(3300, spec.heightPx)
    }

    @Test
    fun pixelUnitDoesNotScaleByDpi() {
        val spec = DocumentSpec.fromPhysicalSize(
            width = 1200.0,
            height = 800.0,
            unit = DocumentSizeUnit.PIXELS,
            dpi = 600
        )

        assertEquals(1200, spec.widthPx)
        assertEquals(800, spec.heightPx)
        assertEquals(600, spec.dpi)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDocumentsAboveSafePixelBudget() {
        DocumentSpec(widthPx = 8000, heightPx = 5000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPhysicalDimensionsThatOverflowPixelLimits() {
        DocumentSpec.fromPhysicalSize(
            width = 100_000.0,
            height = 100_000.0,
            unit = DocumentSizeUnit.INCHES,
            dpi = 1200
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDpi() {
        DocumentSpec(widthPx = 100, heightPx = 100, dpi = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFinitePhysicalDimensions() {
        DocumentSpec.fromPhysicalSize(
            width = Double.POSITIVE_INFINITY,
            height = 10.0,
            unit = DocumentSizeUnit.INCHES
        )
    }
}
