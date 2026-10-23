package dev.noor.music

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.noor.music.domain.Song
import dev.noor.music.playback.SongWaveforms
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaveformDeviceTest {
    @Test
    fun decodesRealFilesAndReadsPersistentCacheWithoutReopeningAudio() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (extension in listOf("mp3", "flac", "ogg")) {
            val file = File.createTempFile("waveform-test-", ".$extension", context.cacheDir)
            try {
                instrumentation.context.assets.open("tone.$extension").use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                val song =
                    Song(
                        Uri.fromFile(file).toString(),
                        0,
                        "Tone",
                        "Test",
                        "Test",
                        0,
                        2000,
                        0,
                        file.length(),
                        "audio/$extension",
                        file.name,
                        "Test",
                    )
                val waveform = withTimeout(60_000) { SongWaveforms(context).load(song) }
                assertEquals(96, waveform.size)
                assertTrue(waveform.all { it.isFinite() && it in 0f..1f })
                assertTrue("A real test tone must produce energy", waveform.count { it > .1f } > 80)
                assertTrue(file.delete())
                assertEquals(waveform, SongWaveforms(context).load(song))
            } finally {
                file.delete()
            }
        }
    }
}
