package dev.noor.music.presentation

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dev.noor.music.common.*
import dev.noor.music.common.icons.automirrored.rounded.*
import dev.noor.music.common.icons.rounded.*
import dev.noor.music.domain.*
import dev.noor.music.playback.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MiniPlayer(state: PlaybackState, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF254338),
        contentColor = Color.White,
    ) {
        Column {
            Row(
                Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Art(state.artwork, state.title, Modifier.size(44.dp), round = 6.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        state.artist,
                        color = Color.White.copy(alpha = .7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ActionIcon(
                    if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    tr("Play or pause", "پخش یا توقف"),
                    onToggle,
                    tint = Color.White,
                )
                ActionIcon(
                    Icons.Rounded.SkipNext,
                    tr("Next song", "آهنگ بعدی"),
                    onNext,
                    tint = Color.White,
                )
            }
            LinearProgressIndicator(
                progress = {
                    if (state.duration > 0)
                        (state.position.toFloat() / state.duration).coerceIn(0f, 1f)
                    else 0f
                },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Green,
                trackColor = Color.White.copy(alpha = .12f),
            )
        }
    }
}

@Composable
fun NowPlaying(
    state: PlaybackState,
    song: Song?,
    playback: PlaybackConnection,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onAdd: () -> Unit,
    onQueue: () -> Unit,
    onMore: () -> Unit,
) {
    val context = LocalContext.current
    var dominant by remember(state.artwork) { mutableStateOf(Color(0xFF254B3B)) }
    LaunchedEffect(state.artwork) {
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(state.artwork)
                            .allowHardware(false)
                            .size(128)
                            .build()
                    )
                val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                bitmap?.let { Palette.from(it).generate().getMutedColor(0xFF254B3B.toInt()) }
            }
                .getOrNull()
                ?.let { color ->
                    dominant = Color(color).copy(alpha = .55f).compositeOver(Color(0xFF121212))
                }
        }
    }
    val background by animateColorAsState(dominant, label = "album background")
    var seek by remember { mutableStateOf<Float?>(null) }
    var speedOpen by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF121212), contentColor = Color.White) {
            Column(
                Modifier.background(Brush.verticalGradient(listOf(background, Color(0xFF121212))))
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ActionIcon(
                        Icons.Rounded.KeyboardArrowDown,
                        tr("Close player", "بستن پخش‌کننده"),
                        onDismiss,
                        tint = Color.White,
                    )
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Label(
                            tr("PLAYING FROM YOUR LIBRARY", "پخش از کتابخانهٔ تو"),
                            color = Color.White.copy(alpha = .7f),
                        )
                        Text("noor", fontWeight = FontWeight.Bold)
                    }
                    ActionIcon(
                        Icons.Rounded.MoreHoriz,
                        tr("Song actions", "گزینه‌های آهنگ"),
                        onMore,
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.height(28.dp))
                Art(
                    state.artwork,
                    state.title,
                    Modifier.widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f),
                    round = 16.dp,
                    description = state.title,
                )
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            state.artist,
                            Modifier.padding(top = 4.dp),
                            color = Color.White.copy(alpha = .65f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ActionIcon(
                        if (song?.favorite == true) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        tr("Favorite", "پسندیدن"),
                        onFavorite,
                        tint = if (song?.favorite == true) Green else Color.White,
                        enabled = song != null,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Slider(
                    value =
                        seek
                            ?: state.position
                                .toFloat()
                                .coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { seek = it },
                    onValueChangeFinished = {
                        seek?.let { playback.seek(it.toLong()) }
                        seek = null
                    },
                    valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = .2f),
                        ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Label(
                        time(seek?.toLong() ?: state.position),
                        color = Color.White.copy(alpha = .65f),
                    )
                    Label(time(state.duration), color = Color.White.copy(alpha = .65f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIcon(
                        Icons.Rounded.Shuffle,
                        tr("Shuffle", "تصادفی"),
                        playback::shuffle,
                        tint = if (state.shuffle) Green else Color.White.copy(alpha = .7f),
                    )
                    ActionIcon(
                        Icons.Rounded.SkipPrevious,
                        tr("Previous", "قبلی"),
                        playback::previous,
                        tint = Color.White,
                    )
                    FilledIconButton(
                        onClick = playback::toggle,
                        modifier = Modifier.size(76.dp),
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                            ),
                    ) {
                        Icon(
                            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            tr("Play or pause", "پخش یا توقف"),
                            Modifier.size(42.dp),
                        )
                    }
                    ActionIcon(
                        Icons.Rounded.SkipNext,
                        tr("Next", "بعدی"),
                        playback::next,
                        tint = Color.White,
                    )
                    ActionIcon(
                        if (state.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        tr("Repeat mode", "حالت تکرار"),
                        playback::repeat,
                        tint = if (state.repeat != 0) Green else Color.White.copy(alpha = .7f),
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        TextButton(onClick = { speedOpen = true }) {
                            Text("${state.speed}×", color = Color.White)
                        }
                        DropdownMenu(speedOpen, { speedOpen = false }) {
                            listOf(.5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("$speed×") },
                                    onClick = {
                                        playback.speed(speed)
                                        speedOpen = false
                                    },
                                )
                            }
                        }
                    }
                    ActionIcon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        tr("Add to collection", "افزودن به مجموعه"),
                        onAdd,
                        tint = Color.White,
                    )
                    ActionIcon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        tr("Playback queue", "صف پخش"),
                        onQueue,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: PlaybackState, playback: PlaybackConnection, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                tr("Up next", "در ادامه"),
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
            LazyColumn(Modifier.heightIn(max = 560.dp)) {
                itemsIndexed(state.queue, key = { index, item -> "$index:${item.mediaId}" }) {
                    index,
                    item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { playback.jump(index) }
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Art(
                            item.mediaMetadata.artworkUri?.toString(),
                            item.mediaId,
                            Modifier.size(40.dp),
                            round = 6.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        TrackText(
                            item.mediaMetadata.title?.toString().orEmpty(),
                            item.mediaMetadata.artist?.toString().orEmpty(),
                            Modifier.weight(1f),
                            index == state.index,
                        )
                        ActionIcon(
                            Icons.Rounded.KeyboardArrowUp,
                            tr("Move up", "بالا"),
                            { playback.move(index, index - 1) },
                            enabled = index > 0,
                        )
                        ActionIcon(
                            Icons.Rounded.KeyboardArrowDown,
                            tr("Move down", "پایین"),
                            { playback.move(index, index + 1) },
                            enabled = index < state.queue.lastIndex,
                        )
                        ActionIcon(
                            Icons.Rounded.Close,
                            tr("Remove from queue", "حذف از صف"),
                            { playback.remove(index) },
                        )
                    }
                }
            }
        }
    }
}
