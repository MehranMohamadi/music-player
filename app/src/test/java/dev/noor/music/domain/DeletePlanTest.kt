package dev.noor.music.domain

import org.junit.Assert.*
import org.junit.Test

class DeletePlanTest {
    @Test
    fun physicalDeletionUsesUniqueBoundedBatches() {
        val uris = (1..1201).map { "content://media/external/audio/media/$it" }
        val plan = DeletePlan(uris + uris.take(30), 4, 10)
        assertEquals(listOf(500, 500, 201), plan.batches.map { it.size })
        assertEquals(uris, plan.batches.flatten())
    }

    @Test
    fun moveOutsideBoundsDoesNotChangeQueue() {
        val order = listOf("a", "b")
        assertEquals(order, CollectionOrder.move(order, 0, -1))
        assertEquals(order, CollectionOrder.move(order, 1, 2))
    }
}
