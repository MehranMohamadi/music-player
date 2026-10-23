package dev.noor.music.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.noor.music.BuildConfig

@Entity(
    tableName = "songs",
    indices = [Index("title"), Index("artist"), Index("album"), Index("folder")],
)
data class Song(
    @PrimaryKey val uri: String,
    val mediaId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val added: Long,
    val size: Long,
    val mime: String,
    val displayName: String,
    val folder: String,
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val revision: Long = 0,
) {
    val artwork: String
        get() =
            "content://${BuildConfig.APPLICATION_ID}.artwork/$mediaId?revision=$revision&album=$albumId"

    val format: String
        get() = displayName.substringAfterLast('.', mime.substringAfter('/')).uppercase()
}

@Entity(tableName = "collections")
data class MusicCollection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cover: String? = null,
)

@Entity(
    tableName = "members",
    primaryKeys = ["collectionId", "songUri"],
    foreignKeys =
        [
            ForeignKey(
                entity = MusicCollection::class,
                parentColumns = ["id"],
                childColumns = ["collectionId"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = Song::class,
                parentColumns = ["uri"],
                childColumns = ["songUri"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("songUri")],
)
data class CollectionMember(val collectionId: Long, val songUri: String, val position: Int)

data class CollectionSummary(
    val id: Long,
    val name: String,
    val cover: String?,
    val count: Int,
    val duration: Long,
    val firstArtworkId: Long?,
)

enum class SongSort {
    TITLE,
    ARTIST,
    ALBUM,
    ADDED,
    DURATION,
}

data class LibraryQuery(
    val text: String = "",
    val sort: SongSort = SongSort.TITLE,
    val folder: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val favorites: Boolean = false,
    val collection: Long? = null,
    val albumId: Long? = null,
    val emoji: String? = null,
)

fun sqlSearch(text: String): String =
    "%" + text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

data class DeletePlan(val uris: List<String>, val collectionId: Long?, val sharedFiles: Int) {
    val batches: List<List<String>>
        get() = uris.distinct().chunked(500)
}

object CollectionOrder {
    fun move(uris: List<String>, from: Int, to: Int): List<String> {
        if (from !in uris.indices || to !in uris.indices) return uris
        return uris.toMutableList().apply { add(to, removeAt(from)) }
    }
}

/** Private, per-song decorations. Duplicate emoji are separate instances. */
@Entity(
    tableName = "song_reactions",
    foreignKeys =
        [
            ForeignKey(
                entity = Song::class,
                parentColumns = ["uri"],
                childColumns = ["songUri"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("songUri"), Index("emoji")],
)
data class SongReaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songUri: String,
    val emoji: String,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val centered: Boolean = false,
)
