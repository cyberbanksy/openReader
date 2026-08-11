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
- API 37 compilation and target configuration

Apple currently provides a tested native domain and SwiftUI library foundation with compact tab and regular sidebar layouts. The installable iOS target, Keychain session store, AVFoundation player, and Readium navigator are the next Apple milestone.

## Build

```shell
./scripts/build-android.sh
./scripts/build-apple.sh
```

The Android script writes a local debug APK to `native/android/dist/openreader-android-debug.apk`. Build artifacts are intentionally excluded from Git.

## Security

No server credentials are embedded in either application. Android stores access and refresh tokens with an Android Keystore-backed AES-GCM key. Cleartext traffic is restricted to the configured private-LAN development host; production endpoints should use HTTPS.

## Reading Engines

EPUB parsing and navigation will use Readium Kotlin and Readium Swift. Audiobook playback uses the operating system media stacks. This keeps format handling and playback behavior on maintained, standards-oriented libraries rather than custom parsers.

## License

This fork retains the upstream GNU GPL v3 license and attribution history.
