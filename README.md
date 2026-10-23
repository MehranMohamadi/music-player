# Noor · نور

A native, offline Android music player. Kotlin, Compose Material 3, Media3, Room, DataStore, Hilt, Coroutines/Flow, and Coil. Original branding with a dark green design, English/Persian interface, and RTL layouts. No accounts, backend, ads, streaming, or `INTERNET` permission.

## Download / دانلود

[Download Noor.apk from Releases](https://github.com/MehranMohamadi/music-player/releases/latest) · [Direct APK](https://github.com/MehranMohamadi/music-player/releases/latest/download/Noor.apk)

فایل `Noor.apk` را از بخش Assets دانلود کنید. اندروید ۱۱ یا بالاتر لازم است. این نسخه با کلید آزمایشی امضا شده است.

The exact optimized test-signed APK is also preserved in [distribution/Noor.apk](distribution/Noor.apk) with its checksum and validation notes. Tag pushes publish these verified bytes through the repository's release workflow; signing keys are never committed.

Version 1.0.1 gives Up Next a stable expandable sheet with a separately scrolling queue. The mini player's waveform shows recent energy from the decoded music PCM at up to 30 updates/second, without microphone permission. Analysis sleeps when the UI is not observing it, and the bars settle when playback pauses. The visualization is a loudness history, not a frequency equalizer.

Version 1.1.0 adds a gray waveform seek bar in Now Playing: real decoded file amplitudes turn green up to the current playback position. Analysis runs off the main thread, is cancelled when changing tracks or closing the player, and caches at most 64 small envelopes locally. Until analysis completes (or for unsupported files), the normal seek control remains usable with a status message.

Tap **☺ +** on the cover for ten emoji choices or enter a single emoji from the keyboard. Each tap adds an independent animated sticker at a random position; duplicates and custom emoji have no imposed count limit. Stickers keep their position and rotation across app restarts and library rescans. Tap a sticker to remove just that copy. **Library → Emoji** groups songs by their stickers and lists each matching song once. All reactions live in the phone's private Room database; they do not modify embedded artwork or upload data. Updating from database v1 preserves existing songs, favorites, history, collections and order. Clearing app data or uninstalling removes these local reactions.

## Build and run

- Android Studio with Android SDK **36** and Build Tools **35.0.0** (AGP's pinned default). Build Tools 36.0.0 is also installed in the cloud workspace for APK inspection.
- JDK **17 or 21** (the cloud build uses JDK 21).
- Android **11 / API 30** or newer.

Open this directory in Android Studio, let Gradle sync, and run `app` on a device. The SDK path belongs in your untracked `local.properties` (`sdk.dir=...`) or `ANDROID_HOME`.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
# With a connected API 30+ device/emulator:
./gradlew connectedDebugAndroidTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Gradle 8.13 is pinned by the wrapper with an official SHA-256 checksum. Only 41 used Material icon vectors are bundled, with upstream Apache license headers, avoiding the full extended icon pack. All dependency versions are pinned stable releases; there are no dynamic or preview versions. A production release needs your own signing configuration; signing keys are deliberately not included.

In this prepared cloud workspace:

```sh
source /workspace/toolchains/noor-env.sh
cd /workspace/music-player
./gradlew assembleDebug testDebugUnitTest lintDebug
```

That environment helper supplies the installed JDK/SDK, writable caches, and this machine's required proxy and CA trust. It is outside the source checkout; normal Android Studio builds do not need it. Never disable TLS verification to resolve dependency access.

## Use the app

Grant audio access on the welcome screen. On Android 13+ the app requests `READ_MEDIA_AUDIO`; Android 11–12 use `READ_EXTERNAL_STORAGE`. Denying access leaves an explanation and an app-settings shortcut. No notification permission is required for Android's exempt media-session notification.

Home shows recently played, liked songs, recent additions, collections, albums, artists, and frequent plays. Empty sections appear when there is relevant listening history. Albums are keyed by MediaStore ID, keeping identical titles separate. There are no fabricated songs or stock album covers. Missing artwork uses an original color-based graphic. Song covers come from a private read-only provider that extracts embedded pictures on at most two background workers; editing a cover advances its cache revision.

The library offers songs, albums, artists, folders, list/grid layouts, sorting, and pull-to-refresh. Search matches local titles, artists, and albums through debounced Room queries and also finds collections. MediaStore changes trigger a background rescan. Excluded folders are hidden from browsing without deleting their files or collection memberships.

Long-press a song for multi-selection. Use the toolbar to select all, add to a collection, create a collection, remove membership, or request physical deletion. A collection's menu supports rename, a custom cover, play/shuffle, and the two separate deletion actions. The up/down controls in collection details persist song order. Adding a song to several collections never copies the audio file.

The mini player opens Now Playing: seek, shuffle, repeat off/all/one, favorite, collection picker, playback speed, and queue. Queue entries support jump, remove, and move up/down. The foreground MediaLibraryService owns ExoPlayer independently of the Activity and supplies system media controls, Bluetooth/headset commands, audio focus, and unplug handling. Queue, track, position, speed, repeat, and shuffle are checkpointed locally; restoration prepares playback without unexpectedly starting audio.

## File deletion: two different actions

**Delete collection only** removes the collection and its membership records. It never calls a storage deletion API. Files and other collections stay intact.

**Delete collection and music files** deduplicates content URIs, reports physical-file count and cross-collection use, requires an explicit acknowledgement, and then launches `MediaStore.createDeleteRequest`. Large requests use batches of at most 500 files. Bulk song deletion uses the same flow.

The app checks actual provider state afterward, including cancellation and partial completion. Only references to confirmed missing files are removed from every collection and the queue. The collection itself is removed only after all its requested files are confirmed gone. If Android denies/cancels the first batch, the files and collection remain. If a later batch is cancelled, already approved deletions cannot be undone; the remaining collection/files are retained and the partial outcome is reported. A fresh scan also reconciles changes after process death.

## Physical metadata editing

The song menu opens an editor for title, artist, album, album artist, genre, track, year, and artwork. Artwork can be chosen, square-cropped, zoomed, positioned, and previewed. Reset restores unsaved fields. Unchanged first values leave original multi-value metadata frames intact. Saving requests `MediaStore.createWriteRequest`, stops the active file reader while retaining the queue/position, and reloads playback metadata after writing. Playback stays paused until the user resumes it.

The conservative write allowlist is **MP3, FLAC, and OGG (Vorbis)** using jaudiotagger's Android artwork implementation. Other formats remain playable when Android supports them, but the editor explicitly declines physical writes. Raw AAC, M4A/MP4, WAV, Opus-in-OGG, DRM, and malformed/unsupported tags are not promised. A `.ogg` file whose codec/tag implementation is unsupported fails with a visible error before replacing the original.

The editor copies the original into private recovery storage, edits a separate file, validates the result, and verifies the bytes written back through the content resolver. A write failure attempts restoration. Indexing is a separate step: Android reads the embedded tags through its public media scanner, and pending indexing is reported without pretending the physical write failed. If restoration fails, the recovery copy is retained and its private location is reported; do not clear app data before recovery. Android does not offer an atomic filesystem rename for a foreign MediaStore content URI: a crash or storage failure during replacement is a residual risk. Keep independent backups of valuable files.

All audio reads/writes use content URIs. An optional MediaStore `DATA` query is used only to ask Android's public `MediaScannerConnection` to re-index a modified file, never to open the audio by path. Provider/indexing and artwork refresh behavior can vary by device.

## Structure

```
app/src/main/java/dev/noor/music/
  common/         Theme, artwork, typography, reusable controls
  domain/         Library models, queries, collection contract and use cases
  database/       Room schema, indexed queries, transactions, foreign keys
  data/           Repository, DataStore settings/checkpoints, Hilt providers
  storage/        MediaStore scanning, artwork provider, verified tag/file operations
  playback/       MediaLibraryService, MediaController bridge, media items
  presentation/   ViewModel, state-driven screens, dialogs and sheets
```

A single Gradle application module keeps development simple. Collection domain actions depend on an interface; storage operations are separate from membership operations. Room entities are the shared local library model. Scans and tag work run off the main thread; artwork is loaded lazily at display size. Collection validation and queue resolution use batches of at most 500 URI bindings. Library filters use SavedStateHandle, metadata drafts survive Activity recreation in the ViewModel, and pending Android consent requests are consumed once to avoid relaunching them on rotation. Large lists use stable URI keys and lazy layouts. Selection uses sets, audio membership uses foreign keys, and rescans preserve favorites/history without replacing parent rows.

## Validation and release checklist

`testDebugUnitTest` covers real Room queries/transactions with Robolectric, collection deletion isolation, cascading stale-reference cleanup, rescan preservation, ordering, literal wildcard search and Persian text, combined filters, summaries, shared use, deletion batching, atomic creation with stale selections, queue restoration with missing files and duplicate tracks, album identity collisions, monotonic artwork revisions, multi-value tag preservation, and bulk queries across binding batches.

`connectedDebugAndroidTest` exercises a real Media3 service/decoder without an Activity, checks the media notification, and creates app-owned generated tone files in `Music/NoorTests`, edits MP3/FLAC/OGG tags, re-reads physical tags, and compares encoded audio samples before/after. It deletes only its own fixtures. These tests do not approve deletion or write prompts for somebody else's files.

Before release, test on physical Android 11, 13, and 16 devices with real removable storage and a large library:

- Permission denial, permanent denial, revocation, rotation and process recreation.
- Playback after Home, screen lock, UI dismissal, audio focus loss, calls, and headset removal; headset/Bluetooth commands and notification actions.
- Foreign-file write/deletion approval, cancellation at each batch, read-only SD cards, insufficient space, disconnected volumes, interrupted writes and recovery.
- Cover writes, MediaStore refresh, unsupported/corrupt files, long Persian/English metadata, RTL, large fonts, small screens and TalkBack.
- Library size and scrolling performance, startup time, battery behavior, signing and store policy requirements.

Some operation/error messages from the platform and storage layer remain in English. Search uses indexed filtering and escaped `LIKE` rather than an FTS tokenizer, preserving Persian substring matching; extremely large libraries require device profiling. Collection/queue reordering uses accessible up/down actions rather than drag-and-drop. The app does not implement gapless crossfade, an equalizer, cloud sync, online lookup, or downloads.

Compilation and automated tests establish the checks they run; they do not establish release certification or physical-device behavior. See `docs/VALIDATION.md` for the actual validation recorded in this workspace.
