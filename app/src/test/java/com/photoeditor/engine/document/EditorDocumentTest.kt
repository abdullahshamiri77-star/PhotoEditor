package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDocumentTest {
    private fun layer(id: String) = EditorLayer(id, "Layer $id", LayerKind.BITMAP)

    @Test
    fun addLayerSelectsNewLayerAndKeepsStableIds() {
        val document = EditorDocument(1200, 800).addLayer(layer("photo"))
        assertEquals("photo", document.selectedLayerId)
        assertEquals("photo", document.layers.single().id)
    }

    @Test
    fun visibilityOpacityLockAndRenameUpdateOnlyTargetLayer() {
        val document = EditorDocument(100, 100)
            .addLayer(layer("one"))
            .addLayer(layer("two"))
            .setLayerVisibility("one", false)
            .setLayerOpacity("two", 0.35f)
            .setLayerLocked("two", true)
            .renameLayer("two", "Portrait")

        assertFalse(document.layers[0].visible)
        assertEquals(0.35f, document.layers[1].opacity, 0.0001f)
        assertTrue(document.layers[1].locked)
        assertEquals("Portrait", document.layers[1].name)
    }

    @Test
    fun opacityIsClampedToSupportedRange() {
        val document = EditorDocument(100, 100).addLayer(layer("one"))
        assertEquals(1f, document.setLayerOpacity("one", 1.5f).layers[0].opacity, 0.0001f)
        assertEquals(0f, document.setLayerOpacity("one", -0.5f).layers[0].opacity, 0.0001f)
    }

    @Test
    fun movingLayerChangesStackOrderWithoutChangingIdentity() {
        val document = EditorDocument(100, 100)
            .addLayer(layer("bottom"))
            .addLayer(layer("middle"))
            .addLayer(layer("top"))
        val moved = document.moveLayer(0, 2)

        assertEquals(listOf("middle", "top", "bottom"), moved.layers.map { it.id })
        assertEquals("top", moved.selectedLayerId)
        assertNotEquals(document.layers, moved.layers)
    }

    @Test
    fun movingLayerByStableIdPreservesSelectionAndEveryLayerTransform() {
        val bottomTransform = LayerTransform(x = 12f, y = 8f, scaleX = 1.2f, scaleY = 1.2f)
        val middleTransform = LayerTransform(x = 40f, y = 25f, scaleX = 0.7f, scaleY = 0.9f)
        val topTransform = LayerTransform(x = 4f, y = 6f)
        val document = EditorDocument(200, 200)
            .addLayer(layer("bottom").copy(transform = bottomTransform))
            .addLayer(layer("middle").copy(transform = middleTransform))
            .addLayer(layer("top").copy(transform = topTransform))
            .selectLayer("middle")

        val reordered = document.moveLayer("middle", 2)

        assertEquals(listOf("bottom", "top", "middle"), reordered.layers.map { it.id })
        assertEquals("middle", reordered.selectedLayerId)
        assertEquals(middleTransform, reordered.layers.last().transform)
        assertEquals(bottomTransform, reordered.layers.first().transform)
        assertEquals(topTransform, reordered.layers[1].transform)
    }

    @Test
    fun lockedLayerCannotBeReorderedAndOtherLayersRemainUntouched() {
        val document = EditorDocument(100, 100)
            .addLayer(layer("bottom"))
            .addLayer(layer("locked").copy(locked = true))
            .addLayer(layer("top"))

        assertThrows(IllegalArgumentException::class.java) {
            document.moveLayer("locked", 0)
        }
        assertEquals(listOf("bottom", "locked", "top"), document.layers.map { it.id })
        assertFalse(document.layers.first { it.id == "bottom" }.locked)
    }

    @Test
    fun movingUnknownLayerByIdIsRejected() {
        val document = EditorDocument(100, 100).addLayer(layer("existing"))
        assertThrows(IllegalArgumentException::class.java) {
            document.moveLayer("missing", 0)
        }
    }

    @Test
    fun removingSelectedLayerSelectsNearestRemainingLayer() {
        val document = EditorDocument(100, 100)
            .addLayer(layer("first"))
            .addLayer(layer("second"))
            .addLayer(layer("third"))
            .selectLayer("second")

        val updated = document.removeLayer("second")
        assertEquals(listOf("first", "third"), updated.layers.map { it.id })
        assertEquals("third", updated.selectedLayerId)
    }

    @Test
    fun removingLastLayerLeavesNoSelection() {
        val updated = EditorDocument(100, 100).addLayer(layer("only")).removeLayer("only")
        assertTrue(updated.layers.isEmpty())
        assertNull(updated.selectedLayerId)
    }

    @Test
    fun duplicateIdsAndInvalidDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            EditorDocument(100, 100).addLayer(layer("same")).addLayer(layer("same"))
        }
        assertThrows(IllegalArgumentException::class.java) { EditorDocument(0, 100) }
    }

    @Test
    fun updateCannotChangeLayerIdentity() {
        val document = EditorDocument(100, 100).addLayer(layer("one"))
        assertThrows(IllegalArgumentException::class.java) {
            document.updateLayer("one") { it.copy(id = "renamed-id") }
        }
    }


    @Test
    fun lockingAndUnlockingOneLayerPreservesOtherLayerState() {
        val document = EditorDocument(100, 100)
            .addLayer(layer("photo"))
            .addLayer(layer("background"))
            .setLayerOpacity("background", 0.4f)
            .setLayerLocked("photo", true)

        val locked = document.layers.first { it.id == "photo" }
        val untouched = document.layers.first { it.id == "background" }
        assertTrue(locked.locked)
        assertEquals(0.4f, untouched.opacity, 0.0001f)
        assertFalse(untouched.locked)

        val unlocked = document.setLayerLocked("photo", false)
        assertFalse(unlocked.layers.first { it.id == "photo" }.locked)
        assertEquals(0.4f, unlocked.layers.first { it.id == "background" }.opacity, 0.0001f)
    }

    @Test
    fun lockingUnknownLayerIsRejected() {
        val document = EditorDocument(100, 100).addLayer(layer("photo"))
        assertThrows(IllegalArgumentException::class.java) {
            document.setLayerLocked("missing", true)
        }
    }
}
