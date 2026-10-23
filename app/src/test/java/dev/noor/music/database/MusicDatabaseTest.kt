package dev.noor.music.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.noor.music.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MusicDatabaseTest {
    private lateinit var db: MusicDatabase
    private lateinit var dao: MusicDao

    private fun song(id: Long, title: String = "Song $id") =
        Song(
            "content://media/external/audio/media/$id",
            id,
            title,
            "Artist",
            "Album",
            10,
            180000,
            100,
            4000,
            "audio/mpeg",
            "$id.mp3",
            "Music/",
        )

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext<Context>(),
                    MusicDatabase::class.java,
                )
                .allowMainThreadQueries()
                .build()
        dao = db.music()
    }

    @After
    fun cleanup() {
        db.close()
    }

    @Test
    fun repeatedReactionsKeepPositionsSurviveRescanAndFilterWithoutDuplicateSongs() = runTest {
        val a = song(1)
        val b = song(2)
        dao.upsert(listOf(a, b))
        val first = SongReaction(songUri = a.uri, emoji = "🔥", x = .2f, y = .7f, rotation = -12f)
        val firstId = dao.addReaction(first)
        val secondId = dao.addReaction(first.copy(x = .8f))
        dao.addReaction(first.copy(songUri = b.uri, emoji = "💔"))
        dao.sync(listOf(a.copy(title = "New title"), b))
        val saved = dao.reactions().first()
        assertEquals(first.copy(id = firstId), saved.first())
        assertNotEquals(firstId, secondId)
        assertEquals(
            listOf(a.uri),
            dao.search("%", "TITLE", null, null, null, false, null, emoji = "🔥").first().map {
                it.uri
            },
        )
        dao.removeReaction(firstId)
        assertEquals(2, dao.reactions().first().size)
        assertEquals(
            listOf(a.uri),
            dao.search("%", "TITLE", null, null, null, false, null, emoji = "🔥").first().map {
                it.uri
            },
        )
        dao.remove(listOf(a.uri))
        assertEquals(listOf("💔"), dao.reactions().first().map { it.emoji })
    }

    @Test
    fun bulkCollectionAdditionSpansBindBatchesAndKeepsOrder() = runTest {
        val songs = (1L..650L).map(::song)
        dao.upsert(songs)
        val uris = songs.map { it.uri }
        val id = dao.createWithMembers(MusicCollection(name = "Bulk"), uris + uris.take(20))
        assertEquals(uris, dao.members(id))
        dao.add(id, listOf("content://media/missing") + uris)
        assertEquals(uris, dao.members(id))
    }

    @Test
    fun bulkQueueResolutionKeepsDuplicatesAndSkipsStaleUris() = runTest {
        val songs = (1L..650L).map(::song)
        dao.upsert(songs)
        val uris = songs.map { it.uri }
        val requested = uris + listOf("content://media/missing", uris[4])
        assertEquals(uris + uris[4], dao.resolveSongs(requested).map { it.uri })
    }

    @Test
    fun artworkKeysAdvanceForSameSecondEditsAndSurviveRescanning() = runTest {
        val s = song(1)
        dao.upsert(listOf(s))
        dao.bumpArtworkRevision(s.uri, 10000)
        val before = dao.song(s.uri)!!.artwork
        dao.sync(listOf(s.copy(revision = 5000)))
        assertEquals(before, dao.song(s.uri)!!.artwork)
        dao.bumpArtworkRevision(s.uri, 10000)
        assertNotEquals(before, dao.song(s.uri)!!.artwork)
        dao.sync(listOf(s.copy(revision = 20000)))
        assertEquals(20000L, dao.song(s.uri)!!.revision)
    }

    @Test
    fun albumsWithIdenticalNamesStaySeparateByMediaStoreId() = runTest {
        val first = song(1).copy(album = "Greatest Hits", albumId = 10)
        val second = song(2).copy(album = "Greatest Hits", albumId = 11)
        dao.upsert(listOf(first, second))
        val result = dao.search("%", "TITLE", null, null, null, false, null, albumKey = 10).first()
        assertEquals(listOf(first.uri), result.map { it.uri })
    }

    @Test
    fun staleSelectionCannotLeavePartiallyCreatedCollection() = runTest {
        val existing = song(1)
        dao.upsert(listOf(existing))
        try {
            dao.createWithMembers(
                MusicCollection(name = "Atomic"),
                listOf(existing.uri, "content://media/missing"),
            )
            fail("A stale selection must be rejected")
        } catch (_: IllegalArgumentException) {}
        assertTrue(dao.collections().first().isEmpty())
        assertEquals(existing, dao.song(existing.uri))
    }

    @Test
    fun deletingCollectionOnlyPreservesFilesAndOtherMemberships() = runTest {
        val s = song(1)
        dao.upsert(listOf(s))
        val first = dao.create(MusicCollection(name = "Gym"))
        val second = dao.create(MusicCollection(name = "Drive"))
        dao.add(first, listOf(s.uri))
        dao.add(second, listOf(s.uri))
        dao.deleteCollection(first)
        assertEquals(s, dao.song(s.uri))
        assertEquals(listOf(s.uri), dao.members(second))
        assertEquals(1, dao.collections().first().size)
    }

    @Test
    fun removingPhysicalFileReferenceCascadesToEveryCollection() = runTest {
        val s = song(1)
        dao.upsert(listOf(s))
        val first = dao.create(MusicCollection(name = "Gym"))
        val second = dao.create(MusicCollection(name = "Drive"))
        dao.add(first, listOf(s.uri))
        dao.add(second, listOf(s.uri))
        dao.remove(listOf(s.uri))
        assertTrue(dao.members(first).isEmpty())
        assertTrue(dao.members(second).isEmpty())
        assertEquals(2, dao.collections().first().size)
    }

    @Test
    fun rescanPreservesFavoriteHistoryAndMemberships() = runTest {
        val s = song(1)
        dao.upsert(listOf(s))
        dao.favorite(s.uri)
        dao.played(s.uri, 1234)
        val c = dao.create(MusicCollection(name = "Persian"))
        dao.add(c, listOf(s.uri))
        dao.sync(listOf(s.copy(title = "عنوان تازه")))
        val after = dao.song(s.uri)!!
        assertTrue(after.favorite)
        assertEquals(1, after.playCount)
        assertEquals(1234L, after.lastPlayed)
        assertEquals("عنوان تازه", after.title)
        assertEquals(listOf(s.uri), dao.members(c))
    }

    @Test
    fun completeScanPrunesOnlyMissingReferences() = runTest {
        val a = song(1)
        val b = song(2)
        dao.upsert(listOf(a, b))
        val c = dao.create(MusicCollection(name = "Mix"))
        dao.add(c, listOf(a.uri, b.uri))
        dao.sync(listOf(b))
        assertNull(dao.song(a.uri))
        assertEquals(listOf(b.uri), dao.members(c))
    }

    @Test
    fun duplicateAddsKeepOrderAndDoNotDuplicateFiles() = runTest {
        val songs = (1L..3).map(::song)
        dao.upsert(songs)
        val c = dao.create(MusicCollection(name = "Mix"))
        dao.add(c, listOf(songs[0].uri, songs[1].uri, songs[0].uri))
        dao.add(c, listOf(songs[1].uri, songs[2].uri))
        assertEquals(songs.map { it.uri }, dao.members(c))
        assertEquals(3, dao.snapshot().size)
    }

    @Test
    fun addingAfterRemovalAppendsAfterLastPosition() = runTest {
        val songs = (1L..4).map(::song)
        dao.upsert(songs)
        val c = dao.create(MusicCollection(name = "Mix"))
        dao.add(c, songs.take(3).map { it.uri })
        dao.removeMembers(c, listOf(songs[1].uri))
        dao.add(c, listOf(songs[3].uri))
        assertEquals(listOf(songs[0].uri, songs[2].uri, songs[3].uri), dao.members(c))
    }

    @Test
    fun reorderingIsPersistentAndRejectsInvalidMembership() = runTest {
        val songs = (1L..3).map(::song)
        dao.upsert(songs)
        val c = dao.create(MusicCollection(name = "Mix"))
        dao.add(c, songs.map { it.uri })
        val moved = CollectionOrder.move(songs.map { it.uri }, 0, 2)
        dao.reorder(c, moved)
        assertEquals(moved, dao.members(c))
        try {
            dao.reorder(c, listOf(songs[0].uri))
            fail("Invalid membership must be rejected")
        } catch (_: IllegalArgumentException) {}
        assertEquals(moved, dao.members(c))
    }

    @Test
    fun searchTreatsWildcardsAsLiteralAndFindsPersian() = runTest {
        dao.upsert(
            listOf(
                song(1, "100% love"),
                song(2, "1000 love"),
                song(3, "دل من"),
                song(4, "under_score"),
            )
        )
        suspend fun search(text: String) =
            dao.search(sqlSearch(text), "TITLE", null, null, null, false, null).first()
        assertEquals(listOf(1L), search("100%").map { it.mediaId })
        assertEquals(listOf(3L), search("دل").map { it.mediaId })
        assertEquals(listOf(4L), search("_").map { it.mediaId })
    }

    @Test
    fun queryCombinesFavoritesFolderAndCollection() = runTest {
        val a = song(1)
        val b = song(2).copy(folder = "Podcasts/")
        dao.upsert(listOf(a, b))
        dao.favorite(a.uri)
        val c = dao.create(MusicCollection(name = "Mix"))
        dao.add(c, listOf(a.uri, b.uri))
        assertEquals(
            listOf(a.uri),
            dao.search("%", "TITLE", "Music/", null, null, true, c).first().map { it.uri },
        )
    }

    @Test
    fun summariesAndSharedCountReflectUniquePhysicalSongs() = runTest {
        val a = song(1)
        val b = song(2)
        dao.upsert(listOf(a, b))
        val c = dao.create(MusicCollection(name = "One"))
        val d = dao.create(MusicCollection(name = "Two"))
        dao.add(c, listOf(a.uri, b.uri))
        dao.add(d, listOf(a.uri))
        assertEquals(1, dao.shared(c, listOf(a.uri, b.uri)))
        val summary = dao.collections().first().first { it.id == c }
        assertEquals(2, summary.count)
        assertEquals(360000L, summary.duration)
    }
}
