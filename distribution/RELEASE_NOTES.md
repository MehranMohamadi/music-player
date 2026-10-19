Noor 1.0.1 — Up Next and audio-reactive mini player.

برای نصب، فایل **Noor.apk** را از بخش Assets دانلود کنید. اندروید ۱۱ یا بالاتر لازم است.

- Stabilized Up Next's measured height and expansion anchors while dragging from partial to full height. The queue scrolls within the sheet and keeps navigation bar padding separate from its drag anchors.
- Added an animated waveform to the mini player driven by actual decoded PCM energy. No microphone permission, generated/random movement, or audio file modification. Bars settle when paused; analysis stops when no UI is observing.
- Retains the offline library, background playback, playlists, favorites, bulk management, metadata editing, and Persian/RTL UI.

**Download:** `Noor.apk` is the optimized, installable APK (~6 MiB), signed with the development/test key. This is an evaluation build, not a production-signed store release. The signing key is not included in this repository.

**Validation:** see `docs/VALIDATION.md` for the recorded build and test results. Physical-device drag smoothness and audible playback remain unverified in this worker; its emulator failed before app execution during the original device test attempt.

`SHA256SUMS` records the exact APK attached here (its repository path is `distribution/Noor.apk`; after downloading both assets, use `sha256sum Noor.apk` to compare).
