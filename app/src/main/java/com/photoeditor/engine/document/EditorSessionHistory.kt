package com.photoeditor.engine.document

import android.graphics.Bitmap
import java.util.ArrayDeque

/**
 * Bounded undo/redo history for complete editor sessions.
 *
 * Every stored state owns copies of its bitmap payloads, preventing subsequent
 * edits from mutating historical pixels. Callers should clear the history when
 * a document is closed to release retained bitmap memory.
 */
class EditorSessionHistory(private val capacity: Int = 20) {
    private val undoStack = ArrayDeque<EditorSession>()
    private val redoStack = ArrayDeque<EditorSession>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(current: EditorSession) {
        undoStack.addLast(copySession(current))
        while (undoStack.size > capacity.coerceAtLeast(1)) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(current: EditorSession): EditorSession? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(copySession(current))
        return undoStack.removeLast()
    }

    fun redo(current: EditorSession): EditorSession? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(copySession(current))
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    private fun copySession(session: EditorSession): EditorSession {
        val copiedPayloads = session.bitmapLayers.mapValues { (_, bitmap) ->
            require(!bitmap.isRecycled) { "Cannot snapshot a recycled bitmap" }
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
                ?: error("Could not copy bitmap for history snapshot")
        }
        return EditorSession(session.document, copiedPayloads)
    }
}
