package dev.noor.music.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.noor.music.common.*
import dev.noor.music.common.icons.automirrored.rounded.*
import dev.noor.music.common.icons.rounded.*
import dev.noor.music.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(vm: MusicViewModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val all by vm.all.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val reactions by vm.reactions.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selected by vm.selection.collectAsStateWithLifecycle()
    val state by vm.playback.flow.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val delete by vm.deletePlan.collectAsStateWithLifecycle()
    val deletedCollection by vm.deletedCollection.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()
    val tags by vm.editTags.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var tab by rememberSaveable { mutableStateOf("songs") }
    var originScreen by rememberSaveable { mutableStateOf("home") }
    var originTab by rememberSaveable { mutableStateOf("songs") }
    var originSearch by rememberSaveable { mutableStateOf("") }
    var originSort by rememberSaveable { mutableStateOf("TITLE") }
    var title by rememberSaveable { mutableStateOf("") }
    var nowPlaying by rememberSaveable { mutableStateOf(false) }
    var queue by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var songMenu by remember { mutableStateOf<Song?>(null) }
    var info by remember { mutableStateOf<Song?>(null) }
    var collectionMenu by remember { mutableStateOf<CollectionSummary?>(null) }
    var deleteOnly by remember { mutableStateOf<CollectionSummary?>(null) }
    var addUris by remember { mutableStateOf<List<String>?>(null) }
    var createUris by remember { mutableStateOf<List<String>?>(null) }
    var rename by remember { mutableStateOf<CollectionSummary?>(null) }
    var addingTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var coverTarget by remember { mutableStateOf<Long?>(null) }
    val coverPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val id = coverTarget
            if (uri != null && id != null) vm.cover(id, uri)
            coverTarget = null
        }
    val scrollStates = rememberSaveableStateHolder()
    val snack = remember { SnackbarHostState() }
    val overlayMessage = message != null && (editing != null || nowPlaying || settings || queue)
    LaunchedEffect(message, overlayMessage) {
        message?.let {
            if (!overlayMessage) {
                snack.showSnackbar(it)
                if (vm.message.value == it) vm.message.value = null
            }
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            vm.message.value = it
            vm.playback.clearError()
        }
    }
    val current by vm.currentSong.collectAsStateWithLifecycle()
    val collection = collections.firstOrNull { it.id == query.collection }
    fun navigate(q: LibraryQuery, label: String) {
        if (screen != "detail") {
            originScreen = screen
            originTab = tab
            originSearch = query.text
            originSort = query.sort.name
        }
        vm.clearSelection()
        vm.query.value = q
        title = label
        screen = "detail"
    }
    fun openCollection(c: CollectionSummary) {
        addingTo = null
        navigate(LibraryQuery(collection = c.id), c.name)
    }
    fun library(kind: String) {
        screen = "library"
        tab = kind
        title = ""
        vm.clearSelection()
        vm.query.value = LibraryQuery()
        addingTo = null
    }
    fun back() {
        if (selected.isNotEmpty()) vm.clearSelection()
        else if (addingTo != null) {
            val c = collections.firstOrNull { it.id == addingTo }
            addingTo = null
            if (c != null) openCollection(c) else library("collections")
        } else if (screen == "detail") {
            screen = originScreen
            tab = originTab
            title = ""
            vm.query.value =
                LibraryQuery(
                    text = if (originScreen == "search") originSearch else "",
                    sort =
                        runCatching { SongSort.valueOf(originSort) }.getOrDefault(SongSort.TITLE),
                )
        } else {
            screen = "home"
            vm.query.value = LibraryQuery()
        }
    }
    LaunchedEffect(deletedCollection) {
        if (deletedCollection != null) {
            if (screen == "detail") library("collections")
            vm.deletedCollection.value = null
        }
    }
    BackHandler(selected.isNotEmpty() || screen != "home" || addingTo != null) { back() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                AnimatedVisibility(
                    state.uri != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                ) {
                    MiniPlayer(
                        state,
                        vm.playback.audioLevels,
                        { nowPlaying = true },
                        vm.playback::toggle,
                        vm.playback::next,
                    )
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = screen == "home",
                        onClick = {
                            screen = "home"
                            addingTo = null
                            vm.clearSelection()
                            vm.query.value = LibraryQuery()
                        },
                        icon = { Icon(Icons.Rounded.Home, tr("Home", "خانه")) },
                        label = { Text(tr("Home", "خانه")) },
                    )
                    NavigationBarItem(
                        selected = screen == "search",
                        onClick = {
                            screen = "search"
                            addingTo = null
                            vm.clearSelection()
                            vm.query.value = LibraryQuery()
                        },
                        icon = { Icon(Icons.Rounded.Search, tr("Search", "جستجو")) },
                        label = { Text(tr("Search", "جستجو")) },
                    )
                    NavigationBarItem(
                        selected = screen == "library" || screen == "detail",
                        onClick = { library(tab) },
                        icon = { Icon(Icons.Rounded.LibraryMusic, tr("Library", "کتابخانه")) },
                        label = { Text(tr("Your library", "کتابخانهٔ تو")) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selected.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ActionIcon(
                                Icons.Rounded.Close,
                                tr("Clear selection", "لغو انتخاب"),
                                vm::clearSelection,
                            )
                            Text(
                                "${selected.size} " + tr("selected", "انتخاب شده"),
                                Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = vm::selectAll) {
                                Text(tr("Select all", "انتخاب همه"))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    val id = addingTo
                                    if (id != null) {
                                        vm.add(id, selected.toList())
                                        collections
                                            .firstOrNull { it.id == id }
                                            ?.let(::openCollection)
                                    } else addUris = selected.toList()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null)
                                Text(tr("Add", "افزودن"))
                            }
                            OutlinedButton(onClick = { createUris = selected.toList() }) {
                                Text(tr("New collection", "مجموعهٔ جدید"))
                            }
                            if (query.collection != null)
                                OutlinedButton(
                                    onClick = { vm.remove(query.collection!!, selected.toList()) }
                                ) {
                                    Text(tr("Remove", "حذف از مجموعه"))
                                }
                            TextButton(onClick = { vm.prepareDelete(selected.toList()) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    tr("Delete files", "حذف فایل‌ها"),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            } else if (screen != "home") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (screen == "detail")
                        ActionIcon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            tr("Back", "بازگشت"),
                            { back() },
                        )
                    Text(
                        when (screen) {
                            "search" -> tr("Search", "جستجو")
                            "detail" ->
                                when (title) {
                                    "favorites" -> tr("Liked songs", "پسندیده‌ها")
                                    "songs" -> tr("All songs", "همهٔ آهنگ‌ها")
                                    else -> collection?.name ?: title
                                }
                            else -> tr("Your library", "کتابخانهٔ تو")
                        },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                    )
                    if (screen == "library")
                        ActionIcon(
                            Icons.Rounded.Add,
                            tr("Create collection", "ساخت مجموعه"),
                            { createUris = emptyList() },
                        )
                    ActionIcon(
                        Icons.Rounded.Settings,
                        tr("Settings", "تنظیمات"),
                        { settings = true },
                    )
                }
            }
            if (addingTo != null && selected.isEmpty())
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            tr(
                                "Tap songs to select, then add to collection",
                                "آهنگ‌ها را انتخاب کن، سپس به مجموعه بیفزا",
                            ),
                            Modifier.weight(1f),
                        )
                        TextButton(onClick = { back() }) { Text(tr("Cancel", "لغو")) }
                    }
                }
            if (screen == "library")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        listOf("songs", "collections", "emojis", "albums", "artists", "folders")
                    ) { kind ->
                        FilterChip(
                            selected = tab == kind,
                            onClick = { library(kind) },
                            label = {
                                Text(
                                    when (kind) {
                                        "songs" -> tr("Songs", "آهنگ‌ها")
                                        "collections" -> tr("Collections", "مجموعه‌ها")
                                        "emojis" -> tr("Emoji", "ایموجی‌ها")
                                        "albums" -> tr("Albums", "آلبوم‌ها")
                                        "artists" -> tr("Artists", "هنرمندان")
                                        else -> tr("Folders", "پوشه‌ها")
                                    }
                                )
                            },
                        )
                    }
                }
            if (screen == "search")
                OutlinedTextField(
                    query.text,
                    { vm.query.value = query.copy(text = it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            tr("Songs, artists, albums, collections", "آهنگ، هنرمند، آلبوم، مجموعه")
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.text.isNotEmpty())
                            ActionIcon(
                                Icons.Rounded.Close,
                                tr("Clear search", "پاک کردن جستجو"),
                                { vm.query.value = query.copy(text = "") },
                            )
                    },
                )
            Box(Modifier.weight(1f)) {
                scrollStates.SaveableStateProvider(
                    "$screen:$tab:${query.collection}:${query.folder}:${query.artist}:${query.album}:${query.albumId}:${query.favorites}:${query.emoji}"
                ) {
                    when {
                        screen == "home" ->
                            HomeScreen(
                                all,
                                collections,
                                scanning,
                                { q, label -> navigate(q, label) },
                                { library("collections") },
                                ::openCollection,
                                { list, index -> vm.playback.play(list, index) },
                                { settings = true },
                            )
                        screen == "library" && tab == "collections" ->
                            CollectionsScreen(
                                collections,
                                ::openCollection,
                                { collectionMenu = it },
                                { createUris = emptyList() },
                            )
                        screen == "library" && tab == "emojis" ->
                            EmojiGroupsScreen(all, reactions) { emoji ->
                                navigate(LibraryQuery(emoji = emoji), emoji)
                            }
                        screen == "library" && tab in listOf("albums", "artists", "folders") ->
                            GroupsScreen(all, tab, ::navigate)
                        else ->
                            SongsScreen(
                                songs,
                                query,
                                selected,
                                state.uri,
                                prefs.grid,
                                scanning,
                                if (screen == "detail") collection else null,
                                onPlay = { index, shuffle ->
                                    if (addingTo != null)
                                        songs.getOrNull(index)?.let { vm.select(it.uri) }
                                    else
                                        vm.playback.play(
                                            songs,
                                            if (shuffle) songs.indices.randomOrNull() ?: 0
                                            else index,
                                            shuffle,
                                        )
                                },
                                onSelect = vm::select,
                                onMore = { songMenu = it },
                                onRefresh = vm::refresh,
                                onGrid = { vm.boolean("grid", !prefs.grid) },
                                onSort = { vm.query.value = query.copy(sort = it) },
                                onCollectionMenu = { collectionMenu = collection },
                                onReorder = { uri, direction ->
                                    query.collection?.let { vm.reorder(it, uri, direction) }
                                },
                                onAddSongs = {
                                    addingTo = query.collection
                                    vm.query.value = LibraryQuery()
                                    title = "songs"
                                },
                                choosing = addingTo != null,
                                searchHeader =
                                    if (screen == "search")
                                        ({
                                            val matches = collections.filter {
                                                it.name.contains(query.text, true)
                                            }
                                            if (matches.isNotEmpty()) {
                                                Text(
                                                    tr("Collections", "مجموعه‌ها"),
                                                    Modifier.padding(20.dp),
                                                    style = MaterialTheme.typography.titleLarge,
                                                )
                                                LazyRow(
                                                    contentPadding =
                                                        PaddingValues(horizontal = 20.dp),
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(16.dp),
                                                ) {
                                                    items(matches, key = { it.id }) { c ->
                                                        CollectionCard(
                                                            c,
                                                            Modifier.width(120.dp),
                                                            { openCollection(c) },
                                                            { collectionMenu = c },
                                                        )
                                                    }
                                                }
                                            }
                                        })
                                    else null,
                            )
                    }
                }
            }
        }
    }
    if (nowPlaying && state.uri != null)
        NowPlaying(
            state,
            current,
            vm.playback,
            vm.waveforms,
            reactions.filter { it.songUri == state.uri },
            { emoji -> state.uri?.let { vm.addReaction(it, emoji) } },
            vm::removeReaction,
            { nowPlaying = false },
            { current?.let(vm::favorite) },
            { state.uri?.let { addUris = listOf(it) } },
            { queue = true },
            { songMenu = current },
        )
    if (queue) QueueSheet(state, vm.playback, { queue = false })
    if (settings) SettingsScreen(prefs, all, vm, { settings = false })
    songMenu?.let { s ->
        SongActions(
            s,
            { songMenu = null },
            {
                vm.favorite(s)
                songMenu = null
            },
            {
                addUris = listOf(s.uri)
                songMenu = null
            },
            {
                vm.playback.enqueue(listOf(s), true)
                songMenu = null
            },
            {
                vm.playback.enqueue(listOf(s))
                songMenu = null
            },
            {
                vm.edit(s)
                songMenu = null
            },
            {
                info = s
                songMenu = null
            },
            {
                vm.prepareDelete(listOf(s.uri))
                songMenu = null
            },
            onRemove =
                query.collection?.let { id ->
                    ({
                        vm.remove(id, listOf(s.uri))
                        songMenu = null
                    })
                },
        )
    }
    info?.let { FileInfo(it, { info = null }) }
    addUris?.let { uris ->
        AddCollectionSheet(
            collections,
            uris.size,
            { addUris = null },
            {
                vm.add(it, uris)
                addUris = null
            },
            {
                createUris = uris
                addUris = null
            },
        )
    }
    createUris?.let { uris ->
        NameDialog(
            tr("New collection", "مجموعهٔ جدید"),
            onDismiss = { createUris = null },
            onSave = {
                vm.create(it, uris)
                createUris = null
            },
        )
    }
    rename?.let { c ->
        NameDialog(
            tr("Rename collection", "تغییر نام مجموعه"),
            c.name,
            { rename = null },
            {
                vm.rename(c.id, it)
                rename = null
            },
        )
    }
    collectionMenu?.let { c ->
        ModalBottomSheet(onDismissRequest = { collectionMenu = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    c.name,
                    Modifier.padding(20.dp),
                    style = MaterialTheme.typography.headlineMedium,
                )
                MenuAction(
                    Icons.Rounded.PlayArrow,
                    tr("Play all", "پخش همه"),
                    {
                        vm.playCollection(c.id)
                        collectionMenu = null
                    },
                )
                MenuAction(
                    Icons.Rounded.Shuffle,
                    tr("Shuffle all", "پخش تصادفی همه"),
                    {
                        vm.playCollection(c.id, true)
                        collectionMenu = null
                    },
                )
                MenuAction(
                    Icons.Rounded.Edit,
                    tr("Rename collection", "تغییر نام مجموعه"),
                    {
                        rename = c
                        collectionMenu = null
                    },
                )
                MenuAction(
                    Icons.Rounded.AddPhotoAlternate,
                    tr("Choose cover", "انتخاب تصویر"),
                    {
                        coverTarget = c.id
                        coverPicker.launch("image/*")
                        collectionMenu = null
                    },
                )
                MenuAction(
                    Icons.Rounded.DeleteOutline,
                    tr("Delete collection only · keep files", "فقط حذف مجموعه · حفظ فایل‌ها"),
                    {
                        deleteOnly = c
                        collectionMenu = null
                    },
                )
                MenuAction(
                    Icons.Rounded.DeleteForever,
                    tr("Delete collection and music files", "حذف مجموعه و فایل‌های موسیقی"),
                    {
                        vm.prepareCollectionDelete(c.id)
                        collectionMenu = null
                    },
                    danger = true,
                )
            }
        }
    }
    deleteOnly?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteOnly = null },
            title = { Text(tr("Delete collection only?", "فقط مجموعه حذف شود؟")) },
            text = {
                Text(
                    tr(
                        "“${c.name}” will be removed. All audio files and other collections will be kept.",
                        "«${c.name}» حذف می‌شود. همهٔ فایل‌های صوتی و مجموعه‌های دیگر باقی می‌مانند.",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteOnly(c.id)
                        deleteOnly = null
                        library("collections")
                    }
                ) {
                    Text(tr("Delete collection", "حذف مجموعه"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteOnly = null }) { Text(tr("Cancel", "لغو")) }
            },
        )
    }
    delete?.let { DeleteConfirmation(it, { vm.deletePlan.value = null }, vm::confirmDelete) }
    editing?.let {
        EditSongScreen(it, tags, busy, vm.tags.supported(it), vm, { vm.editing.value = null })
    }
    if (overlayMessage)
        AlertDialog(
            onDismissRequest = { vm.message.value = null },
            title = { Text("Noor · نور") },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(message.orEmpty())
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.message.value = null }) { Text(tr("Done", "تمام")) }
            },
        )
}
