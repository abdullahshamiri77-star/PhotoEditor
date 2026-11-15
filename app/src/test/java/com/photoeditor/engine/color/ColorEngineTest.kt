package com.photoeditor.engine.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorModelsTest {
    @Test fun parsesShortAndLongHex() {
        assertEquals(ColorValue(255, 0, 170), ColorValue.fromHex("#F0A"))
        assertEquals(ColorValue(18, 52, 86), ColorValue.fromHex("#123456"))
        assertEquals(ColorValue(18, 52, 86, 128), ColorValue.fromHex("#80123456"))
        assertNull(ColorValue.fromHex("#12"))
    }

    @Test fun rgbHslRoundTrip() {
        val original = ColorValue(34, 145, 210, 180)
        val restored = ColorValue.fromHsl(original.toHsl(), original.alpha)
        assertChannelClose(original.red, restored.red)
        assertChannelClose(original.green, restored.green)
        assertChannelClose(original.blue, restored.blue)
        assertEquals(original.alpha, restored.alpha)
    }

    @Test fun rgbCmykRoundTrip() {
        val original = ColorValue(32, 120, 200)
        val restored = ColorValue.fromCmyk(original.toCmyk())
        assertChannelClose(original.red, restored.red)
        assertChannelClose(original.green, restored.green)
        assertChannelClose(original.blue, restored.blue)
    }

    @Test fun blackCmykDoesNotDivideByZero() {
        assertEquals(CmykColor(0.0, 0.0, 0.0, 100.0), ColorValue(0, 0, 0).toCmyk())
    }

    @Test fun rejectsInvalidChannelValues() {
        assertThrows(IllegalArgumentException::class.java) { ColorValue(256, 0, 0) }
    }

    private fun assertChannelClose(expected: Int, actual: Int) {
        assertTrue("Expected $expected to be within 2 of $actual", kotlin.math.abs(expected - actual) <= 2)
    }
}

class GradientModelTest {
    @Test fun samplesThreeColorGradient() {
        val gradient = GradientSpec(
            listOf(
                GradientStop("blue", 0f, ColorValue(0, 0, 255)),
                GradientStop("gold", 0.5f, ColorValue(255, 190, 0)),
                GradientStop("white", 1f, ColorValue(255, 255, 255))
            )
        )
        assertEquals(ColorValue(0, 0, 255), gradient.colorAt(0f))
        assertEquals(ColorValue(255, 190, 0), gradient.colorAt(0.5f))
        assertEquals(ColorValue(255, 255, 255), gradient.colorAt(1f))
        assertNotNull(gradient.colorAt(0.25f))
    }

    @Test fun movingStopChangesItsPositionWithoutChangingOtherStops() {
        val original = GradientSpec.twoColor(ColorValue(0, 0, 0), ColorValue(255, 255, 255))
        val moved = original.moveStop("end", 0.7f)
        assertTrue(kotlin.math.abs(original.stops.last().position - 1f) < 0.0001f)
        assertTrue(kotlin.math.abs(moved.stops.last().position - 0.7f) < 0.0001f)
    }

    @Test fun addingAndRemovingStopsPreservesMinimumTwo() {
        val base = GradientSpec.twoColor(ColorValue(0, 0, 0), ColorValue(255, 255, 255))
        val expanded = base.addStop(GradientStop("middle", 0.5f, ColorValue(120, 80, 40)))
        assertEquals(3, expanded.stops.size)
        assertEquals(2, expanded.removeStop("middle").stops.size)
        assertThrows(IllegalArgumentException::class.java) { base.removeStop("start") }
    }
}
