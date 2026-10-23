# Validation record — 2026-10-18

## Version 1.1.1 — 2026-10-23

- Cover reactions now use a two-step flow: choose an emoji below the artwork, then tap the artwork to place it. Heart ❤️ is the default selection. Picker choices (including custom keyboard emoji) select and close the picker without creating a sticker.
- Each tap stores the selected emoji centered on its normalized touch point. Edge taps keep the sticker inside the cover. Repeated taps, including taps on an existing sticker, create independent copies. Pointer input remains stable while database updates arrive, avoiding cancellation of an in-progress tap.
- New stickers enter with a spring scale/bounce and a short decaying rotational wiggle, then settle. Hold a sticker to remove its topmost copy; accessibility actions support placing a sticker at the center and removing individual stickers.
- Database v3 adds a default-false `centered` flag through Room's generated v2→v3 migration. Existing stickers retain their original layout formula and coordinates. New stickers store normalized centers. Exported v1 and v2 database fixtures both upgrade successfully, and reopen checks verify old and new placement records.
- **36 unit tests passed, zero failures/errors/skipped.** Placement regressions cover centered taps, resizing, corner clamping, tiny covers and exact legacy positions; migration/reopen checks cover v1 and v2 upgrades. `assembleDebug`, `testDebugUnitTest`, and `lintDebug` passed with zero lint errors and 15 warnings.
- `assembleRelease` and `assembleDebugAndroidTest` also passed. The optimized distribution is signed with the existing certificate; its exact size/checksum are recorded in `docs/ARTIFACTS.md`.
- Touch/long-press feel and animation smoothness remain **unverified on a physical device**. No device execution or screenshot is claimed from this worker.

## Version 1.1.0 — 2026-10-23

- Now Playing has a gray waveform seek bar with green played amplitudes, standard Slider touch/accessibility behavior, and a white position marker. The 96-bin RMS envelope is decoded from the actual file on an IO worker, supports 16-bit/float PCM output, cancels on track change/player dismissal, and caches at most 64 small envelopes in private cache storage. Unsupported/read-failed audio keeps seeking available and shows an unavailable state; no fabricated peaks or microphone access.
- Cover reactions offer ten emoji and a custom single-grapheme keyboard emoji field. Each tap creates a separate Room row, including duplicates, with persisted normalized coordinates and rotation. New stickers enter with a spring animation. Tapping a sticker lets the user remove just that instance. No application count limit is imposed.
- Library → Emoji groups the visible library by reaction, counts distinct songs, and opens the normal song list filtered by that emoji. Excluded folders stay excluded. The query observes reaction changes and never duplicates songs for repeated stickers.
- Database v2 adds only the reactions table and indexes through `MIGRATION_1_2`; no destructive migration. A fixture built from the exported v1 schema upgrades and reopens successfully while preserving favorites, history, collections and order. Reactions survive reopen and rescan and cascade only when the song reference is removed.
- **31 unit tests passed, zero failures/errors/skipped.** New regressions cover migration/reopen, duplicate reactions and stable positions, emoji filtering/removal, RMS timeline placement/silence/opposite channel phase, and Unicode emoji sequences (family, flags, skin tones, keycaps).
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `assembleDebugAndroidTest` passed. Lint reports zero errors and 15 warnings. The device test APK compiles and packages successfully; device tests remain unrun.
- `assembleRelease` also passed with code/resource shrinking and Release vital lint. The signed optimized APK reports versionName `1.1.0`, versionCode `3`, and its verified signing certificate matches `distribution/Noor.apk` (certificate SHA-256 `0be974b117c68b1049f5fa1c71ceaa1c8b862d1504468b9612524e982944e13d`). The current optimized distribution size and checksum are recorded in `docs/ARTIFACTS.md`.
- Physical-device UI animation, touch/RTL, audible playback during analysis, and decoder performance are **unverified**. This worker has no `/dev/kvm`. The new `WaveformDeviceTest` exercises real MP3/FLAC/OGG decoding and cache reuse on a device; it has not been executed here.

## Version 1.0.1 — 2026-10-19

- Up Next now uses a remembered sheet state, a stable 92% viewport height, and a weighted scrolling list. Insets no longer change the sheet's measured expansion anchors during a drag.
- The mini player's waveform consumes decoded 16-bit PCM through Media3's pass-through TeeAudioProcessor. It calculates a short RMS history at 30 Hz, observes lifecycle state, and animates down when playback stops. No microphone permission was added.
- **24 unit tests passed, zero failures/errors:** the original 21 plus three PCM-energy regressions covering loudness/silence, preservation of audio buffer position, split decoder buffers, reset, and skipping analysis with no UI observers.
- `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, and `lintDebug` succeeded (zero lint errors, 19 warnings). Device gesture smoothness and audio-output behavior require a physical device; the worker's original emulator limitation below still applies.
- The `distribution/Noor.apk` evaluation build uses the same development signing key as version 1.0.0, so it can update that installed version without uninstalling. No signing key is committed.

## Completed checks

- Android SDK 36, AGP 8.13.2, Gradle wrapper 8.13, Kotlin 2.2.21, JDK 21, Java/Kotlin bytecode target 17.
- `./gradlew --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` completed successfully against the final source.
- **21 unit tests passed, zero failures/errors, zero skipped**: 15 Room tests, 2 deletion/order tests, 3 queue-restoration tests, and a real ID3v2.4 multi-value tag-preservation test. Room checks include same-name album IDs, monotonic artwork keys, and bulk operations spanning SQLite binding batches.
- Android lint: **zero errors, 19 warnings**. Remaining warnings include optional newer dependency versions, backward-compatible manifest attributes, the intentionally exported media service (connections are checked), and KTX style suggestions. No lint errors are suppressed.
- Eight device tests compile into the instrumented test APK: MP3/FLAC/OGG physical tag round trips and unchanged encoded samples; artwork round trip and private artwork-provider decoding; unsupported-format refusal; DataStore playback checkpoint round trip; cancelled-deletion collection preservation; real Media3 decoding and notification without an Activity.
- The debug APK's v2 signature verifies. Its merged manifest has no `INTERNET` permission. The media dependency includes the normal `ACCESS_NETWORK_STATE` permission, which does not permit network connections.

## Device validation blocked before application execution

This worker has no `/dev/kvm`. Headless Android 15/API 35 and Android 11/API 30 system images were attempted using software emulation. They did not reach a reliable, installable Android runtime. Android 15 reported missing package/activity services during installation/start; the Android 11 installation failed with `cmd: Failure calling service package: Broken pipe (32)` during system initialization. The crash buffer showed repeated `DeadSystemException` failures in Android System UI, phone, and launcher processes before Noor ran.

The application did **not** execute successfully in these attempts. The eight instrumented tests are **compiled but unrun**, not passed or failed. There is no verified screenshot, audible playback, Bluetooth result, lock-screen result, foreign-file approval/cancellation result, or physical tag-write result from this worker. This is an emulator/runtime blocker before app execution, not evidence of a repository test failure.

Use an accelerated Android emulator or a physical test device (API 30+) and run:

```sh
./gradlew connectedDebugAndroidTest
```

Then perform the manual release checks in README.md. These include Android storage approvals for foreign files, partial deletions, interrupted writes, audio focus, headset/Bluetooth controls, background playback, accessibility/RTL, and large-library performance. Run device tests on a test device; they play a generated tone and temporarily change playback state, restoring the saved queue afterward. They only delete files they created.

## Generated test fixtures

Only `androidTest` contains tones; production assets contain no sample music. The two-second fixtures were generated locally with:

```sh
for format in mp3 flac ogg; do
  ffmpeg -f lavfi -i 'sine=frequency=440:duration=2' \
    -metadata title='Original tone' -metadata artist='Noor tests' \
    "app/src/androidTest/assets/tone.$format"
done
ffmpeg -f lavfi -i 'sine=frequency=440:duration=30' \
  -metadata title='Service integration tone' \
  app/src/androidTest/assets/long-tone.mp3
```

Fixture ownership is recorded by MediaStore insertions in the instrumented tests. Teardown deletes only those returned URIs.

## Artifact notes

The installable debug build uses the standard development signing key. The optimized release build is unsigned and requires the owner's release signing configuration. No production signing keys are stored in the repository. Physical metadata writing is limited to MP3, FLAC, and supported Vorbis OGG; other formats have an explicit limitation in the editor.

Release build verification and artifact checksums are recorded in [ARTIFACTS.md](ARTIFACTS.md).
