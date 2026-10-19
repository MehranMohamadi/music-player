package dev.noor.music.domain

import org.junit.Assert.*
import org.junit.Test

class RestoreQueueTest {
    private fun song(id: String) =
        Song(id, 1, id, "Artist", "Album", 1, 1000, 0, 100, "audio/mpeg", "tone.mp3", "Music/")

    @Test
    fun duplicateTrackRestoresItsOriginalOccurrenceAfterMissingFilesAreFiltered() {
        val a = song("a")
        val b = song("b")
        val restored = restoreQueue(listOf("a", "missing", "b", "a"), 3, mapOf("a" to a, "b" to b))
        assertEquals(listOf(a, b, a), restored.songs)
        assertEquals(2, restored.index)
        assertTrue(restored.canResumePosition)
    }

    @Test
    fun missingCurrentTrackStartsAnotherTrackFromItsBeginning() {
        val restored = restoreQueue(listOf("missing", "b"), 0, mapOf("b" to song("b")))
        assertEquals(0, restored.index)
        assertFalse(restored.canResumePosition)
    }

    @Test
    fun entirelyStaleQueueRemainsEmpty() {
        val restored = restoreQueue(listOf("gone"), 0, emptyMap())
        assertTrue(restored.songs.isEmpty())
        assertFalse(restored.canResumePosition)
    }
}
