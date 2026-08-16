# OpenReader

OpenReader is a native audiobook and ebook client for self-hosted libraries. This fork uses the Audiobookshelf mobile project as an API and migration reference while replacing the Capacitor UI with platform-native applications.

## Native Rewrite

The active implementation is on the `native-rewrite` branch:

- `native/android`: Kotlin, Jetpack Compose, Material 3 Adaptive, Android Media3, Android Keystore
- `native/apple`: Swift 6.2 package, SwiftUI adaptive library shell, platform-neutral domain tests
- `scripts`: network-volume-safe verification scripts

The inherited Nuxt, Capacitor, `android`, and `ios` directories remain temporarily for protocol comparison. New features must not be added there.

## Current Milestone

Android currently provides:

- Adaptive phone, tablet, foldable, desktop, TV, and car classification based on the active window
- Audiobookshelf password or API-key sign-in and catalog loading
- Encrypted token persistence without password storage
- Native library browsing, filtering, search, list-detail layouts, and a demo catalog
- Background audiobook playback through a Media3 session service
- Native EPUB reading through Readium with page navigation, font sizing, themes, and local resume
- Authenticated EPUB caching with structural validation and partial-download cleanup
- Project Gutenberg OPDS search with direct, validated EPUB downloads
- Optional Standard Ebooks OPDS search using a Patrons Circle email
- API 37 compilation and target configuration

Apple currently provides a tested native domain and SwiftUI library foundation with compact tab and regular sidebar layouts. The installable iOS target, Keychain session store, AVFoundation player, and Readium Swift navigator are the next Apple milestone.

## Library Contents

OpenReader displays media files indexed by Audiobookshelf. Author or title metadata that exists only in Bookshelf/Readarr, including unmonitored discovery entries, will not appear until a user-owned ebook or audiobook file is imported and the Audiobookshelf library is scanned.

Public-domain OPDS results are downloaded into OpenReader's local EPUB cache and do not pass through Bookshelf, Readarr, or Audiobookshelf. Project Gutenberg is available without an account. Standard Ebooks requires access to its full OPDS feed and uses the Patrons Circle email entered under Sources.

The current self-hosted test library contains the public-domain EPUB and LibriVox audiobook of *Alice's Adventures in Wonderland*. It does not contain Junie B. Jones or Judy Moody media files.

## Build

```shell
./scripts/build-android.sh
./scripts/build-apple.sh
```

The Android script writes a local debug APK to `native/android/dist/openreader-android-debug.apk`. Build artifacts are intentionally excluded from Git.

## Security

No server credentials are embedded in either application. Android stores access and refresh tokens with an Android Keystore-backed AES-GCM key. Cleartext traffic is restricted to the configured private-LAN development host; production endpoints should use HTTPS.

## Reading Engines

Android EPUB parsing and navigation use Readium Kotlin. The Apple target will use Readium Swift. Audiobook playback uses the operating system media stacks. This keeps format handling and playback behavior on maintained, standards-oriented libraries rather than custom parsers.

## License

This fork retains the upstream GNU GPL v3 license and attribution history.
