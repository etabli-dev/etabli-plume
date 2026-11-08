# Établi Plume

> A fast offline reference for typesetting commands.

`iOS` `Android` `Desktop preview` · Apache-2.0 · Part of the [Établi Suite](https://github.com/etabli-dev)

> **Note:** Renamed from EtabliTeX to avoid implying affiliation with TeX/LaTeX (whose names are governed by Knuth's TeX license and the LPPL).

Établi Plume is a categorized reference and snippet picker for LaTeX typesetting commands: commands, Greek letters, math operators, formatting, environments and bibliography, with a preview pane and favorites. The 123 commands and 10 snippets are bundled for offline use. Search by command, description, category or package; copy exact command/snippet source and keep favourites. Android uses Compose and DataStore; iOS uses SwiftUI; the desktop development preview (Linux, Windows, macOS) uses Compose Multiplatform and packages the same catalogue. (Not affiliated with the LaTeX Project or TeX.)

## Availability

Établi Plume is **under active development**. There are no App Store, Google Play or F-Droid releases yet.

- **Android:** install the current **development build** as a signed **APK** from **[GitHub Releases](../../releases)**.
- **App Store (iOS):** planned — not yet available.
- **Google Play:** planned — not yet available.
- **F-Droid:** planned — not yet available.
- **Desktop (Linux, Windows, macOS):** development preview — build from source in `desktop/`; no packaged downloads yet.

**Platforms:** the desktop app is a development preview: its Linux `.deb` was built and launch-tested locally, while the Windows and macOS packages are built only in CI. What each platform is for, its current state and the steps to release are in the [platform roadmap](docs/platform-roadmap.md).

## Privacy

No accounts, analytics or network requests. Favourite IDs are stored locally in Android DataStore, iOS UserDefaults or, on desktop, a `preferences.json` file in the per-user configuration directory. Copy is an explicit user action that places the selected source on the system clipboard. The operating system may include local settings in its device backups.

## Repository layout

```
ios/        SwiftUI app
android/    Kotlin + Jetpack Compose app
desktop/    Compose Multiplatform desktop app (development preview)
fastlane/   F-Droid / store listing metadata
```

All apps are one product, sharing the Coder Design System tokens.

## Tech

iOS: SwiftUI. Android: Compose, bundled dictionary JSON. Desktop: Compose Multiplatform (Kotlin/JVM), packaging the Android dictionary JSON

**Status:** In active development — not yet released; dev builds available as a signed APK via [GitHub Releases](../../releases).

## Build and first use

Android prerequisites: JDK 17, Android SDK 36, build-tools 35.0.0, Python 3,
and `ANDROID_HOME` pointing to that SDK. From a clean checkout:

```sh
python3 scripts/check_dictionary.py
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Établi Plume**, search `fraction`, copy a command, toggle its favourite,
and reopen Favourites. Cheatsheet copy buttons copy complete LaTeX source,
including line breaks. The app provides reference previews; it does not compile
LaTeX documents. Debug APKs are for testing; release bundles need your signing
configuration and device acceptance before store submission.

For iOS, open `ios/EtabliTeX.xcodeproj` in Xcode, select a development team and
an iOS 17+ destination, and run. The Foundation-only model checks can also run
with macOS command-line tools:

```sh
swiftc ios/App/Models.swift scripts/check_models.swift -o /tmp/plume-model-check
/tmp/plume-model-check ios/App/Resources/Dictionary.json
```

For the desktop development preview, run Gradle with JDK 17 or 21 (the build
provisions its own JDK 17 toolchain if needed):

```sh
cd desktop
./gradlew test             # unit tests
./gradlew run              # start the app
./gradlew packageDeb       # Linux .deb; packageMsi / packageDmg on Windows / macOS
```

The search field has focus on start: type, use ↑/↓, press Enter to copy,
Shift+Enter to copy a usage example, Ctrl+D to toggle a favourite and Ctrl+T to
keep the window on top (Cmd instead of Ctrl on macOS).

Android tests, lint, APK and release bundle, cross-platform asset validation and
Swift model checks passed locally. Full iOS compilation and device clipboard,
favourite persistence, VoiceOver/TalkBack and backup/restore remain unverified.
See [the refinement record](docs/refinement-2026-10.md).

## Support development

- 💚 **[Liberapay](https://liberapay.com/rabanheller/)** — recurring, 0% commission, to be shown on the F-Droid listing once published.

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

Copyright 2026 R. Heller.
