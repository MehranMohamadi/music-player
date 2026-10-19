# Validation record — 2026-10-18

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
