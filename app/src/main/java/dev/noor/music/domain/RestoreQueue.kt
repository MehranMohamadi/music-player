package dev.noor.music.domain

/** Original indices distinguish repeated occurrences of the same physical file. */
data class RestoredQueue(val songs: List<Song>, val index: Int, val canResumePosition: Boolean)

fun restoreQueue(uris: List<String>, savedIndex: Int, library: Map<String, Song>): RestoredQueue {
    val available = uris.mapIndexedNotNull { index, uri -> library[uri]?.let { index to it } }
    val index = available.indexOfFirst { it.first == savedIndex }
    return RestoredQueue(available.map { it.second }, index.coerceAtLeast(0), index >= 0)
}
