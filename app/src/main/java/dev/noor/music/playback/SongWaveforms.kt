package dev.noor.music.playback

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.noor.music.domain.Song
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Fixed-size RMS envelope: memory use is independent of the song's length. */
internal class WaveformBins(private val durationUs: Long, count: Int = 96) {
    private val energy = DoubleArray(count)
    private val samples = LongArray(count)

    fun add(timeUs: Long, sample: Double) {
        if (durationUs <= 0 || timeUs < 0 || timeUs >= durationUs || !sample.isFinite()) return
        val index = (timeUs.toDouble() / durationUs * energy.size).toInt().coerceIn(energy.indices)
        energy[index] += sample.coerceIn(-1.0, 1.0).let { it * it }
        samples[index]++
    }

    fun finish(): List<Float> {
        val rms =
            energy.indices.map { if (samples[it] == 0L) 0.0 else sqrt(energy[it] / samples[it]) }
        val maximum = rms.maxOrNull()?.coerceAtLeast(.0001) ?: 1.0
        return rms.map { sqrt(it / maximum).toFloat().coerceIn(0f, 1f) }
    }
}

/**
 * Decodes the file silently, off the main thread. Only amplitudes are cached in app-private
 * storage.
 */
@Singleton
class SongWaveforms @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun load(song: Song): List<Float> =
        withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "waveforms").apply { mkdirs() }
            val key =
                MessageDigest.getInstance("SHA-256")
                    .digest(
                        "v1:${song.uri}:${song.size}:${song.duration}:${song.revision}"
                            .toByteArray()
                    )
                    .joinToString("") { "%02x".format(it) }
            val cache = File(cacheDir, key)
            val cached = runCatching {
                DataInputStream(cache.inputStream()).use { input ->
                    require(input.readInt() == 96)
                    List(96) { input.readFloat().also { require(it.isFinite() && it in 0f..1f) } }
                }
            }
                .getOrNull()
            if (cached != null) return@withContext cached
            val result = decode(song)
            currentCoroutineContext().ensureActive()
            // Cache failures must not prevent displaying an already decoded waveform.
            runCatching {
                val temporary = File.createTempFile("envelope-", ".tmp", cacheDir)
                try {
                    DataOutputStream(temporary.outputStream()).use { output ->
                        output.writeInt(result.size)
                        result.forEach(output::writeFloat)
                    }
                    check(temporary.renameTo(cache))
                } finally {
                    temporary.delete()
                }
                cacheDir
                    .listFiles()
                    ?.filter { it != cache }
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(63)
                    ?.forEach { it.delete() }
            }
            result
        }

    private suspend fun decode(song: Song): List<Float> {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(song.uri), null)
            val track =
                (0 until extractor.trackCount).firstOrNull {
                    extractor
                        .getTrackFormat(it)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                } ?: error("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val duration =
                if (format.containsKey(MediaFormat.KEY_DURATION))
                    format.getLong(MediaFormat.KEY_DURATION)
                else song.duration * 1000
            require(duration > 0)
            val bins = WaveformBins(duration)
            val codec =
                MediaCodec.createDecoderByType(
                    requireNotNull(format.getString(MediaFormat.KEY_MIME))
                )
            decoder = codec
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            var lastActivity = System.nanoTime()
            while (true) {
                currentCoroutineContext().ensureActive()
                check(System.nanoTime() - lastActivity < 15_000_000_000L) {
                    "Audio decoder stalled"
                }
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val buffer = requireNotNull(codec.getInputBuffer(inputIndex))
                        val bytes = extractor.readSampleData(buffer, 0)
                        if (bytes < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, bytes, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                        lastActivity = System.nanoTime()
                    }
                }
                val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val output = codec.outputFormat
                    sampleRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    encoding =
                        if (output.containsKey(MediaFormat.KEY_PCM_ENCODING))
                            output.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        else AudioFormat.ENCODING_PCM_16BIT
                    require(
                        encoding == AudioFormat.ENCODING_PCM_16BIT ||
                            encoding == AudioFormat.ENCODING_PCM_FLOAT
                    )
                    require(sampleRate > 0 && channels > 0)
                } else if (outputIndex >= 0) {
                    try {
                        val pcm =
                            codec
                                .getOutputBuffer(outputIndex)
                                ?.duplicate()
                                ?.order(ByteOrder.LITTLE_ENDIAN)
                        if (
                            pcm != null &&
                                info.size > 0 &&
                                info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            pcm.position(info.offset)
                            pcm.limit(info.offset + info.size)
                            val bytesPerSample =
                                if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            var frame = 0L
                            while (pcm.remaining() >= bytesPerSample * channels) {
                                val time = info.presentationTimeUs + frame * 1_000_000L / sampleRate
                                repeat(channels) {
                                    bins.add(
                                        time,
                                        if (bytesPerSample == 4) pcm.float.toDouble()
                                        else pcm.short / 32768.0,
                                    )
                                }
                                frame++
                            }
                        }
                    } finally {
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                    lastActivity = System.nanoTime()
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            return bins.finish()
        } finally {
            decoder?.let {
                runCatching { it.stop() }
                it.release()
            }
            extractor.release()
        }
    }
}
