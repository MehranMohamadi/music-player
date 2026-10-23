# Version 1.1.0 — 2026-10-23

Optimized Release APK built with code/resource shrinking and aligned for 16 KiB native-library pages. The installable APK's v3 signature verifies and its certificate matches the previous distribution, so it supports an in-place update. Package `dev.noor.music`, versionCode `3`, versionName `1.1.0`, minimum API 30. The Release APK is not debuggable and has no INTERNET permission.

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `distribution/Noor.apk` | 6356006 bytes (6.06 MiB) | `6ab7d3e172c4b2447533806b3c077773e5466c874cfb9975f3cd76ebc98f1467` |

`assembleRelease`, `assembleDebug`, `testDebugUnitTest`, `lintDebug` and `assembleDebugAndroidTest` succeeded. All 31 unit tests passed; lint reports zero errors and 15 warnings. Physical-device animation, audible playback during waveform analysis and device-codec behavior remain unverified; device tests are compiled but unrun. The existing development/test key is retained outside the repository.

# Version 1.0.1 — 2026-10-19

Debug and optimized release builds succeeded; 24 unit tests passed; lint has zero errors and 19 warnings. The installable `distribution/Noor.apk` uses the same test signing key as 1.0.0. Device drag smoothness and audible playback are unverified in this cloud worker.

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `distribution/Noor.apk` | 6323238 bytes | `0b6069d9a165f88c12b97817e3815704ce9fa5ee36ded88480f4c53e80fbc443` |

# Build artifacts — 2026-10-18

Final source validation: debug/release builds succeeded; 21 unit tests passed; lint has zero errors. The eight instrumented tests compiled but did not run because the worker's Android system images failed before Noor execution.

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | 45,883,968 bytes | `afd3b262bf05d9b0c5d280a52e6d01d444d85bde4b5f5be4479aba4af90d60e8` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 6,294,719 bytes | `7952043b6628335c06e9f60eaf29b9a31f47dde496ee9b1a4fcf2456788d5406` |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | 1,720,910 bytes | `4fbf02265bd8fb2b07fa01767d1acd24e3e8a96ab657108a306d73b1ab0a703d` |

The debug APK is installable on API 30+ and its v2 signature verifies. The release APK is optimized and unsigned; use the owner's signing configuration for distribution. The test APK is intended for AndroidJUnitRunner, not normal app installation.

Build commands executed:

```sh
source /workspace/toolchains/noor-env.sh
cd /workspace/music-player
./gradlew --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew --offline :app:assembleRelease
```

The source archive is `/workspace/artifacts/noor-source.zip`. It contains the Gradle project, source, Room schema, test fixtures, and documentation; generated build outputs, toolchains, caches, local SDK paths, and signing keys are excluded.
