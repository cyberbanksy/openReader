# OpenReader Engineering Rules

## Product Boundary

- New product code belongs in `native/android` or `native/apple`.
- Treat the inherited Nuxt and Capacitor app as read-only protocol and behavior reference.
- Keep each platform's presentation native: Jetpack Compose on Android and SwiftUI on Apple platforms.
- Share API contracts and test fixtures conceptually, not UI source code.

## Delivery

- Add focused tests before changing domain or protocol behavior.
- Keep builds reproducible and use the scripts in `scripts/` on network-mounted workspaces.
- Use a debug application ID suffix and fail closed when release signing is not configured.
- Never commit APKs, build directories, credentials, access tokens, passwords, or signing material.

## Architecture

- Detect the active window, not only the physical device. Phone, tablet, foldable, desktop, and split-window layouts must remain usable.
- Use Media3 for Android audio sessions and AVFoundation for Apple audio sessions.
- Use Readium toolkits for EPUB parsing and navigation. Do not implement an EPUB parser.
- Keep server transport, persistence, playback, and UI in separate modules as the project grows.
- Store renewable session credentials in Android Keystore or Apple Keychain; never persist account passwords.

## Quality Gate

- Android: `./scripts/build-android.sh`
- Apple: `./scripts/build-apple.sh`
- Validate compact and expanded layouts before merging UI changes.
