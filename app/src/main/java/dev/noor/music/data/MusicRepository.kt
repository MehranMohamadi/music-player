package dev.noor.music.data

import dev.noor.music.database.MusicDao
import dev.noor.music.domain.*
import dev.noor.music.storage.MediaScanner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class MusicRepository @Inject constructor(val dao: MusicDao, private val scanner: MediaScanner) :
    CollectionStore {
    private val scanLock = Mutex()
    val all = dao.all()
    val collections = dao.collections()
    val changes = scanner.changes

    fun search(q: LibraryQuery) =
        dao.search(
            sqlSearch(q.text),
            q.sort.name,
            q.folder,
            q.artist,
            q.album,
            q.favorites,
            q.collection,
            q.albumId,
            q.emoji,
        )

    suspend fun refresh() = scanLock.withLock { dao.sync(scanner.scan()) }

    override suspend fun createCollection(name: String, uris: List<String>): Long {
        require(name.trim().isNotEmpty())
        return dao.createWithMembers(MusicCollection(name = name.trim().take(100)), uris)
    }

    override suspend fun addToCollection(id: Long, uris: List<String>) {
        dao.add(id, uris)
    }

    override suspend fun renameCollection(id: Long, name: String) = dao.rename(id, name)

    override suspend fun removeFromCollection(id: Long, uris: List<String>) {
        uris.chunked(500).forEach { dao.removeMembers(id, it) }
    }

    override suspend fun orderedMembers(id: Long) = dao.members(id)

    override suspend fun orderCollection(id: Long, uris: List<String>) = dao.reorder(id, uris)

    override suspend fun removeCollection(id: Long) = dao.deleteCollection(id)

    suspend fun deletePlan(uris: List<String>, collectionId: Long?): DeletePlan {
        val unique = uris.distinct()
        return DeletePlan(
            unique,
            collectionId,
            unique.chunked(500).sumOf { dao.shared(collectionId ?: -1, it) },
        )
    }

    // Reconcile actual provider state even if Android reports cancellation or partial success.
    suspend fun reconcileDelete(plan: DeletePlan, approved: Boolean): Boolean = scanLock.withLock {
        val missing = scanner.missing(plan.uris)
        missing.chunked(500).forEach { dao.remove(it) }
        val complete = missing.size == plan.uris.size
        if (complete && approved && plan.collectionId != null)
            dao.deleteCollection(plan.collectionId)
        complete
    }
}
