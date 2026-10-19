package dev.noor.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.noor.music.domain.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PlaybackState(
    val uri: String? = null,
    val title: String = "",
    val artist: String = "",
    val artwork: String? = null,
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = 0,
    val speed: Float = 1f,
    val queue: List<MediaItem> = emptyList(),
    val index: Int = 0,
    val error: String? = null,
)

@Singleton
class PlaybackConnection
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val audioLevels: AudioLevels,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val state = MutableStateFlow(PlaybackState())
    private var cachedQueue: List<MediaItem> = emptyList()

    private data class EditBookmark(val uri: String?, val index: Int, val position: Long)

    private var editBookmark: EditBookmark? = null
    val flow = state.asStateFlow()
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    init {
        scope.launch {
            while (isActive) {
                delay(500)
                update()
            }
        }
    }

    fun connect() {
        if (future != null) return
        val f =
            MediaController.Builder(
                    context,
                    SessionToken(context, ComponentName(context, MusicService::class.java)),
                )
                .setListener(
                    object : MediaController.Listener {
                        override fun onDisconnected(disconnected: MediaController) {
                            if (controller === disconnected) {
                                controller = null
                                future = null
                                cachedQueue = emptyList()
                                state.update { it.copy(playing = false) }
                            }
                        }
                    }
                )
                .buildAsync()
        future = f
        f.addListener(
            {
                runCatching { f.get() }
                    .onSuccess { c ->
                        controller = c
                        c.addListener(
                            object : Player.Listener {
                                override fun onEvents(player: Player, events: Player.Events) {
                                    update(events.contains(Player.EVENT_TIMELINE_CHANGED))
                                }

                                override fun onPlayerError(error: PlaybackException) {
                                    state.update {
                                        it.copy(
                                            error =
                                                error.localizedMessage
                                                    ?: "This file could not be played"
                                        )
                                    }
                                }
                            }
                        )
                        pending.toList().also { pending.clear() }.forEach { it(c) }
                        update(true)
                    }
                    .onFailure {
                        state.update { s -> s.copy(error = it.localizedMessage) }
                        future = null
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun update(refreshQueue: Boolean = false) {
        val c = controller ?: return
        if (refreshQueue || cachedQueue.size != c.mediaItemCount)
            cachedQueue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        val m = c.currentMediaItem
        state.value =
            PlaybackState(
                m?.mediaId,
                m?.mediaMetadata?.title?.toString().orEmpty(),
                m?.mediaMetadata?.artist?.toString().orEmpty(),
                m?.mediaMetadata?.artworkUri?.toString(),
                c.isPlaying,
                c.currentPosition.coerceAtLeast(0),
                c.duration.coerceAtLeast(0),
                c.shuffleModeEnabled,
                c.repeatMode,
                c.playbackParameters.speed,
                cachedQueue,
                c.currentMediaItemIndex,
                state.value.error,
            )
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action)
            ?: run {
                pending.add(action)
                connect()
            }
    }

    fun play(songs: List<Song>, index: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        withController {
            it.setMediaItems(songs.map(Song::mediaItem), index.coerceIn(songs.indices), 0)
            it.shuffleModeEnabled = shuffle
            it.prepare()
            it.play()
        }
    }

    fun toggle() = withController {
        if (it.isPlaying) it.pause()
        else {
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.play()
        }
    }

    fun pauseForFileEdit() = withController { c ->
        editBookmark =
            EditBookmark(c.currentMediaItem?.mediaId, c.currentMediaItemIndex, c.currentPosition)
        c.pause()
        c.stop()
    }

    fun completeFileEdit(song: Song?) = withController { c ->
        val bookmark = editBookmark
        editBookmark = null
        if (song == null) return@withController
        for (index in 0 until c.mediaItemCount) {
            if (c.getMediaItemAt(index).mediaId == song.uri)
                c.replaceMediaItem(index, song.mediaItem())
        }
        if (c.mediaItemCount > 0) {
            c.prepare()
            val index =
                if (
                    bookmark != null &&
                        bookmark.index in 0 until c.mediaItemCount &&
                        c.getMediaItemAt(bookmark.index).mediaId == bookmark.uri
                )
                    bookmark.index
                else
                    (0 until c.mediaItemCount).firstOrNull {
                        c.getMediaItemAt(it).mediaId == bookmark?.uri
                    }
            if (index != null) c.seekTo(index, bookmark?.position ?: 0)
        }
    }

    fun pause() = withController { it.pause() }

    fun next() = withController { it.seekToNextMediaItem() }

    fun previous() = withController { it.seekToPrevious() }

    fun seek(position: Long) = withController { it.seekTo(position) }

    fun shuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun repeat() = withController {
        it.repeatMode =
            when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
    }

    fun speed(value: Float) = withController { it.setPlaybackSpeed(value) }

    fun enqueue(songs: List<Song>, next: Boolean = false) = withController { c ->
        c.addMediaItems(
            if (next) c.currentMediaItemIndex + 1 else c.mediaItemCount,
            songs.map(Song::mediaItem),
        )
        c.prepare()
    }

    fun move(from: Int, to: Int) = withController {
        if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount)
            it.moveMediaItem(from, to)
    }

    fun remove(index: Int) = withController {
        if (index in 0 until it.mediaItemCount) it.removeMediaItem(index)
    }

    fun jump(index: Int) = withController {
        it.seekTo(index, 0)
        it.play()
    }

    fun reconcile(songs: List<Song>) = withController { c ->
        val library = songs.associateBy { it.uri }
        for (index in c.mediaItemCount - 1 downTo 0) {
            val item = c.getMediaItemAt(index)
            val song = library[item.mediaId]
            if (song == null) c.removeMediaItem(index)
            else if (
                item.mediaMetadata.title?.toString() != song.title ||
                    item.mediaMetadata.artist?.toString() != song.artist ||
                    item.mediaMetadata.albumTitle?.toString() != song.album ||
                    item.mediaMetadata.artworkUri?.toString() != song.artwork
            )
                c.replaceMediaItem(index, song.mediaItem())
        }
    }

    fun clearError() {
        state.update { it.copy(error = null) }
    }
}
