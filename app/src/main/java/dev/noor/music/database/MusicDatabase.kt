package dev.noor.music.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.noor.music.domain.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MusicDao {
    @Query("SELECT * FROM song_reactions ORDER BY id")
    abstract fun reactions(): Flow<List<SongReaction>>

    @Insert abstract suspend fun addReaction(reaction: SongReaction): Long

    @Query("DELETE FROM song_reactions WHERE id=:id") abstract suspend fun removeReaction(id: Long)

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE") abstract fun all(): Flow<List<Song>>

    @Query("SELECT * FROM songs") abstract suspend fun snapshot(): List<Song>

    @Query("SELECT * FROM songs WHERE uri=:uri") abstract suspend fun song(uri: String): Song?

    @Query(
        """SELECT songs.* FROM songs LEFT JOIN members ON songs.uri=members.songUri AND members.collectionId=:collection
        WHERE (:collection IS NULL OR members.collectionId IS NOT NULL)
        AND (:favorite=0 OR favorite=1) AND (:folder IS NULL OR folder=:folder)
        AND (:artist IS NULL OR artist=:artist) AND (:album IS NULL OR album=:album)
        AND (:albumKey IS NULL OR songs.albumId=:albumKey)
        AND (:emoji IS NULL OR EXISTS (SELECT 1 FROM song_reactions r WHERE r.songUri=songs.uri AND r.emoji=:emoji))
        AND (title LIKE :text ESCAPE '\' OR artist LIKE :text ESCAPE '\' OR album LIKE :text ESCAPE '\')
        ORDER BY CASE WHEN :collection IS NOT NULL THEN members.position END,
        CASE WHEN :sort='ADDED' THEN added END DESC,
        CASE WHEN :sort='DURATION' THEN duration END DESC,
        CASE WHEN :sort='ARTIST' THEN artist END COLLATE NOCASE,
        CASE WHEN :sort='ALBUM' THEN album END COLLATE NOCASE, title COLLATE NOCASE"""
    )
    abstract fun search(
        text: String,
        sort: String,
        folder: String?,
        artist: String?,
        album: String?,
        favorite: Boolean,
        collection: Long?,
        albumKey: Long? = null,
        emoji: String? = null,
    ): Flow<List<Song>>

    @Query("SELECT uri FROM songs WHERE uri IN (:uris)")
    abstract suspend fun existingUris(uris: List<String>): List<String>

    @Query("SELECT * FROM songs WHERE uri IN (:uris)")
    abstract suspend fun songsByUris(uris: List<String>): List<Song>

    @Transaction
    open suspend fun resolveSongs(uris: List<String>): List<Song> {
        val found = uris.distinct().chunked(500).flatMap { songsByUris(it) }.associateBy { it.uri }
        return uris.mapNotNull(found::get)
    }

    @Upsert abstract suspend fun upsert(songs: List<Song>)

    @Query("DELETE FROM songs WHERE uri IN (:uris)") abstract suspend fun remove(uris: List<String>)

    @Query(
        "UPDATE songs SET revision=CASE WHEN revision>=:time THEN revision+1 ELSE :time END WHERE uri=:uri"
    )
    abstract suspend fun bumpArtworkRevision(uri: String, time: Long)

    @Query("UPDATE songs SET favorite=NOT favorite WHERE uri=:uri")
    abstract suspend fun favorite(uri: String)

    @Query("UPDATE songs SET playCount=playCount+1, lastPlayed=:time WHERE uri=:uri")
    abstract suspend fun played(uri: String, time: Long)

    @Query(
        """SELECT c.id,c.name,c.cover,COUNT(s.uri) AS count,COALESCE(SUM(s.duration),0) AS duration,
        (SELECT s2.albumId FROM songs s2 JOIN members m2 ON s2.uri=m2.songUri WHERE m2.collectionId=c.id ORDER BY m2.position LIMIT 1) AS firstArtworkId
        FROM collections c LEFT JOIN members m ON c.id=m.collectionId LEFT JOIN songs s ON m.songUri=s.uri GROUP BY c.id ORDER BY c.name COLLATE NOCASE"""
    )
    abstract fun collections(): Flow<List<CollectionSummary>>

    @Insert abstract suspend fun create(c: MusicCollection): Long

    @Query("UPDATE collections SET name=:name WHERE id=:id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("UPDATE collections SET cover=:cover WHERE id=:id")
    abstract suspend fun cover(id: Long, cover: String?)

    @Query("DELETE FROM collections WHERE id=:id") abstract suspend fun deleteCollection(id: Long)

    @Query("SELECT songUri FROM members WHERE collectionId=:id ORDER BY position")
    abstract suspend fun members(id: Long): List<String>

    @Query("SELECT MAX(position) FROM members WHERE collectionId=:id")
    abstract suspend fun lastPosition(id: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMembers(m: List<CollectionMember>)

    @Query("DELETE FROM members WHERE collectionId=:id AND songUri IN (:uris)")
    abstract suspend fun removeMembers(id: Long, uris: List<String>)

    @Query("DELETE FROM members WHERE collectionId=:id") abstract suspend fun clearMembers(id: Long)

    @Query(
        "SELECT COUNT(DISTINCT songUri) FROM members WHERE collectionId!=:id AND songUri IN (:uris)"
    )
    abstract suspend fun shared(id: Long, uris: List<String>): Int

    @Transaction
    open suspend fun createWithMembers(collection: MusicCollection, uris: List<String>): Long {
        val unique = uris.distinct()
        require(unique.chunked(500).sumOf { existingUris(it).size } == unique.size) {
            "Some selected songs are missing. Rescan and try again."
        }
        val id = create(collection)
        add(id, unique)
        return id
    }

    @Transaction
    open suspend fun add(id: Long, uris: List<String>) {
        val existing = members(id)
        val known = existing.toHashSet()
        val requested = uris.distinct()
        val present = requested.chunked(500).flatMap { existingUris(it) }.toHashSet()
        val offset = (lastPosition(id) ?: -1) + 1
        insertMembers(
            requested
                .filter { it in present && it !in known }
                .mapIndexed { index, uri -> CollectionMember(id, uri, offset + index) }
        )
    }

    @Transaction
    open suspend fun reorder(id: Long, uris: List<String>) {
        require(uris.size == uris.distinct().size && uris.toSet() == members(id).toSet())
        clearMembers(id)
        insertMembers(uris.mapIndexed { index, uri -> CollectionMember(id, uri, index) })
    }

    @Transaction
    open suspend fun sync(scanned: List<Song>) {
        val old = snapshot().associateBy { it.uri }
        val current = scanned.map { s ->
            old[s.uri]?.let {
                s.copy(
                    favorite = it.favorite,
                    playCount = it.playCount,
                    lastPlayed = it.lastPlayed,
                    revision = maxOf(s.revision, it.revision),
                )
            } ?: s
        }
        upsert(current)
        val present = scanned.mapTo(hashSetOf()) { it.uri }
        old.keys.filterNot { it in present }.chunked(500).forEach { remove(it) }
    }
}

@Database(
    entities = [Song::class, MusicCollection::class, CollectionMember::class, SongReaction::class],
    version = 2,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun music(): MusicDao
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `song_reactions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `songUri` TEXT NOT NULL, `emoji` TEXT NOT NULL,
            `x` REAL NOT NULL, `y` REAL NOT NULL, `rotation` REAL NOT NULL,
            `createdAt` INTEGER NOT NULL,
            FOREIGN KEY(`songUri`) REFERENCES `songs`(`uri`) ON UPDATE NO ACTION ON DELETE CASCADE)"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_song_reactions_songUri` ON `song_reactions` (`songUri`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_song_reactions_emoji` ON `song_reactions` (`emoji`)"
            )
        }
    }
