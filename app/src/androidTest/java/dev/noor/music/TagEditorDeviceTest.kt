package dev.noor.music

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.noor.music.domain.Song
import dev.noor.music.storage.*
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Uses only app-owned, generated tones. Never edits or deletes the user's music. */
@RunWith(AndroidJUnit4::class)
class TagEditorDeviceTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val created = mutableListOf<Uri>()

    @After
    fun clean() {
        created.forEach { context.contentResolver.delete(it, null, null) }
    }

    private fun fixture(format: String): Song {
        val resolver = context.contentResolver
        val mime =
            when (format) {
                "mp3" -> "audio/mpeg"
                "flac" -> "audio/flac"
                else -> "audio/ogg"
            }
        val uri =
            resolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        "noor-test-${System.nanoTime()}.$format",
                    )
                    put(MediaStore.Audio.Media.MIME_TYPE, mime)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/NoorTests/")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                },
            )!!
        created.add(uri)
        InstrumentationRegistry.getInstrumentation().context.assets.open("tone.$format").use { input
            ->
            resolver.openOutputStream(uri)!!.use { input.copyTo(it) }
        }
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
            null,
            null,
        )
        return Song(
            uri.toString(),
            ContentUris.parseId(uri),
            "Original tone",
            "Noor tests",
            "",
            0,
            2000,
            0,
            0,
            mime,
            "tone.$format",
            "Music/NoorTests/",
        )
    }

    private fun audioDigest(uri: String): ByteArray {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            extractor.selectTrack(0)
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val hash = MessageDigest.getInstance("SHA-256")
            while (true) {
                buffer.clear()
                val count = extractor.readSampleData(buffer, 0)
                if (count < 0) break
                val data = ByteArray(count)
                buffer.position(0)
                buffer.get(data)
                hash.update(data)
                extractor.advance()
            }
            return hash.digest()
        } finally {
            extractor.release()
        }
    }

    private fun roundTrip(format: String, withArtwork: Boolean = false) = runBlocking {
        val song = fixture(format)
        val editor = TagEditor(context)
        val before = audioDigest(song.uri)
        val cover =
            if (withArtwork)
                java.io.File(context.cacheDir, "test-cover.jpg").also { file ->
                    val bitmap =
                        android.graphics.Bitmap.createBitmap(
                            64,
                            64,
                            android.graphics.Bitmap.Config.ARGB_8888,
                        )
                    bitmap.eraseColor(android.graphics.Color.GREEN)
                    file.outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)
                    }
                    bitmap.recycle()
                }
            else null
        editor.write(
            song,
            SongTags(
                artwork = cover?.absolutePath,
                title = "آهنگ آزمایشی",
                artist = "Noor",
                album = "Local",
                albumArtist = "Noor",
                genre = "Test",
                track = "2",
                year = "2026",
            ),
        )
        val read = editor.read(song)
        assertEquals("آهنگ آزمایشی", read.title)
        assertEquals("Noor", read.artist)
        assertEquals("Local", read.album)
        assertEquals("2", read.track)
        assertEquals("2026", read.year)
        assertArrayEquals(
            "Encoded audio must be unchanged by tag writing",
            before,
            audioDigest(song.uri),
        )
        if (cover != null) {
            val copied = java.io.File(context.cacheDir, "artwork-read.$format")
            try {
                context.contentResolver.openInputStream(Uri.parse(song.uri))!!.use { input ->
                    copied.outputStream().use { input.copyTo(it) }
                }
                val tag = org.jaudiotagger.audio.AudioFileIO.read(copied).tag
                assertArrayEquals(cover.readBytes(), tag.firstArtwork.binaryData)
                val rendered =
                    context.contentResolver.openInputStream(Uri.parse(song.artwork))!!.use {
                        android.graphics.BitmapFactory.decodeStream(it)
                    }
                assertNotNull(
                    "The artwork provider must read the edited file's actual cover",
                    rendered,
                )
                val center = rendered!!.getPixel(rendered.width / 2, rendered.height / 2)
                assertTrue(android.graphics.Color.green(center) > 180)
                assertTrue(android.graphics.Color.red(center) < 80)
                rendered.recycle()
            } finally {
                copied.delete()
                cover.delete()
            }
        }
    }

    @Test
    fun playbackQueueAndPositionRoundTrip() = runBlocking {
        val settings = dev.noor.music.data.Settings(context)
        val original = settings.playback()
        try {
            val saved =
                dev.noor.music.data.SavedPlayback(
                    listOf("content://media/1", "content://media/2"),
                    1,
                    12345,
                    true,
                    2,
                    1.25f,
                )
            settings.savePlayback(saved)
            assertEquals(saved, settings.playback())
            val positionOnly = saved.copy(position = 54321)
            settings.savePlayback(positionOnly)
            assertEquals(positionOnly, settings.playback())
        } finally {
            settings.savePlayback(original)
        }
    }

    @Test
    fun cancelledDeletionKeepsCollectionEvenIfFilesDisappearedElsewhere() = runBlocking {
        val song = fixture("mp3")
        val db =
            androidx.room.Room.inMemoryDatabaseBuilder(
                    context,
                    dev.noor.music.database.MusicDatabase::class.java,
                )
                .build()
        try {
            val dao = db.music()
            dao.upsert(listOf(song))
            val id = dao.create(dev.noor.music.domain.MusicCollection(name = "Keep me"))
            dao.add(id, listOf(song.uri))
            val repository =
                dev.noor.music.data.MusicRepository(
                    dao,
                    dev.noor.music.storage.MediaScanner(context),
                )
            val plan = repository.deletePlan(listOf(song.uri), id)
            context.contentResolver.delete(Uri.parse(song.uri), null, null)
            repository.reconcileDelete(plan, false)
            assertNull(dao.song(song.uri))
            val remaining = dao.collections().first()
            assertEquals(id, remaining.single().id)
        } finally {
            db.close()
        }
    }

    @Test fun mp3TagsRoundTripWithoutChangingAudio() = roundTrip("mp3")

    @Test fun flacTagsRoundTripWithoutChangingAudio() = roundTrip("flac")

    @Test fun oggTagsRoundTripWithoutChangingAudio() = roundTrip("ogg")

    @Test fun coverArtworkRoundTripsWithoutChangingAudio() = roundTrip("mp3", true)

    @Test
    fun unsupportedFormatIsRejectedBeforeWriting() = runBlocking {
        val editor = TagEditor(context)
        val song =
            Song(
                "content://media/external/audio/media/0",
                0,
                "x",
                "x",
                "x",
                0,
                0,
                0,
                0,
                "audio/wav",
                "tone.wav",
                "Music/",
            )
        assertFalse(editor.supported(song))
        try {
            editor.write(song, SongTags(title = "x"))
            fail("WAV must not be modified")
        } catch (_: IllegalArgumentException) {}
    }
}
