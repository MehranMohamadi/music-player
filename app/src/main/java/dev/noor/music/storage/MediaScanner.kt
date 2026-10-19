package dev.noor.music.storage

import android.content.*
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.noor.music.domain.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

@Singleton
class MediaScanner @Inject constructor(@ApplicationContext private val context: Context) {
    private val resolver = context.contentResolver
    val changes = callbackFlow {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
        resolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    suspend fun scan(): List<Song> =
        withContext(Dispatchers.IO) {
            val root = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val cols =
                arrayOf(
                    "_id",
                    "title",
                    "artist",
                    "album",
                    "album_id",
                    "duration",
                    "date_added",
                    "_size",
                    "mime_type",
                    "_display_name",
                    "relative_path",
                    "date_modified",
                )
            val rows =
                resolver.query(root, cols, "is_pending=0 AND is_trashed=0", null, null)
                    ?: error("MediaStore did not return a library. Try again.")
            rows.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        add(
                            Song(
                                ContentUris.withAppendedId(root, id).toString(),
                                id,
                                c.getString(1)?.takeIf { it.isNotBlank() }
                                    ?: c.getString(9).orEmpty(),
                                c.getString(2)?.takeUnless { it == "<unknown>" }
                                    ?: "Unknown artist",
                                c.getString(3) ?: "Unknown album",
                                c.getLong(4),
                                c.getLong(5),
                                c.getLong(6),
                                c.getLong(7),
                                c.getString(8) ?: "audio/*",
                                c.getString(9).orEmpty(),
                                c.getString(10).orEmpty(),
                                revision = c.getLong(11) * 1000,
                            )
                        )
                    }
                }
            }
        }

    suspend fun missing(uris: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            uris.filter { value ->
                resolver.query(Uri.parse(value), arrayOf("_id"), null, null, null)?.use {
                    !it.moveToFirst()
                } ?: false
            }
        }
}
