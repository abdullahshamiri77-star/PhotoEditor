package com.photoeditor.engine.document

import android.graphics.Bitmap
import java.util.ArrayDeque
import java.util.IdentityHashMap

/**
 * Bounded undo/redo history for complete editor sessions.
 *
 * Each stored state owns copies of its bitmap payloads. Snapshots discarded by
 * capacity limits, redo invalidation, or clear() are recycled immediately.
 * A state returned by undo()/redo() transfers ownership to the caller.
 */
class EditorSessionHistory(private val capacity: Int = 20) {
    private val undoStack = ArrayDeque<EditorSession>()
    private val redoStack = ArrayDeque<EditorSession>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(current: EditorSession) {
        undoStack.addLast(copySession(current))
        while (undoStack.size > capacity.coerceAtLeast(1)) {
            recycleSession(undoStack.removeFirst())
        }
        recycleAll(redoStack)
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

    /** Release all bitmap copies owned by this history. */
    fun clear() {
        recycleAll(undoStack)
        recycleAll(redoStack)
    }

    private fun copySession(session: EditorSession): EditorSession {
        val copiedPayloads = LinkedHashMap<String, Bitmap>()
        try {
            session.bitmapLayers.forEach { (id, bitmap) ->
                require(!bitmap.isRecycled) { "Cannot snapshot a recycled bitmap" }
                copiedPayloads[id] = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    ?: error("Could not copy bitmap for history snapshot")
            }
            return EditorSession(session.document, copiedPayloads)
        } catch (error: Throwable) {
            recycleBitmaps(copiedPayloads.values)
            throw error
        }
    }

    private fun recycleAll(stack: ArrayDeque<EditorSession>) {
        while (stack.isNotEmpty()) {
            recycleSession(stack.removeFirst())
        }
    }

    private fun recycleSession(session: EditorSession) {
        recycleBitmaps(session.bitmapLayers.values)
    }

    private fun recycleBitmaps(bitmaps: Collection<Bitmap>) {
        // Be defensive if a payload is referenced by more than one layer id.
        val seen = IdentityHashMap<Bitmap, Boolean>()
        bitmaps.forEach { bitmap ->
            if (seen.put(bitmap, true) == null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
