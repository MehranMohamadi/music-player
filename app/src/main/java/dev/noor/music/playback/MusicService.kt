package dev.noor.music.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.noor.music.MainActivity
import dev.noor.music.data.*
import dev.noor.music.domain.restoreQueue
import javax.inject.Inject
import kotlinx.coroutines.*

@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaLibraryService() {
    @Inject lateinit var repo: MusicRepository
    @Inject lateinit var settings: Settings
    @Inject lateinit var audioLevels: AudioLevels
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var counted: String? = null

    override fun onCreate() {
        super.onCreate()
        player =
            ExoPlayer.Builder(
                    this,
                    object : DefaultRenderersFactory(this) {
                        override fun buildAudioSink(
                            context: android.content.Context,
                            enableFloatOutput: Boolean,
                            enableAudioTrackPlaybackParams: Boolean,
                        ): AudioSink =
                            DefaultAudioSink.Builder(context)
                                .setEnableFloatOutput(false)
                                .setEnableAudioOutputPlaybackParameters(
                                    enableAudioTrackPlaybackParams
                                )
                                .setAudioProcessors(arrayOf(TeeAudioProcessor(audioLevels)))
                                .build()
                    },
                )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        session =
            MediaLibrarySession.Builder(this, player, LibraryCallback())
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                )
                .build()
        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    counted = null
                    record()
                    persist()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) record()
                    persist()
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    persist()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    persist()
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    persist()
                }
            }
        )
        scope.launch {
            val saved = settings.playback()
            val restored =
                restoreQueue(
                    saved.uris,
                    saved.index,
                    repo.dao.resolveSongs(saved.uris).associateBy { it.uri },
                )
            val items = restored.songs.map { it.mediaItem() }
            if (player.mediaItemCount == 0 && items.isNotEmpty()) {
                player.setMediaItems(
                    items,
                    restored.index,
                    if (restored.canResumePosition) saved.position.coerceAtLeast(0) else 0,
                )
                player.shuffleModeEnabled = saved.shuffle
                player.repeatMode = saved.repeat
                player.setPlaybackSpeed(saved.speed.coerceIn(0.5f, 2f))
                player.prepare()
            }
            while (isActive) {
                delay(5000)
                save()
            }
        }
    }

    private fun record() {
        val id = player.currentMediaItem?.mediaId ?: return
        if (player.isPlaying && counted != id) {
            counted = id
            scope.launch { repo.dao.played(id, System.currentTimeMillis()) }
        }
    }

    private fun persist() {
        scope.launch { save() }
    }

    private suspend fun save() {
        settings.savePlayback(
            SavedPlayback(
                (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
                player.currentMediaItemIndex.coerceAtLeast(0),
                player.currentPosition.coerceAtLeast(0),
                player.shuffleModeEnabled,
                player.repeatMode,
                player.playbackParameters.speed,
            )
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        session.release()
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        scope.launch {
            try {
                result.set(block())
            } catch (e: Exception) {
                result.setException(e)
            }
        }
        return result
    }

    inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            if (controller.packageName != packageName && !controller.isTrusted)
                return MediaSession.ConnectionResult.reject()
            return super.onConnect(session, controller)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(
                LibraryResult.ofItem(
                    MediaItem.Builder()
                        .setMediaId("root")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Noor · On your device")
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .build()
                        )
                        .build(),
                    params,
                )
            )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val songs = if (parentId == "root") repo.dao.snapshot() else emptyList()
            val start = (page.toLong() * pageSize).coerceIn(0, songs.size.toLong()).toInt()
            LibraryResult.ofItemList(
                songs.drop(start).take(pageSize.coerceIn(0, 500)).map { it.mediaItem() },
                params,
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = future {
            repo.dao.song(mediaId)?.let { LibraryResult.ofItem(it.mediaItem(), null) }
                ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = future {
            repo.dao.resolveSongs(mediaItems.map { it.mediaId }).map { it.mediaItem() }
        }
    }
}
