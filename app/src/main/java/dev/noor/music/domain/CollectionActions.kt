package dev.noor.music.domain

import javax.inject.Inject

/** Domain contract: collection operations never touch physical audio storage. */
interface CollectionStore {
    suspend fun createCollection(name: String, uris: List<String>): Long

    suspend fun addToCollection(id: Long, uris: List<String>)

    suspend fun renameCollection(id: Long, name: String)

    suspend fun removeFromCollection(id: Long, uris: List<String>)

    suspend fun orderedMembers(id: Long): List<String>

    suspend fun orderCollection(id: Long, uris: List<String>)

    suspend fun removeCollection(id: Long)
}

class CollectionActions @Inject constructor(private val store: CollectionStore) {
    private fun name(value: String) =
        value.trim().also {
            require(it.isNotEmpty()) { "A collection name is required" }
            require(it.length <= 100) { "Use at most 100 characters" }
        }

    suspend fun create(value: String, uris: List<String>) =
        store.createCollection(name(value), uris.distinct())

    suspend fun add(id: Long, uris: List<String>) = store.addToCollection(id, uris.distinct())

    suspend fun rename(id: Long, value: String) = store.renameCollection(id, name(value))

    suspend fun remove(id: Long, uris: List<String>) =
        store.removeFromCollection(id, uris.distinct())

    suspend fun move(id: Long, uri: String, direction: Int) {
        val current = store.orderedMembers(id)
        val from = current.indexOf(uri)
        val reordered = CollectionOrder.move(current, from, from + direction)
        if (reordered != current) store.orderCollection(id, reordered)
    }

    suspend fun deleteOnly(id: Long) = store.removeCollection(id)
}
