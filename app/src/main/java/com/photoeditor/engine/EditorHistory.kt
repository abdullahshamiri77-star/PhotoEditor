package com.photoeditor.engine

import android.graphics.Bitmap
import java.util.ArrayDeque

/**
 * Bounded bitmap history. Snapshots are copied so later edits cannot mutate
 * an earlier state. The current image is stored separately by the screen.
 */
class EditorHistory(private val capacity: Int = 20) {
    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(current: Bitmap) {
        undoStack.addLast(current.copy(Bitmap.Config.ARGB_8888, false))
        while (undoStack.size > capacity.coerceAtLeast(1)) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: Bitmap): Bitmap? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current.copy(Bitmap.Config.ARGB_8888, false))
        return undoStack.removeLast()
    }

    fun redo(current: Bitmap): Bitmap? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current.copy(Bitmap.Config.ARGB_8888, false))
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
