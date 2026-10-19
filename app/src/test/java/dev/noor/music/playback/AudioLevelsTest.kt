package dev.noor.music.playback

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AudioLevelsTest {
    private fun tone(amplitude: Double, frames: Int = 1600): ByteBuffer =
        ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            repeat(frames) { frame ->
                val sample =
                    (sin(2 * PI * 440 * frame / 48000) * amplitude * 32767).toInt().toShort()
                putShort(sample)
                putShort(sample)
            }
            flip()
        }

    @Test
    fun realPcmEnergyTracksLoudnessAndSilenceWithoutConsumingPlaybackBuffer() = runTest {
        val meter = AudioLevels()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { meter.flow.collect {} }
        meter.flush(48000, 2, C.ENCODING_PCM_16BIT)
        val quiet = tone(.04)
        meter.handleBuffer(quiet)
        assertEquals(0, quiet.position())
        val quietLevel = meter.flow.value.last()
        meter.handleBuffer(tone(.5))
        assertTrue(meter.flow.value.last() > quietLevel)
        meter.handleBuffer(tone(0.0))
        assertEquals(0f, meter.flow.value.last(), 0f)
        assertTrue(meter.flow.value.all { it in 0f..1f })
    }

    @Test
    fun splitDecoderBuffersProduceSameEnergyAndFlushClearsHistory() = runTest {
        val whole = AudioLevels()
        val split = AudioLevels()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { whole.flow.collect {} }
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { split.flow.collect {} }
        whole.flush(48000, 2, C.ENCODING_PCM_16BIT)
        split.flush(48000, 2, C.ENCODING_PCM_16BIT)
        val buffer = tone(.25)
        whole.handleBuffer(buffer)
        val first = buffer.duplicate().apply { limit(3200) }
        val second = buffer.duplicate().apply { position(3200) }
        split.handleBuffer(first)
        assertTrue(split.flow.value.all { it == 0f })
        split.handleBuffer(second)
        assertEquals(whole.flow.value, split.flow.value)
        split.flush(44100, 1, C.ENCODING_PCM_16BIT)
        assertTrue(split.flow.value.all { it == 0f })
    }

    @Test
    fun hiddenUiDoesNotAnalyzeOrAdvanceTheAudioBuffer() {
        val meter = AudioLevels()
        meter.flush(48000, 2, C.ENCODING_PCM_16BIT)
        val buffer = tone(.8)
        meter.handleBuffer(buffer)
        assertEquals(0, buffer.position())
        assertTrue(meter.flow.value.all { it == 0f })
    }
}
