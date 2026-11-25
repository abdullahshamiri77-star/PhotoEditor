package com.photoeditor.engine.document

import android.graphics.Bitmap
import java.util.ArrayDeque
import java.util.IdentityHashMap

/**
 * Bounded undo/redo history for complete editor sessions.
 *
 * Snapshots own their bitmap copies. Both the number of snapshots and their
 * combined pixel allocation are bounded to avoid unbounded native-memory use.
 */
class EditorSessionHistory(
    private val capacity: Int = 20,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) {
    private val undoStack = ArrayDeque<EditorSession>()
    private val redoStack = ArrayDeque<EditorSession>()
    private var retainedBytes: Long = 0L

    /** True when the latest record was skipped because its snapshot exceeded the budget. */
    var lastRecordSkipped: Boolean = false
        private set

    /** True when undo/redo succeeded but its opposite-direction snapshot could not be retained. */
    var lastTransitionSkipped: Boolean = false
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val snapshotCount: Int get() = undoStack.size + redoStack.size
    val retainedBitmapBytes: Long get() = retainedBytes

    /**
     * Stores a deep snapshot. Returns false when the current state cannot fit
     * in the configured memory budget; the caller can still apply the edit
     * while informing the user that this operation will not be undoable.
     */
    fun record(current: EditorSession): Boolean {
        lastRecordSkipped = false
        lastTransitionSkipped = false

        val budget = maxBytes.coerceAtLeast(0L)
        if (estimateBytes(current) > budget) {
            lastRecordSkipped = true
            return false
        }

        // Snapshot first: if allocation/copy fails, existing undo and redo remain intact.
        val snapshot = try {
            copySession(current)
        } catch (_: OutOfMemoryError) {
            lastRecordSkipped = true
            return false
        } catch (_: RuntimeException) {
            lastRecordSkipped = true
            return false
        }

        val actualBytes = estimateBytes(snapshot)
        if (actualBytes > budget) {
            recycleSession(snapshot)
            lastRecordSkipped = true
            return false
        }

        // Only a valid snapshot permits destructive history changes.
        recycleAll(redoStack)
        while (undoStack.isNotEmpty() &&
            (undoStack.size >= capacity.coerceAtLeast(1) ||
                retainedBytes > budget - actualBytes)
        ) {
            removeAndRecycleFirst(undoStack)
        }

        undoStack.addLast(snapshot)
        retainedBytes += actualBytes
        return true
    }

    /**
     * Restores the previous state even when the current state is too large to
     * retain for redo. In that case redo history is cleared rather than making
     * undo itself fail or allowing the user to skip over an intermediate state.
     */
    fun undo(current: EditorSession): EditorSession? {
        lastTransitionSkipped = false
        if (undoStack.isEmpty()) return null

        val currentCopy = copyIfWithinBudget(current)
        val restored = undoStack.removeLast()
        retainedBytes = (retainedBytes - estimateBytes(restored)).coerceAtLeast(0L)

        if (currentCopy == null) {
            recycleAll(redoStack)
            lastTransitionSkipped = true
        } else {
            redoStack.addLast(currentCopy)
            retainedBytes += estimateBytes(currentCopy)
            trimOldSnapshots(preserveNewestRedo = true)
        }
        return restored
    }

    /**
     * Restores the next state even when the current state is too large to
     * retain for undo. In that case undo history is cleared to preserve a
     * truthful, contiguous history rather than skipping an intermediate state.
     */
    fun redo(current: EditorSession): EditorSession? {
        lastTransitionSkipped = false
        if (redoStack.isEmpty()) return null

        val currentCopy = copyIfWithinBudget(current)
        val restored = redoStack.removeLast()
        retainedBytes = (retainedBytes - estimateBytes(restored)).coerceAtLeast(0L)

        if (currentCopy == null) {
            recycleAll(undoStack)
            lastTransitionSkipped = true
        } else {
            undoStack.addLast(currentCopy)
            retainedBytes += estimateBytes(currentCopy)
            trimOldSnapshots(preserveNewestUndo = true)
        }
        return restored
    }

    /** Release all bitmap copies owned by this history. */
    fun clear() {
        recycleAll(undoStack)
        recycleAll(redoStack)
        retainedBytes = 0L
        lastRecordSkipped = false
        lastTransitionSkipped = false
    }

    private fun copyIfWithinBudget(session: EditorSession): EditorSession? {
        val budget = maxBytes.coerceAtLeast(0L)
        if (estimateBytes(session) > budget) return null
        return try {
            copySession(session).also { snapshot ->
                if (estimateBytes(snapshot) > budget) {
                    recycleSession(snapshot)
                    return null
                }
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun estimateBytes(session: EditorSession): Long =
        session.bitmapLayers.values.fold(0L) { total, bitmap ->
            if (bitmap.isRecycled) total
            else {
                val bytes = bitmap.allocationByteCount.toLong()
                if (Long.MAX_VALUE - total < bytes) Long.MAX_VALUE else total + bytes
            }
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

    private fun trimOldSnapshots(
        preserveNewestUndo: Boolean = false,
        preserveNewestRedo: Boolean = false
    ) {
        val budget = maxBytes.coerceAtLeast(0L)
        while (retainedBytes > budget) {
            when {
                undoStack.isNotEmpty() && (!preserveNewestUndo || undoStack.size > 1) ->
                    removeAndRecycleFirst(undoStack)
                redoStack.isNotEmpty() && (!preserveNewestRedo || redoStack.size > 1) ->
                    removeAndRecycleFirst(redoStack)
                else -> break
            }
        }
    }

    private fun removeAndRecycleFirst(stack: ArrayDeque<EditorSession>) {
        val removed = stack.removeFirst()
        retainedBytes = (retainedBytes - estimateBytes(removed)).coerceAtLeast(0L)
        recycleSession(removed)
    }

    private fun recycleAll(stack: ArrayDeque<EditorSession>) {
        while (stack.isNotEmpty()) {
            removeAndRecycleFirst(stack)
        }
    }

    private fun recycleSession(session: EditorSession) {
        recycleBitmaps(session.bitmapLayers.values)
    }

    private fun recycleBitmaps(bitmaps: Collection<Bitmap>) {
        val seen = IdentityHashMap<Bitmap, Boolean>()
        bitmaps.forEach { bitmap ->
            if (seen.put(bitmap, true) == null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    companion object {
        /** Combined native bitmap allocation retained by undo and redo snapshots. */
        const val DEFAULT_MAX_BYTES: Long = 64L * 1024L * 1024L
    }
}
