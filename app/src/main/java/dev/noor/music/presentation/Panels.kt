package dev.noor.music.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.noor.music.common.*
import dev.noor.music.common.icons.automirrored.rounded.*
import dev.noor.music.common.icons.rounded.*
import dev.noor.music.data.Preferences
import dev.noor.music.domain.*
import dev.noor.music.storage.SongTags

@Composable
fun NameDialog(
    title: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { name = it.take(100) },
                label = { Text(tr("Collection name", "نام مجموعه")) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Text(tr("Save", "ذخیره"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "لغو")) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCollectionSheet(
    collections: List<CollectionSummary>,
    count: Int,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(bottom = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Text(
                tr("Add to collection", "افزودن به مجموعه"),
                Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
            Label(
                "$count " + tr("songs selected", "آهنگ انتخاب شده"),
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text(tr("New collection", "مجموعهٔ جدید")) },
                leadingContent = { Icon(Icons.Rounded.Add, null) },
                modifier = Modifier.clickable(onClick = onCreate),
            )
            collections.forEach { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = { Text("${c.count} " + tr("songs", "آهنگ")) },
                    leadingContent = {
                        Art(
                            c.cover
                                ?: c.firstArtworkId?.let {
                                    "content://media/external/audio/albumart/$it"
                                },
                            c.name,
                            Modifier.size(44.dp),
                        )
                    },
                    modifier = Modifier.clickable { onAdd(c.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActions(
    song: Song,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onAdd: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(song.artwork, song.album, Modifier.size(64.dp))
                Spacer(Modifier.width(16.dp))
                TrackText(song.title, song.artist, Modifier.weight(1f))
            }
            MenuAction(
                if (song.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                tr("Toggle favorite", "پسندیدن / لغو پسند"),
                onFavorite,
            )
            MenuAction(
                Icons.AutoMirrored.Rounded.PlaylistAdd,
                tr("Add to collection", "افزودن به مجموعه"),
                onAdd,
            )
            MenuAction(Icons.Rounded.SkipNext, tr("Play next", "پخش بعد از آهنگ فعلی"), onNext)
            MenuAction(
                Icons.AutoMirrored.Rounded.QueueMusic,
                tr("Add to queue", "افزودن به صف"),
                onQueue,
            )
            MenuAction(Icons.Rounded.Edit, tr("Edit song & artwork", "ویرایش آهنگ و تصویر"), onEdit)
            MenuAction(Icons.Rounded.Info, tr("File information", "اطلاعات فایل"), onInfo)
            if (onRemove != null)
                MenuAction(
                    Icons.Rounded.PlaylistRemove,
                    tr("Remove from collection", "حذف از مجموعه"),
                    onRemove,
                )
            MenuAction(
                Icons.Rounded.DeleteOutline,
                tr("Delete from device", "حذف از دستگاه"),
                onDelete,
                danger = true,
            )
        }
    }
}

@Composable
fun MenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    ListItem(
        headlineContent = {
            Text(
                text,
                color =
                    if (danger) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingContent = {
            Icon(
                icon,
                null,
                tint =
                    if (danger) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun DeleteConfirmation(plan: DeletePlan, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var acknowledged by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(tr("Permanently delete audio?", "حذف دائمی فایل‌های صوتی؟")) },
        text = {
            Column {
                Text(
                    tr(
                        "${plan.uris.distinct().size} physical files will be permanently removed from device storage. This cannot be undone.",
                        "${plan.uris.distinct().size} فایل فیزیکی برای همیشه از حافظهٔ دستگاه حذف می‌شود. این کار برگشت‌پذیر نیست.",
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    tr(
                        "Deleted songs will disappear from every collection. ${plan.sharedFiles} files are also used by other collections. Android will ask you to approve each batch.",
                        "آهنگ‌های حذف‌شده از تمام مجموعه‌ها حذف می‌شوند. ${plan.sharedFiles} فایل در مجموعه‌های دیگر هم استفاده شده‌اند. اندروید هر مرحله را تأیید می‌کند.",
                    )
                )
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { acknowledged = !acknowledged }
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(acknowledged, { acknowledged = it })
                    Text(
                        tr(
                            "I understand these files will be deleted",
                            "می‌دانم این فایل‌ها حذف می‌شوند",
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = acknowledged,
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(tr("Delete files", "حذف فایل‌ها"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Keep files", "نگه داشتن فایل‌ها")) }
        },
    )
}

@Composable
fun FileInfo(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("File information", "اطلاعات فایل")) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(song.displayName)
                Text(song.artist + " · " + song.album)
                Text(song.format + " · " + size(song.size) + " · " + time(song.duration))
                Text(song.mime)
                Text(song.folder)
                SelectionContainerCompat(song.uri)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Done", "تمام")) } },
    )
}

@Composable
private fun SelectionContainerCompat(text: String) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SettingsScreen(
    prefs: Preferences,
    songs: List<Song>,
    vm: MusicViewModel,
    onDismiss: () -> Unit,
) {
    var exclude by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIcon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        tr("Back", "بازگشت"),
                        onDismiss,
                    )
                    Text(tr("Settings", "تنظیمات"), style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    tr("Make yourself at home.", "اینجا خانهٔ توست."),
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.headlineLarge,
                )
                ListItem(
                    headlineContent = { Text(tr("Light theme", "پوستهٔ روشن")) },
                    supportingContent = { Text(tr("Dark is the default", "پیش‌فرض تیره است")) },
                    trailingContent = { Switch(prefs.light, { vm.boolean("light", it) }) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                Label(tr("LANGUAGE", "زبان"), Modifier.padding(start = 20.dp, top = 20.dp))
                listOf(
                        "system" to tr("System default", "زبان دستگاه"),
                        "en" to "English",
                        "fa" to "فارسی",
                    )
                    .forEach { (code, name) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            leadingContent = {
                                RadioButton(prefs.language == code, { vm.language(code) })
                            },
                            modifier = Modifier.clickable { vm.language(code) },
                        )
                    }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                MenuAction(
                    Icons.Rounded.Refresh,
                    tr("Rescan music library", "اسکن دوبارهٔ کتابخانه"),
                    vm::refresh,
                )
                MenuAction(
                    Icons.Rounded.FolderOff,
                    tr("Manage excluded folders", "مدیریت پوشه‌های مستثنی"),
                    { exclude = true },
                )
                ListItem(
                    headlineContent = { Text(tr("Local music storage", "حجم موسیقی محلی")) },
                    supportingContent = {
                        Text(
                            "${songs.size} " +
                                tr("visible songs", "آهنگ قابل مشاهده") +
                                " · " +
                                size(songs.sumOf { it.size })
                        )
                    },
                    leadingContent = { Icon(Icons.Rounded.Storage, null) },
                )
                MenuAction(Icons.Rounded.Info, tr("About Noor", "دربارهٔ نور"), { about = true })
            }
        }
    }
    if (exclude) {
        var chosen by remember { mutableStateOf(prefs.excluded) }
        val folders =
            remember(songs, prefs.excluded) {
                (songs.map { it.folder } + prefs.excluded).distinct().sorted()
            }
        AlertDialog(
            onDismissRequest = { exclude = false },
            title = { Text(tr("Exclude folders", "پوشه‌های مستثنی")) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        tr(
                            "Hidden from browsing. Files and collection memberships are kept.",
                            "از مرور پنهان می‌شوند. فایل‌ها و عضویت مجموعه‌ها باقی می‌مانند.",
                        )
                    )
                    folders.forEach { folder ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                chosen = if (folder in chosen) chosen - folder else chosen + folder
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                folder in chosen,
                                { chosen = if (it) chosen + folder else chosen - folder },
                            )
                            Text(folder.ifEmpty { tr("Device root", "ریشهٔ دستگاه") })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.excluded(chosen)
                        exclude = false
                    }
                ) {
                    Text(tr("Save", "ذخیره"))
                }
            },
            dismissButton = {
                TextButton(onClick = { exclude = false }) { Text(tr("Cancel", "لغو")) }
            },
        )
    }
    if (about)
        AlertDialog(
            onDismissRequest = { about = false },
            title = { Text("Noor · نور") },
            text = {
                Text(
                    tr(
                        "Version 1.0\nYour music, in its own light.\n\nAn offline player with no accounts, ads or network permission. Audio support depends on your device’s codecs. Tag writing is available for MP3, FLAC and OGG. Keep independent backups before editing files.",
                        "نسخهٔ ۱.۰\nموسیقی تو در نور خودش.\n\nپخش‌کنندهٔ آفلاین بدون حساب، تبلیغات یا مجوز اینترنت. پشتیبانی صوتی به دستگاه بستگی دارد. ویرایش برچسب برای MP3، FLAC و OGG ارائه می‌شود. پیش از ویرایش نسخهٔ پشتیبان تهیه کن.",
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { about = false }) { Text(tr("Done", "تمام")) }
            },
        )
}

@Composable
fun EditSongScreen(
    song: Song,
    initial: SongTags,
    busy: Boolean,
    supported: Boolean,
    vm: MusicViewModel,
    onDismiss: () -> Unit,
) {
    val draft by vm.editDraft.collectAsStateWithLifecycle()
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { cropUri = it }
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIcon(
                        Icons.Rounded.Close,
                        tr("Cancel edit", "لغو ویرایش"),
                        onDismiss,
                        enabled = !busy,
                    )
                    Text(
                        tr("Edit song", "ویرایش آهنگ"),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TextButton(
                        onClick = { vm.requestWrite(draft) },
                        enabled = !busy && supported && draft.title.isNotBlank(),
                    ) {
                        Text(tr("Save", "ذخیره"))
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Art(
                        draft.artwork ?: song.artwork,
                        song.title,
                        Modifier.size(180.dp).align(Alignment.CenterHorizontally),
                    )
                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        enabled = supported && !busy,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Choose & crop cover", "انتخاب و برش تصویر"))
                    }
                    if (!supported)
                        Text(
                            tr(
                                "This format can be played, but cannot be safely tagged here. No physical metadata will be changed. MP3, FLAC and OGG editing is supported.",
                                "این قالب پخش می‌شود اما ویرایش امن آن در این برنامه پشتیبانی نمی‌شود. هیچ اطلاعاتی در فایل تغییر نمی‌کند. ویرایش MP3، FLAC و OGG پشتیبانی می‌شود.",
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    TagField(tr("Title", "عنوان"), draft.title, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(title = it)
                    }
                    TagField(tr("Artist", "هنرمند"), draft.artist, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(artist = it)
                    }
                    TagField(tr("Album", "آلبوم"), draft.album, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(album = it)
                    }
                    TagField(tr("Album artist", "هنرمند آلبوم"), draft.albumArtist, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(albumArtist = it)
                    }
                    TagField(tr("Genre", "سبک"), draft.genre, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(genre = it)
                    }
                    TagField(tr("Track number", "شمارهٔ قطعه"), draft.track, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(track = it)
                    }
                    TagField(tr("Year", "سال"), draft.year, busy) {
                        vm.editDraft.value = vm.editDraft.value.copy(year = it)
                    }
                    OutlinedButton(onClick = { vm.editDraft.value = initial }, enabled = !busy) {
                        Text(tr("Reset unsaved edits", "بازنشانی تغییرات ذخیره‌نشده"))
                    }
                    Label(
                        tr(
                            "Saving changes the original audio file. Keep a separate backup. After Android approval, playback pauses during the edit.",
                            "ذخیره، فایل صوتی اصلی را تغییر می‌دهد. یک نسخهٔ پشتیبان جداگانه نگه دار. پس از تأیید اندروید، پخش هنگام ویرایش متوقف می‌شود.",
                        )
                    )
                }
            }
        }
    }
    cropUri?.let { uri ->
        CropDialog(
            uri,
            busy,
            onDismiss = { cropUri = null },
            onCrop = { zoom, x, y ->
                vm.crop(uri, zoom, x, y) {
                    vm.editDraft.value = vm.editDraft.value.copy(artwork = it)
                    cropUri = null
                }
            },
        )
    }
}

@Composable
private fun TagField(label: String, value: String, busy: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value,
        onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !busy,
    )
}

@Composable
fun CropDialog(
    uri: Uri,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCrop: (Float, Float, Float) -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(tr("Crop artwork", "برش تصویر")) },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(
                        uri,
                        null,
                        Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom),
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(x, y),
                    )
                }
                Label(tr("Zoom", "بزرگ‌نمایی"))
                Slider(zoom, { zoom = it }, valueRange = 1f..3f, enabled = !busy)
                Label(tr("Horizontal position", "موقعیت افقی"))
                Slider(x, { x = it }, valueRange = -1f..1f, enabled = !busy)
                Label(tr("Vertical position", "موقعیت عمودی"))
                Slider(y, { y = it }, valueRange = -1f..1f, enabled = !busy)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCrop(zoom, x, y) }, enabled = !busy) {
                Text(tr("Use cover", "استفاده از تصویر"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(tr("Cancel", "لغو")) }
        },
    )
}
