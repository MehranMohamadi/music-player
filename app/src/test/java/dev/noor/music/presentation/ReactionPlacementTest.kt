package dev.noor.music.presentation

import dev.noor.music.domain.SongReaction
import org.junit.Assert.assertEquals
import org.junit.Test

class ReactionPlacementTest {
    private val reaction =
        SongReaction(
            songUri = "song:1",
            emoji = "❤️",
            x = .25f,
            y = .75f,
            rotation = 0f,
            centered = true,
        )

    @Test
    fun centersStickerOnTapAndPreservesRelativePointAfterResize() {
        assertEquals(76f to 276f, reactionTopLeft(reaction, 400f, 400f, 48f))
        assertEquals(26f to 201f, reactionTopLeft(reaction, 200f, 300f, 48f))
    }

    @Test
    fun tapsAtCoverCornersKeepEntireStickerVisible() {
        assertEquals(0f to 0f, reactionTopLeft(reaction.copy(x = 0f, y = 0f), 400f, 400f, 48f))
        assertEquals(352f to 352f, reactionTopLeft(reaction.copy(x = 1f, y = 1f), 400f, 400f, 48f))
    }

    @Test
    fun legacyStickersStayAtTheirPreviousPositions() {
        assertEquals(88f to 228f, reactionTopLeft(reaction.copy(centered = false), 400f, 400f, 48f))
    }

    @Test
    fun tinyCoverNeverProducesNegativeOffsets() {
        assertEquals(0f to 0f, reactionTopLeft(reaction, 20f, 20f, 48f))
    }
}
