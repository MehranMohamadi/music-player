# Third-party components

Noor uses the following projects through the pinned Gradle dependencies. Their original source and license notices remain applicable.

| Project | License | Source |
| --- | --- | --- |
| AndroidX, Compose, Room, DataStore, Media3 | Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ and https://github.com/androidx/media |
| Kotlin and kotlinx.coroutines | Apache-2.0 | https://github.com/JetBrains/kotlin and https://github.com/Kotlin/kotlinx.coroutines |
| Dagger / Hilt | Apache-2.0 | https://github.com/google/dagger |
| Coil | Apache-2.0 | https://github.com/coil-kt/coil |
| jaudiotagger 3.0.1 | LGPL (see upstream distribution) | https://bitbucket.org/ijabz/jaudiotagger/ |
| Robolectric, JUnit, AndroidX Test (test only) | See their distributions | https://github.com/robolectric/robolectric and https://github.com/junit-team/junit4 |

The 41 vector sources under `common/icons` are a subset of AndroidX Material Icons 1.7.8, with only their Kotlin package relocated; upstream Apache-2.0 headers are retained.

jaudiotagger is used unmodified; its published source artifact is available alongside the binary at https://repo.maven.apache.org/maven2/net/jthink/jaudiotagger/3.0.1/ . The included Gradle project can be rebuilt with a replacement tagging library. Source and license materials for the exact dependency version should accompany redistribution according to its license.

The test audio fixtures are generated sine waves, produced with the command documented in `docs/VALIDATION.md`. The app contains no bundled commercial music, album covers, Spotify branding, or Spotify assets.
