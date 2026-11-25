package com.photoeditor.engine.document

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorSessionHistoryTest {

    @Test
    fun undoRestoresPreviousStateWhenCurrentStateExceedsBudget() {
        val original = solidBitmap(1, 1, Color.RED)
        val oversized = solidBitmap(2, 2, Color.BLUE)
        val initial = EditorSession.fromBitmap(original, id = "layer")
        val current = initial.replaceBitmapPayload("layer", oversized)
        val history = EditorSessionHistory(capacity = 4, maxBytes = 8L)

        try {
            assertTrue(history.record(initial))
            val restored = history.undo(current)

            assertNotNull(restored)
            assertEquals(Color.RED, restored!!.bitmapLayers.getValue("layer").getPixel(0, 0))
            assertFalse(history.canRedo)
            assertTrue(history.lastTransitionSkipped)
            assertEquals(0L, history.retainedBitmapBytes)
        } finally {
            history.clear()
            original.recycle()
            oversized.recycle()
        }
    }

    @Test
    fun redoRestoresNextStateWhenCurrentStateExceedsBudget() {
        val original = solidBitmap(1, 1, Color.RED)
        val changed = solidBitmap(1, 1, Color.GREEN)
        val oversized = solidBitmap(2, 2, Color.BLUE)
        val initial = EditorSession.fromBitmap(original, id = "layer")
        val changedSession = initial.replaceBitmapPayload("layer", changed)
        val history = EditorSessionHistory(capacity = 4, maxBytes = 8L)

        try {
            assertTrue(history.record(initial))
            val afterUndo = history.undo(changedSession)
            assertNotNull(afterUndo)
            assertTrue(history.canRedo)

            val restored = history.redo(
                afterUndo!!.replaceBitmapPayload("layer", oversized)
            )

            assertNotNull(restored)
            assertEquals(Color.GREEN, restored!!.bitmapLayers.getValue("layer").getPixel(0, 0))
            assertFalse(history.canUndo)
            assertTrue(history.lastTransitionSkipped)
            assertEquals(0L, history.retainedBitmapBytes)
        } finally {
            history.clear()
            original.recycle()
            changed.recycle()
            oversized.recycle()
        }
    }

    @Test
    fun undoAndRedoRetainOppositeSnapshotsWhenWithinBudget() {
        val original = solidBitmap(1, 1, Color.RED)
        val changed = solidBitmap(1, 1, Color.GREEN)
        val initial = EditorSession.fromBitmap(original, id = "layer")
        val changedSession = initial.replaceBitmapPayload("layer", changed)
        val history = EditorSessionHistory(capacity = 4, maxBytes = 32L)

        try {
            assertTrue(history.record(initial))
            val undone = history.undo(changedSession)
            assertNotNull(undone)
            assertEquals(Color.RED, undone!!.bitmapLayers.getValue("layer").getPixel(0, 0))
            assertTrue(history.canRedo)
            assertFalse(history.lastTransitionSkipped)

            val redone = history.redo(undone)
            assertNotNull(redone)
            assertEquals(Color.GREEN, redone!!.bitmapLayers.getValue("layer").getPixel(0, 0))
            assertTrue(history.canUndo)
            assertFalse(history.lastTransitionSkipped)
        } finally {
            history.clear()
            original.recycle()
            changed.recycle()
        }
    }

    @Test
    fun failedOversizedRecordPreservesRedoSnapshot() {
        val red = solidBitmap(1, 1, Color.RED)
        val green = solidBitmap(1, 1, Color.GREEN)
        val oversized = solidBitmap(2, 2, Color.BLUE)
        val initial = EditorSession.fromBitmap(red, id = "layer")
        val changed = initial.replaceBitmapPayload("layer", green)
        val tooLarge = initial.replaceBitmapPayload("layer", oversized)
        val history = EditorSessionHistory(capacity = 4, maxBytes = 8L)

        try {
            assertTrue(history.record(initial))
            val undone = history.undo(changed)
            assertNotNull(undone)
            assertTrue(history.canRedo)
            val bytesBefore = history.retainedBitmapBytes

            assertFalse(history.record(tooLarge))
            assertTrue("redo state must survive a rejected record", history.canRedo)
            assertEquals(1, history.snapshotCount)
            assertEquals(bytesBefore, history.retainedBitmapBytes)
            assertTrue(history.lastRecordSkipped)

            val redone = history.redo(undone!!)
            assertNotNull(redone)
            assertEquals(Color.GREEN, redone!!.bitmapLayers.getValue("layer").getPixel(0, 0))
        } finally {
            history.clear()
            red.recycle()
            green.recycle()
            oversized.recycle()
        }
    }

    private fun solidBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
}
