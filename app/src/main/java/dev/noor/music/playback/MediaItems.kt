package dev.noor.music.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.noor.music.domain.Song

fun Song.mediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(uri)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(Uri.parse(artwork))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()
