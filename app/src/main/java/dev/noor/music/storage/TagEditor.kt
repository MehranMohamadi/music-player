package dev.noor.music.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.noor.music.domain.Song
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.AndroidArtwork

data class SongTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val genre: String = "",
    val track: String = "",
    val year: String = "",
    val artwork: String? = null,
)

@Singleton
class TagEditor @Inject constructor(@ApplicationContext private val context: Context) {
    private val resolver = context.contentResolver

    init {
        org.jaudiotagger.tag.TagOptionSingleton.getInstance().setAndroid(true)
    }

    // Deliberately conservative: other formats remain fully playable but are not written.
    fun supported(song: Song) = song.format.lowercase() in setOf("mp3", "flac", "ogg")

    private fun copyIn(song: Song, target: File) {
        resolver.openInputStream(Uri.parse(song.uri))?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        } ?: error("The audio file is unavailable")
        require(target.length() > 0) { "The audio file is empty" }
    }

    suspend fun read(song: Song): SongTags =
        withContext(Dispatchers.IO) {
            if (!supported(song)) return@withContext SongTags(song.title, song.artist, song.album)
            val f = File(context.cacheDir, "read-${UUID.randomUUID()}.${song.format.lowercase()}")
            try {
                copyIn(song, f)
                val tag = AudioFileIO.read(f).tag
                fun value(key: FieldKey) = runCatching {
                    tag?.getFirst(key).orEmpty()
                }
                    .getOrDefault("")
                SongTags(
                    value(FieldKey.TITLE),
                    value(FieldKey.ARTIST),
                    value(FieldKey.ALBUM),
                    value(FieldKey.ALBUM_ARTIST),
                    value(FieldKey.GENRE),
                    value(FieldKey.TRACK),
                    value(FieldKey.YEAR),
                )
            } finally {
                f.delete()
            }
        }

    suspend fun crop(uri: Uri, zoom: Float, offsetX: Float = 0f, offsetY: Float = 0f): File =
        withContext(Dispatchers.IO) {
            val bitmap =
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) {
                    decoder,
                    info,
                    _ ->
                    val max = maxOf(info.size.width, info.size.height)
                    if (max > 2048)
                        decoder.setTargetSampleSize(((max + 2047) / 2048).coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            val side =
                (minOf(bitmap.width, bitmap.height) / zoom.coerceIn(1f, 3f))
                    .toInt()
                    .coerceAtLeast(1)
            val x =
                ((bitmap.width - side) / 2f + offsetX * (bitmap.width - side) / 2f)
                    .toInt()
                    .coerceIn(0, bitmap.width - side)
            val y =
                ((bitmap.height - side) / 2f + offsetY * (bitmap.height - side) / 2f)
                    .toInt()
                    .coerceIn(0, bitmap.height - side)
            val cropped = Bitmap.createBitmap(bitmap, x, y, side, side)
            val resized = Bitmap.createScaledBitmap(cropped, 768, 768, true)
            val file =
                File(context.filesDir, "covers/${UUID.randomUUID()}.jpg").apply {
                    parentFile?.mkdirs()
                }
            file.outputStream().use { resized.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            if (resized !== cropped) resized.recycle()
            if (cropped !== bitmap) cropped.recycle()
            bitmap.recycle()
            file
        }

    private fun duration(file: File): Long =
        MediaMetadataRetriever().use { r ->
            r.setDataSource(file.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: error("Cannot verify audio integrity")
        }

    private fun replace(uri: Uri, file: File) {
        val descriptor =
            resolver.openFileDescriptor(uri, "rwt") ?: error("Android did not grant write access")
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { out ->
            file.inputStream().use { it.copyTo(out) }
            out.flush()
            out.fd.sync()
        }
    }

    private fun audioDigest(file: File): ByteArray {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val track =
                (0 until extractor.trackCount).firstOrNull {
                    extractor
                        .getTrackFormat(it)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                } ?: error("No audio stream found")
            extractor.selectTrack(track)
            val hash = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
            var samples = 0
            while (true) {
                buffer.clear()
                val count = extractor.readSampleData(buffer, 0)
                if (count < 0) break
                buffer.position(0)
                buffer.limit(count)
                hash.update(buffer)
                samples++
                extractor.advance()
            }
            require(samples > 0) { "No readable audio samples" }
            return hash.digest()
        } finally {
            extractor.release()
        }
    }

    private fun digest(file: File): ByteArray {
        val hash = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest()
    }

    @Suppress("DEPRECATION")
    private suspend fun requestMediaScan(uri: Uri): Boolean {
        // DATA is used solely as input to Android's public scanner, never for file access.
        val path =
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: return false
        return withTimeoutOrNull(10000) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, scanned ->
                    if (continuation.isActive) continuation.resume(scanned != null)
                }
            }
        } ?: false
    }

    suspend fun write(song: Song, tags: SongTags) =
        withContext(Dispatchers.IO) {
            require(supported(song)) {
                "Safe tag writing is available for MP3, FLAC and OGG only. This file has not been modified."
            }
            require(tags.title.isNotBlank()) { "A title is required" }
            require(tags.track.isEmpty() || tags.track.toIntOrNull()?.let { it > 0 } == true) {
                "Track number must be positive"
            }
            require(tags.year.isEmpty() || tags.year.toIntOrNull()?.let { it in 1..9999 } == true) {
                "Enter a valid year"
            }
            val directory =
                File(context.filesDir, "tag-recovery/${UUID.randomUUID()}").apply { mkdirs() }
            val backup = File(directory, "original.${song.format.lowercase()}")
            val edited = File(directory, "edited.${song.format.lowercase()}")
            File(directory, "uri.txt").writeText(song.uri)
            var replacementStarted = false
            var keepRecovery = false
            try {
                copyIn(song, backup)
                backup.copyTo(edited)
                val before = duration(backup)
                val originalAudio = audioDigest(backup)
                val audio = AudioFileIO.read(edited)
                val tag = audio.tagOrCreateAndSetDefault
                applyTagFields(
                    tag,
                    mapOf(
                        FieldKey.TITLE to tags.title,
                        FieldKey.ARTIST to tags.artist,
                        FieldKey.ALBUM to tags.album,
                        FieldKey.ALBUM_ARTIST to tags.albumArtist,
                        FieldKey.GENRE to tags.genre,
                        FieldKey.TRACK to tags.track,
                        FieldKey.YEAR to tags.year,
                    ),
                )
                tags.artwork?.let { path ->
                    val image = File(path)
                    val art =
                        AndroidArtwork().apply {
                            binaryData = image.readBytes()
                            mimeType = "image/jpeg"
                            pictureType = 3
                            description = "Cover"
                            width = 768
                            height = 768
                        }
                    tag.deleteArtworkField()
                    tag.setField(art)
                }
                audio.commit()
                val checked = AudioFileIO.read(edited)
                require(checked.tag.getFirst(FieldKey.TITLE) == tags.title) {
                    "The edited title could not be verified"
                }
                require(kotlin.math.abs(duration(edited) - before) <= 1000) {
                    "Audio verification failed; the original has not been changed"
                }
                require(originalAudio.contentEquals(audioDigest(edited))) {
                    "Audio stream changed during tag editing; the original has not been modified"
                }
                replacementStarted = true
                replace(Uri.parse(song.uri), edited)
                // Re-read the provider, not just the temporary file.
                val verify = File(directory, "verify.${song.format.lowercase()}")
                copyIn(song, verify)
                require(digest(verify).contentEquals(digest(edited))) {
                    "Written file verification failed"
                }
            } catch (e: Exception) {
                if (replacementStarted) {
                    val rollback = runCatching {
                        replace(Uri.parse(song.uri), backup)
                        val restored = File(directory, "restored.${song.format.lowercase()}")
                        copyIn(song, restored)
                        require(digest(restored).contentEquals(digest(backup)))
                    }
                    if (rollback.isFailure) {
                        keepRecovery = true
                        error(
                            "Write failed and automatic recovery failed. Original retained at ${backup.absolutePath}. Do not delete application data. ${e.message}"
                        )
                    }
                }
                throw e
            } finally {
                if (!keepRecovery) directory.deleteRecursively()
            }
            // Indexing is separate from a verified physical write; MediaStore metadata
            // columns may be read-only. Let Android read the embedded tags itself.
            try {
                val indexed = requestMediaScan(Uri.parse(song.uri))
                coil.Coil.imageLoader(context).memoryCache?.clear()
                resolver.notifyChange(Uri.parse(song.uri), null)
                indexed
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }
}
