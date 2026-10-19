package dev.noor.music.storage

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import java.io.FileNotFoundException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Private, read-only thumbnails; the audio content URI remains the playback source. */
class ArtworkProvider : ContentProvider() {
    private val workers =
        ThreadPoolExecutor(
                2,
                2,
                30,
                TimeUnit.SECONDS,
                ArrayBlockingQueue<Runnable>(32),
                { task -> Thread(task, "Noor artwork").apply { isDaemon = true } },
            )
            .apply { allowCoreThreadTimeOut(true) }

    override fun onCreate() = true

    override fun getType(uri: Uri) = "image/jpeg"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Artwork is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Artwork is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Artwork is read-only")

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Artwork is read-only")
        val id =
            uri.lastPathSegment?.toLongOrNull()?.takeIf { it > 0 }
                ?: throw FileNotFoundException("Invalid audio ID")
        val album = uri.getQueryParameter("album")?.toLongOrNull()
        val app = context?.applicationContext ?: throw FileNotFoundException("Artwork unavailable")
        val pipe = ParcelFileDescriptor.createPipe()
        try {
            workers.execute {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                        val embedded = runCatching {
                            MediaMetadataRetriever().use { reader ->
                                reader.setDataSource(
                                    app,
                                    ContentUris.withAppendedId(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        id,
                                    ),
                                )
                                reader.embeddedPicture
                            }
                        }
                            .getOrNull()
                        var bitmap: Bitmap? = null
                        try {
                            if (embedded != null) {
                                val bounds =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(embedded, 0, embedded.size, bounds)
                                var sample = 1
                                while (
                                    bounds.outWidth / sample > 1024 ||
                                        bounds.outHeight / sample > 1024
                                ) sample *= 2
                                bitmap =
                                    BitmapFactory.decodeByteArray(
                                        embedded,
                                        0,
                                        embedded.size,
                                        BitmapFactory.Options().apply { inSampleSize = sample },
                                    )
                            }
                            val image = bitmap
                            if (image != null)
                                image.compress(Bitmap.CompressFormat.JPEG, 92, output)
                            else if (album != null && album > 0) {
                                app.contentResolver
                                    .openInputStream(
                                        Uri.parse("content://media/external/audio/albumart/$album")
                                    )
                                    ?.use { it.copyTo(output) }
                            }
                        } finally {
                            bitmap?.recycle()
                        }
                    }
                } catch (_: Exception) {
                    // Missing/corrupt pictures produce Coil's normal artwork fallback.
                } catch (_: OutOfMemoryError) {
                    // An oversized embedded picture must not take down audio playback.
                }
            }
        } catch (_: RejectedExecutionException) {
            pipe.forEach { it.close() }
            throw FileNotFoundException("Artwork is busy; try again")
        }
        return pipe[0]
    }
}
