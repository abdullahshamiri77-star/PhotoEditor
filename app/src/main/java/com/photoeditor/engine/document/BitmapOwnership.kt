package com.photoeditor.engine.document

import java.util.IdentityHashMap

/**
 * Releases resources that belong to an old state but are not retained by the
 * next state. Identity semantics are intentional: two distinct bitmap objects
 * may contain equal pixels but have independent native allocations.
 */
object BitmapOwnership {
    fun <T : Any> releaseUnretained(
        previous: Iterable<T>,
        retained: Iterable<T>,
        release: (T) -> Unit
    ) {
        val retainedByIdentity = IdentityHashMap<T, Boolean>()
        retained.forEach { retainedByIdentity[it] = true }

        val released = IdentityHashMap<T, Boolean>()
        previous.forEach { resource ->
            if (retainedByIdentity[resource] != true && released.put(resource, true) == null) {
                release(resource)
            }
        }
    }
}
