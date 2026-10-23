package dev.noor.music.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.noor.music.data.*
import dev.noor.music.domain.*
import dev.noor.music.playback.PlaybackConnection
import dev.noor.music.playback.SongWaveforms
import dev.noor.music.storage.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicViewModel
@Inject
constructor(
    val repo: MusicRepository,
    private val collectionActions: CollectionActions,
    val settings: Settings,
    val playback: PlaybackConnection,
    val tags: TagEditor,
    val waveforms: SongWaveforms,
    @ApplicationContext val context: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val preferences = settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())
    val all =
        combine(repo.all, preferences) { songs, p -> songs.filterNot { it.folder in p.excluded } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val collections =
        repo.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val query =
        MutableStateFlow(
            LibraryQuery(
                text = savedStateHandle["query.text"] ?: "",
                sort =
                    runCatching { SongSort.valueOf(savedStateHandle["query.sort"] ?: "TITLE") }
                        .getOrDefault(SongSort.TITLE),
                folder = savedStateHandle["query.folder"],
                artist = savedStateHandle["query.artist"],
                album = savedStateHandle["query.album"],
                favorites = savedStateHandle["query.favorites"] ?: false,
                collection = savedStateHandle["query.collection"],
                albumId = savedStateHandle["query.albumId"],
                emoji = savedStateHandle["query.emoji"],
            )
        )
    val currentSong =
        combine(repo.all, playback.flow.map { it.uri }.distinctUntilChanged()) { library, uri ->
                library.firstOrNull { it.uri == uri }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val songs =
        query
            .debounce(180)
            .flatMapLatest(repo::search)
            .combine(preferences) { songs, p -> songs.filterNot { it.folder in p.excluded } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reactions =
        repo.dao
            .reactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReaction(uri: String, emoji: String) = work {
        repo.dao.addReaction(
            SongReaction(
                songUri = uri,
                emoji = emoji,
                x = kotlin.random.Random.nextFloat(),
                y = kotlin.random.Random.nextFloat(),
                rotation = kotlin.random.Random.nextFloat() * 32f - 16f,
            )
        )
    }

    fun removeReaction(id: Long) = work { repo.dao.removeReaction(id) }

    val selection = MutableStateFlow<Set<String>>(emptySet())
    val scanning = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val deletedCollection = MutableStateFlow<Long?>(null)
    val deletePlan = MutableStateFlow<DeletePlan?>(null)
    val platformDelete = MutableStateFlow<List<String>?>(null)
    private var activeDelete: DeletePlan? = null
    private var deleteBatch = 0
    val editing = MutableStateFlow<Song?>(null)
    val editTags = MutableStateFlow(SongTags())
    val editDraft = MutableStateFlow(SongTags())
    val platformWrite = MutableStateFlow<String?>(null)
    private var pendingTags: SongTags? = null
    private var observer: Job? = null

    init {
        viewModelScope.launch {
            query.collect { q ->
                savedStateHandle["query.text"] = q.text
                savedStateHandle["query.sort"] = q.sort.name
                savedStateHandle["query.folder"] = q.folder
                savedStateHandle["query.artist"] = q.artist
                savedStateHandle["query.album"] = q.album
                savedStateHandle["query.favorites"] = q.favorites
                savedStateHandle["query.collection"] = q.collection
                savedStateHandle["query.albumId"] = q.albumId
                savedStateHandle["query.emoji"] = q.emoji
            }
        }
        playback.connect()
    }

    private fun work(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.value = e.message ?: "The operation failed. Please try again."
            }
        }
    }

    fun permissionGranted() {
        refresh()
        if (observer == null)
            observer = viewModelScope.launch { repo.changes.debounce(700).collect { refresh() } }
        work { settings.boolean("welcome", true) }
    }

    fun refresh() {
        if (scanning.value) return
        work {
            scanning.value = true
            try {
                repo.refresh()
                playback.reconcile(repo.dao.snapshot())
            } finally {
                scanning.value = false
            }
        }
    }

    fun select(uri: String) {
        selection.update { if (uri in it) it - uri else it + uri }
    }

    fun selectAll() {
        selection.value = songs.value.mapTo(linkedSetOf()) { it.uri }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun playCollection(id: Long, shuffle: Boolean = false) = work {
        val order = repo.dao.members(id)
        val list =
            repo.dao.resolveSongs(order).filterNot { it.folder in preferences.value.excluded }
        playback.play(list, if (shuffle && list.isNotEmpty()) list.indices.random() else 0, shuffle)
    }

    fun favorite(song: Song) = work { repo.dao.favorite(song.uri) }

    fun create(name: String, uris: List<String>) = work {
        collectionActions.create(name, uris)
        clearSelection()
    }

    fun add(id: Long, uris: List<String>) = work {
        collectionActions.add(id, uris)
        clearSelection()
        message.value = "Added to collection"
    }

    fun rename(id: Long, name: String) = work { collectionActions.rename(id, name) }

    fun cover(id: Long, uri: Uri) = work {
        val f = tags.crop(uri, 1f)
        repo.dao.cover(id, f.absolutePath)
    }

    fun remove(id: Long, uris: List<String>) = work {
        collectionActions.remove(id, uris)
        clearSelection()
    }

    fun reorder(id: Long, uri: String, direction: Int) = work {
        collectionActions.move(id, uri, direction)
    }

    fun deleteOnly(id: Long) = work {
        collectionActions.deleteOnly(id)
        if (query.value.collection == id) query.value = LibraryQuery()
        message.value = "Collection deleted. Audio files kept."
    }

    fun prepareDelete(uris: List<String>, collectionId: Long? = null) = work {
        if (uris.isNotEmpty()) deletePlan.value = repo.deletePlan(uris, collectionId)
    }

    fun prepareCollectionDelete(id: Long) = work {
        val uris = repo.dao.members(id)
        if (uris.isEmpty()) deleteOnly(id) else deletePlan.value = repo.deletePlan(uris, id)
    }

    fun confirmDelete() {
        val plan = deletePlan.value ?: return
        deletePlan.value = null
        activeDelete = plan
        deleteBatch = 0
        if (playback.flow.value.uri in plan.uris) playback.pause()
        platformDelete.value = plan.batches.firstOrNull()
    }

    fun deletionResult(approved: Boolean) = work {
        platformDelete.value = null
        val plan = activeDelete ?: return@work
        deleteBatch++
        if (approved && deleteBatch < plan.batches.size) {
            platformDelete.value = plan.batches[deleteBatch]
            return@work
        }
        activeDelete = null
        val complete = repo.reconcileDelete(plan, approved)
        refresh()
        clearSelection()
        if (complete) {
            deletedCollection.value = if (approved) plan.collectionId else null
            message.value = "Audio files deleted"
            if (
                approved && plan.collectionId != null && query.value.collection == plan.collectionId
            )
                query.value = LibraryQuery()
        } else
            message.value =
                "Deletion cancelled or incomplete. Remaining files and collection were kept; missing files were removed from all collections."
    }

    fun edit(song: Song) = work {
        editing.value = song
        editTags.value = SongTags(song.title, song.artist, song.album)
        editDraft.value = editTags.value
        busy.value = true
        try {
            editTags.value = tags.read(song)
            editDraft.value = editTags.value
        } catch (e: Exception) {
            editTags.value = SongTags(song.title, song.artist, song.album)
            editDraft.value = editTags.value
            throw e
        } finally {
            busy.value = false
        }
    }

    fun requestWrite(value: SongTags) {
        val song = editing.value ?: return
        if (!tags.supported(song)) {
            message.value =
                "Tag writing supports MP3, FLAC and OGG. This file has not been changed."
            return
        }
        pendingTags = value
        platformWrite.value = song.uri
    }

    fun writeResult(approved: Boolean) = work {
        platformWrite.value = null
        val value = pendingTags
        pendingTags = null
        val song = editing.value
        if (!approved || value == null || song == null) {
            message.value = "Save cancelled. No metadata changed."
            return@work
        }
        busy.value = true
        playback.pauseForFileEdit()
        var edited: Song? = null
        try {
            val indexed = tags.write(song, value)
            edited = song
            val refreshed =
                try {
                    repo.dao.bumpArtworkRevision(song.uri, System.currentTimeMillis())
                    repo.refresh()
                    repo.dao.song(song.uri)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message.value = "File metadata saved, but library refresh failed: ${e.message}"
                    null
                }
            if (refreshed != null) {
                edited = refreshed
                playback.reconcile(repo.dao.snapshot())
                message.value =
                    if (indexed) "File metadata saved and verified"
                    else
                        "File metadata saved. Android indexing is pending; rescan the library if details are stale."
            }
            editing.value = null
        } finally {
            playback.completeFileEdit(edited)
            busy.value = false
        }
    }

    fun crop(uri: Uri, zoom: Float, x: Float, y: Float, onReady: (String) -> Unit) = work {
        busy.value = true
        try {
            onReady(tags.crop(uri, zoom, x, y).absolutePath)
        } finally {
            busy.value = false
        }
    }

    fun boolean(key: String, value: Boolean) = work { settings.boolean(key, value) }

    fun language(value: String) = work { settings.language(value) }

    fun excluded(value: Set<String>) = work { settings.excluded(value) }
}
