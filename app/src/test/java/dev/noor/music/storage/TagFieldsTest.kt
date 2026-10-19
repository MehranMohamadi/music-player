package dev.noor.music.storage

import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.junit.Assert.*
import org.junit.Test

class TagFieldsTest {
    @Test
    fun titleEditPreservesUndisplayedArtistValues() {
        val tag = ID3v24Tag()
        tag.setField(FieldKey.TITLE, "Before")
        tag.setField(FieldKey.ARTIST, "First artist")
        tag.addField(FieldKey.ARTIST, "Second artist")
        val original = tag.getAll(FieldKey.ARTIST).toList()
        assertEquals(listOf("First artist", "Second artist"), original)
        applyTagFields(
            tag,
            mapOf(FieldKey.TITLE to "After", FieldKey.ARTIST to tag.getFirst(FieldKey.ARTIST)),
        )
        assertEquals("After", tag.getFirst(FieldKey.TITLE))
        assertEquals(original, tag.getAll(FieldKey.ARTIST))
    }
}
