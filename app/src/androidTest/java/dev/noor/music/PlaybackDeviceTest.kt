package dev.noor.music

import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.noor.music.data.Settings
import dev.noor.music.database.MIGRATION_1_2
import dev.noor.music.database.MusicDatabase
import dev.noor.music.domain.Song
import dev.noor.music.playback.MusicService
import dev.noor.music.playback.mediaItem
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real service and decoder without creating an Activity. */
@RunWith(AndroidJUnit4::class)
class PlaybackDeviceTest {
    @Test
    fun serviceDecodesLocalAudioAndPublishesMediaNotification() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val resolver = context.contentResolver
        val settings = Settings(context)
        val saved = settings.playback()
        val uri =
            resolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        "noor-service-test-${System.nanoTime()}.mp3",
                    )
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/NoorTests/")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                },
            )!!
        val database =
            Room.databaseBuilder(context, MusicDatabase::class.java, "noor.db")
                .addMigrations(MIGRATION_1_2)
                .build()
        var controller: MediaController? = null
        try {
            instrumentation.context.assets.open("long-tone.mp3").use { input ->
                resolver.openOutputStream(uri)!!.use { input.copyTo(it) }
            }
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null,
            )
            val song =
                Song(
                    uri.toString(),
                    ContentUris.parseId(uri),
                    "Service integration tone",
                    "Noor tests",
                    "Test",
                    0,
                    30000,
                    0,
                    0,
                    "audio/mpeg",
                    "tone.mp3",
                    "Music/NoorTests/",
                )
            database.music().upsert(listOf(song))
            val future =
                AtomicReference<
                    com.google.common.util.concurrent.ListenableFuture<MediaController>
                >()
            instrumentation.runOnMainSync {
                future.set(
                    MediaController.Builder(
                            context,
                            SessionToken(context, ComponentName(context, MusicService::class.java)),
                        )
                        .buildAsync()
                )
            }
            val connected = future.get().get(90, TimeUnit.SECONDS)
            controller = connected
            val ready = CountDownLatch(1)
            val playing = CountDownLatch(1)
            instrumentation.runOnMainSync {
                connected.addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) ready.countDown()
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) playing.countDown()
                        }
                    }
                )
                connected.setMediaItems(listOf(song.mediaItem()))
                connected.prepare()
            }
            assertTrue("The actual decoder must become ready", ready.await(90, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { connected.play() }
            assertTrue(
                "The service must start playback without an Activity",
                playing.await(60, TimeUnit.SECONDS),
            )
            var advanced = false
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
            while (System.nanoTime() < deadline && !advanced) {
                instrumentation.runOnMainSync {
                    assertNull(connected.playerError)
                    advanced = connected.currentPosition > 100 && connected.duration > 1000
                }
                if (!advanced) Thread.sleep(100)
            }
            assertTrue("Decoded audio position must advance", advanced)
            instrumentation.runOnMainSync {
                assertEquals(uri.toString(), connected.currentMediaItem?.mediaId)
                connected.pause()
                connected.seekTo(500)
                connected.shuffleModeEnabled = true
                connected.repeatMode = Player.REPEAT_MODE_ONE
                assertFalse(connected.isPlaying)
                assertTrue(connected.shuffleModeEnabled)
                assertEquals(Player.REPEAT_MODE_ONE, connected.repeatMode)
            }
            val notifications =
                context.getSystemService(NotificationManager::class.java).activeNotifications
            assertTrue(
                "Media playback must publish a system notification",
                notifications.isNotEmpty(),
            )
        } finally {
            instrumentation.runOnMainSync {
                controller?.apply {
                    pause()
                    stop()
                    clearMediaItems()
                    release()
                }
                context.stopService(Intent(context, MusicService::class.java))
            }
            Thread.sleep(500)
            settings.savePlayback(saved)
            database.music().remove(listOf(uri.toString()))
            database.close()
            resolver.delete(uri, null, null)
        }
    }
}
