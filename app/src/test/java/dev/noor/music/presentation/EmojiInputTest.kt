package dev.noor.music.presentation

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class EmojiInputTest {
    @Test
    fun acceptsComplexEmojiAsSingleStickers() {
        listOf("🔥", "❤️", "👨‍👩‍👧‍👦", "🇮🇷", "👍🏽", "1️⃣", "©️").forEach {
            assertTrue(it, isSingleEmoji(it))
        }
    }

    @Test
    fun rejectsWordsAndMultipleStickersInCustomField() {
        listOf("", " ", "A", "سلام", "🔥🔥", "1", "a️").forEach {
            assertFalse(it, isSingleEmoji(it))
        }
    }
}
