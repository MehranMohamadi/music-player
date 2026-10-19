package dev.noor.music.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A short history of actual decoded PCM energy, without recording microphone audio. */
@androidx.annotation.OptIn(UnstableApi::class)
@Singleton
class AudioLevels @Inject constructor() : TeeAudioProcessor.AudioBufferSink {
    private val levels = MutableStateFlow(List(12) { 0f })
    val flow = levels.asStateFlow()
    private var samplesPerWindow = 1
    private var encoding = C.ENCODING_INVALID
    private var samples = 0
    private var energy = 0.0

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.encoding = encoding
        samplesPerWindow = (sampleRateHz * channelCount / 30).coerceAtLeast(1)
        samples = 0
        energy = 0.0
        levels.value = List(12) { 0f }
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        // Analysis is unnecessary while the player UI is not visible. The tee never
        // modifies the samples sent to the audio output.
        if (levels.subscriptionCount.value == 0 || encoding != C.ENCODING_PCM_16BIT) return
        val pcm = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        while (pcm.remaining() >= 2) {
            val sample = pcm.short / 32768.0
            energy += sample * sample
            samples++
            if (samples >= samplesPerWindow) {
                val rms = sqrt(energy / samples)
                val height = (sqrt(rms) * 1.6).toFloat().coerceIn(0f, 1f)
                levels.value = levels.value.drop(1) + height
                samples = 0
                energy = 0.0
            }
        }
    }
}
