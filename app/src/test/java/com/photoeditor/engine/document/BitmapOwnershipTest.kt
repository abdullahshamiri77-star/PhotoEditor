package com.photoeditor.engine.document

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapOwnershipTest {
    @Test
    fun releasesOnlyResourcesNotRetainedByIdentity() {
        val retained = Any()
        val obsolete = Any()
        val released = mutableListOf<Any>()

        BitmapOwnership.releaseUnretained(
            previous = listOf(retained, obsolete, obsolete),
            retained = listOf(retained)
        ) { released += it }

        assertEquals(listOf(obsolete), released)
    }

    @Test
    fun releasesNothingWhenAllResourcesRemainReferenced() {
        val first = Any()
        val second = Any()
        val released = mutableListOf<Any>()

        BitmapOwnership.releaseUnretained(
            previous = listOf(first, second),
            retained = listOf(second, first)
        ) { released += it }

        assertEquals(emptyList<Any>(), released)
    }

    @Test
    fun releasesEveryDistinctObsoleteResourceExactlyOnce() {
        val first = Any()
        val second = Any()
        val released = mutableListOf<Any>()

        BitmapOwnership.releaseUnretained(
            previous = listOf(first, second, first, second),
            retained = emptyList()
        ) { released += it }

        assertEquals(2, released.size)
        assertEquals(true, released.any { it === first })
        assertEquals(true, released.any { it === second })
    }
}
