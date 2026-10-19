package dev.noor.music.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.noor.music.common.*
import dev.noor.music.common.icons.automirrored.rounded.*
import dev.noor.music.common.icons.rounded.*
import dev.noor.music.domain.*

@Composable
fun WelcomeScreen(onAllow: () -> Unit, onSettings: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Box(
            Modifier.background(
                Brush.verticalGradient(
                    listOf(Color(0xFF173A2B), Color(0xFF121212), Color(0xFF121212))
                )
            )
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .safeDrawingPadding()
                    .padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        null,
                        tint = Green,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("noor", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                }
                Column {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1.15f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(230.dp).clip(CircleShape).background(Color(0xFF1C3428)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(170.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF294B39)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    null,
                                    tint = Green,
                                    modifier = Modifier.size(96.dp),
                                )
                            }
                        }
                    }
                    Label(tr("YOUR MUSIC. YOUR SPACE.", "موسیقی تو، دنیای تو."), color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tr("A little closer\nto your music.", "به موسیقی‌ات\nنزدیک‌تر شو."),
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 42.sp,
                        lineHeight = 46.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        tr(
                            "All the songs you love, right here on your device. No accounts. No connection. Just press play.",
                            "همهٔ آهنگ‌های محبوبت روی همین دستگاه. بدون حساب و اینترنت. فقط پخش کن.",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp,
                    )
                }
                Column {
                    Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(tr("Find my music", "پیدا کردن موسیقی"), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                    }
                    Text(
                        tr(
                            "Allow audio access to discover your local library. Your files stay on your device.",
                            "برای یافتن آهنگ‌ها دسترسی صوتی را بده. فایل‌ها روی دستگاه می‌مانند.",
                        ),
                        Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onSettings,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            tr(
                                "Permission denied? Open app settings",
                                "دسترسی رد شده؟ تنظیمات برنامه",
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (action != null)
            TextButton(onClick = onAction) {
                Text(action, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
    }
}

@Composable
private fun SongCarousel(songs: List<Song>, onPlay: (Song) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(songs.take(20), key = { it.uri }) { song ->
            Column(
                Modifier.width(142.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlay(song) }
                    .padding(bottom = 8.dp)
            ) {
                Art(song.artwork, song.album, Modifier.size(142.dp), description = song.album)
                Spacer(Modifier.height(10.dp))
                Text(
                    song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    songs: List<Song>,
    collections: List<CollectionSummary>,
    scanning: Boolean,
    onLibrary: (LibraryQuery, String) -> Unit,
    onCollections: () -> Unit,
    onCollection: (CollectionSummary) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onSettings: () -> Unit,
) {
    val recent =
        remember(songs) {
            songs.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(20)
        }
    val added = remember(songs) { songs.sortedByDescending { it.added }.take(20) }
    val frequent =
        remember(songs) {
            songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(20)
        }
    val artists = remember(songs) { songs.distinctBy { it.artist }.take(12) }
    val albums = remember(songs) { songs.distinctBy { it.albumId }.take(12) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(
                Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                if (MaterialTheme.colorScheme.background == Color(0xFF121212))
                                    Color(0xFF163D2B)
                                else MaterialTheme.colorScheme.primaryContainer,
                                Color.Transparent,
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label(tr("ON YOUR DEVICE", "روی دستگاه تو"), color = Green)
                        Text(
                            tr("Made for you.", "برای تو."),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                    ActionIcon(Icons.Rounded.Settings, tr("Settings", "تنظیمات"), onSettings)
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { onLibrary(LibraryQuery(favorites = true), "favorites") },
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF253B33),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Favorite, null, tint = Green)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                tr("Liked songs", "پسندیده‌ها"),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    Surface(
                        onClick = { onLibrary(LibraryQuery(), "songs") },
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF253B33),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.LibraryMusic, null, tint = Green)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                tr("All songs", "همهٔ آهنگ‌ها"),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
        if (scanning) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (songs.isEmpty()) item { EmptyLibrary(scanning) }
        if (recent.isNotEmpty()) {
            item { SectionTitle(tr("Back in rotation", "دوباره گوش بده")) }
            item { SongCarousel(recent) { onPlay(recent, recent.indexOf(it)) } }
        }
        item {
            SectionTitle(
                tr("Your collections", "مجموعه‌های تو"),
                tr("View all", "مشاهدهٔ همه"),
                onCollections,
            )
        }
        if (collections.isEmpty())
            item {
                Surface(
                    onClick = onCollections,
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AddCircleOutline,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                tr("Find a home for every mood", "برای هر حال‌وهوا یک مجموعه"),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Label(tr("Create your first collection", "اولین مجموعه‌ات را بساز"))
                        }
                    }
                }
            }
        else
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(collections, key = { it.id }) { c ->
                        CollectionCard(
                            c,
                            Modifier.width(150.dp),
                            onClick = { onCollection(c) },
                            onLongClick = { onCollection(c) },
                        )
                    }
                }
            }
        if (added.isNotEmpty()) {
            item { SectionTitle(tr("Fresh on your device", "تازه روی دستگاه")) }
            item { SongCarousel(added) { onPlay(added, added.indexOf(it)) } }
        }
        if (albums.isNotEmpty()) {
            item { SectionTitle(tr("Albums worth a listen", "آلبوم‌های شنیدنی")) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(albums, key = { it.albumId }) { s ->
                        Column(
                            Modifier.width(142.dp).clickable {
                                onLibrary(LibraryQuery(albumId = s.albumId), s.album)
                            }
                        ) {
                            Art(s.artwork, s.album, Modifier.size(142.dp))
                            Spacer(Modifier.height(8.dp))
                            TrackText(s.album, s.artist)
                        }
                    }
                }
            }
        }
        if (artists.isNotEmpty()) {
            item { SectionTitle(tr("Your artists", "هنرمندان تو")) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(artists, key = { it.artist }) { s ->
                        Column(
                            Modifier.width(126.dp).clickable {
                                onLibrary(LibraryQuery(artist = s.artist), s.artist)
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Art(s.artwork, s.artist, Modifier.size(120.dp), round = 60.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
        if (frequent.isNotEmpty()) {
            item { SectionTitle(tr("On repeat", "همیشه در حال پخش")) }
            item { SongCarousel(frequent) { onPlay(frequent, frequent.indexOf(it)) } }
        }
        item {
            Label(
                tr("ONLY YOUR MUSIC. ALWAYS WITH YOU.", "فقط موسیقی تو. همیشه همراهت."),
                Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
fun CollectionCard(
    c: CollectionSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(bottom = 8.dp)
    ) {
        Art(
            c.cover ?: c.firstArtworkId?.let { "content://media/external/audio/albumart/$it" },
            c.name,
            Modifier.fillMaxWidth().aspectRatio(1f),
            description = c.name,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            c.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Label("${c.count} " + tr("songs", "آهنگ") + " · " + time(c.duration))
    }
}

@Composable
fun EmptyLibrary(scanning: Boolean = false, noResults: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            if (noResults) Icons.Rounded.SearchOff else Icons.Rounded.MusicNote,
            null,
            Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (scanning) tr("Finding your music…", "در حال یافتن موسیقی…")
            else if (noResults) tr("No matches yet", "نتیجه‌ای پیدا نشد")
            else tr("Your next favorite is out there", "آهنگ محبوب بعدی‌ات منتظر توست"),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (noResults)
                tr(
                    "Try another title, artist, album or collection.",
                    "نام آهنگ، هنرمند، آلبوم یا مجموعهٔ دیگری را جستجو کن.",
                )
            else
                tr(
                    "Add audio files to your device, then pull down to refresh. Nothing is downloaded or streamed.",
                    "فایل‌های صوتی را به دستگاه اضافه کن، سپس برای تازه‌سازی پایین بکش. پخش اینترنتی وجود ندارد.",
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SongRow(
    song: Song,
    selected: Boolean,
    selectionMode: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(start = 20.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) Checkbox(selected, onCheckedChange = { onClick() })
        else Art(song.artwork, song.album, Modifier.size(52.dp), round = 8.dp)
        Spacer(Modifier.width(12.dp))
        TrackText(song.title, song.artist + " · " + song.format, Modifier.weight(1f), playing)
        if (onUp != null && onDown != null) {
            ActionIcon(Icons.Rounded.KeyboardArrowUp, tr("Move up", "بالا"), onUp)
            ActionIcon(Icons.Rounded.KeyboardArrowDown, tr("Move down", "پایین"), onDown)
        } else Label(time(song.duration), Modifier.padding(horizontal = 4.dp))
        if (!selectionMode)
            ActionIcon(Icons.Rounded.MoreVert, tr("Song actions", "گزینه‌های آهنگ"), onMore)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    query: LibraryQuery,
    selected: Set<String>,
    current: String?,
    grid: Boolean,
    scanning: Boolean,
    collection: CollectionSummary?,
    onPlay: (Int, Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onMore: (Song) -> Unit,
    onRefresh: () -> Unit,
    onGrid: () -> Unit,
    onSort: (SongSort) -> Unit,
    onCollectionMenu: () -> Unit,
    onReorder: (String, Int) -> Unit,
    onAddSongs: () -> Unit,
    choosing: Boolean = false,
    searchHeader: (@Composable () -> Unit)? = null,
) {
    var sortOpen by remember { mutableStateOf(false) }
    val total = remember(songs) { songs.sumOf { it.duration } }
    val header: @Composable () -> Unit = {
        Column {
            if (collection != null) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    Color.Transparent,
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Art(
                        collection.cover
                            ?: collection.firstArtworkId?.let {
                                "content://media/external/audio/albumart/$it"
                            },
                        collection.name,
                        Modifier.widthIn(max = 240.dp)
                            .fillMaxWidth(.58f)
                            .aspectRatio(1f)
                            .align(Alignment.CenterHorizontally),
                        round = 16.dp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(collection.name, style = MaterialTheme.typography.headlineLarge)
                    Label(
                        "${collection.count} " +
                            tr("songs", "آهنگ") +
                            " · " +
                            time(collection.duration)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onAddSongs) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Add songs", "افزودن آهنگ"))
                        }
                        Spacer(Modifier.weight(1f))
                        ActionIcon(
                            Icons.Rounded.MoreHoriz,
                            tr("Collection actions", "گزینه‌های مجموعه"),
                            onCollectionMenu,
                        )
                    }
                }
            }
            searchHeader?.invoke()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${songs.size} " + tr("songs", "آهنگ"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Label(time(total))
                }
                if (songs.isNotEmpty() && !choosing) {
                    ActionIcon(
                        Icons.Rounded.Shuffle,
                        tr("Shuffle all", "پخش تصادفی همه"),
                        { onPlay(0, true) },
                    )
                    FilledIconButton(
                        onClick = { onPlay(0, false) },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            tr("Play all", "پخش همه"),
                            Modifier.size(32.dp),
                        )
                    }
                }
            }
            if (collection == null) {
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        TextButton(onClick = { sortOpen = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (query.sort) {
                                    SongSort.TITLE -> tr("Title", "عنوان")
                                    SongSort.ARTIST -> tr("Artist", "هنرمند")
                                    SongSort.ALBUM -> tr("Album", "آلبوم")
                                    SongSort.ADDED -> tr("Newest first", "تازه‌ترین")
                                    SongSort.DURATION -> tr("Duration", "مدت")
                                }
                            )
                        }
                        DropdownMenu(sortOpen, { sortOpen = false }) {
                            SongSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (sort) {
                                                SongSort.TITLE -> tr("Title", "عنوان")
                                                SongSort.ARTIST -> tr("Artist", "هنرمند")
                                                SongSort.ALBUM -> tr("Album", "آلبوم")
                                                SongSort.ADDED -> tr("Newest first", "تازه‌ترین")
                                                SongSort.DURATION -> tr("Duration", "مدت")
                                            }
                                        )
                                    },
                                    onClick = {
                                        onSort(sort)
                                        sortOpen = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    ActionIcon(
                        if (grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                        tr("Toggle grid or list", "نمای فهرست یا شبکه"),
                        onGrid,
                    )
                }
            } else
                Label(
                    tr(
                        "Custom order · use the arrows to reorder",
                        "ترتیب دلخواه · با پیکان‌ها جابه‌جا کن",
                    ),
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            if (songs.isEmpty()) EmptyLibrary(scanning, query.text.isNotBlank())
        }
    }
    PullToRefreshBox(
        isRefreshing = scanning,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (grid && collection == null)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { header() }
                items(songs, key = { it.uri }) { song ->
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (selected.isNotEmpty()) onSelect(song.uri)
                                    else onPlay(songs.indexOf(song), false)
                                },
                                onLongClick = { onSelect(song.uri) },
                            )
                            .background(
                                if (song.uri in selected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = .15f)
                                else Color.Transparent
                            )
                    ) {
                        Art(song.artwork, song.album, Modifier.fillMaxWidth().aspectRatio(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrackText(
                                song.title,
                                song.artist,
                                Modifier.weight(1f),
                                song.uri == current,
                            )
                            if (selected.isNotEmpty())
                                Checkbox(song.uri in selected, { onSelect(song.uri) })
                            else
                                ActionIcon(
                                    Icons.Rounded.MoreVert,
                                    tr("Song actions", "گزینه‌های آهنگ"),
                                    { onMore(song) },
                                )
                        }
                    }
                }
            }
        else
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { header() }
                itemsIndexed(songs, key = { _, song -> song.uri }) { index, song ->
                    SongRow(
                        song,
                        song.uri in selected,
                        selected.isNotEmpty(),
                        song.uri == current,
                        onClick = {
                            if (selected.isNotEmpty()) onSelect(song.uri) else onPlay(index, false)
                        },
                        onLongClick = { onSelect(song.uri) },
                        onMore = { onMore(song) },
                        onUp =
                            if (collection != null && selected.isEmpty())
                                ({ onReorder(song.uri, -1) })
                            else null,
                        onDown =
                            if (collection != null && selected.isEmpty())
                                ({ onReorder(song.uri, 1) })
                            else null,
                    )
                }
            }
    }
}

@Composable
fun CollectionsScreen(
    collections: List<CollectionSummary>,
    onOpen: (CollectionSummary) -> Unit,
    onMenu: (CollectionSummary) -> Unit,
    onCreate: () -> Unit,
) {
    LazyVerticalGrid(
        GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(tr("A SOUNDTRACK FOR EVERY MOOD", "برای هر حال‌وهوا"))
                    Text(
                        tr("Your collections", "مجموعه‌های تو"),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                FilledTonalIconButton(onClick = onCreate) {
                    Icon(Icons.Rounded.Add, tr("Create collection", "ساخت مجموعه"))
                }
            }
        }
        if (collections.isEmpty())
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.padding(vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Rounded.LibraryAdd,
                        null,
                        Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        tr("Make it yours", "به سلیقهٔ خودت"),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        tr(
                            "Create a collection. Long-press songs to add them.",
                            "یک مجموعه بساز. برای افزودن، آهنگ‌ها را نگه دار.",
                        ),
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onCreate) { Text(tr("Create collection", "ساخت مجموعه")) }
                }
            }
        items(collections, key = { it.id }) { c ->
            Box {
                CollectionCard(c, onClick = { onOpen(c) }, onLongClick = { onMenu(c) })
                IconButton(
                    onClick = { onMenu(c) },
                    modifier =
                        Modifier.align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = .35f), CircleShape),
                ) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        tr("Collection actions", "گزینه‌های مجموعه"),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun GroupsScreen(songs: List<Song>, kind: String, onOpen: (LibraryQuery, String) -> Unit) {
    fun groupName(group: List<Song>): String =
        when (kind) {
            "artists" -> group.first().artist
            "folders" -> group.first().folder
            else -> group.first().album
        }
    val groups =
        remember(songs, kind) {
            songs
                .groupBy { song ->
                    when (kind) {
                        "artists" -> song.artist
                        "folders" -> song.folder
                        else -> song.albumId.toString()
                    }
                }
                .values
                .sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { group: List<Song> ->
                        groupName(group)
                    }
                )
        }
    LazyVerticalGrid(
        GridCells.Adaptive(if (kind == "folders") 260.dp else 150.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(
            groups,
            key = { group ->
                when (kind) {
                    "artists" -> group.first().artist
                    "folders" -> group.first().folder
                    else -> group.first().albumId.toString()
                }
            },
        ) { list ->
            val name = groupName(list)
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                    onOpen(
                        when (kind) {
                            "artists" -> LibraryQuery(artist = name)
                            "folders" -> LibraryQuery(folder = name)
                            else -> LibraryQuery(albumId = list.first().albumId)
                        },
                        name,
                    )
                }
            ) {
                Art(
                    if (kind == "folders") null else list.first().artwork,
                    name,
                    Modifier.fillMaxWidth().aspectRatio(if (kind == "folders") 2.5f else 1f),
                    round = if (kind == "artists") 100.dp else 12.dp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    name.ifBlank { tr("Device root", "ریشهٔ دستگاه") },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    (if (kind == "albums") list.first().artist + " · " else "") +
                        "${list.size} " +
                        tr("songs", "آهنگ"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (groups.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyLibrary() }
    }
}
