# Établi Plume — platform roadmap

Status as of 2026-11-08. This page covers Ubuntu/Debian, Windows, macOS, iOS and
Android: what exists, how each platform is built and verified, and the ordered
steps to a release on each. Verification claims are limited to what was actually
run; "CI only" means the check exists in a workflow but has not been run on that
platform outside GitHub Actions.

See the [Apple build and recovery audit](apple-audit-2026-11-08.md) for the current local build evidence and recovery fixes.

## 1. Platform role

Plume is planned for **both desktop and mobile, desktop first**. LaTeX documents
are written at the computer, so the main use is a small window next to the editor:
type a few letters, press Enter, paste the command. The phone is a second screen
and a pocket reference away from the desk.

| Platform | What it is for |
|---|---|
| Ubuntu/Debian | Primary desktop target: a keyboard-driven window beside the editor, optionally kept on top. |
| Windows | The same desktop app for Windows users of TeX distributions and online editors. |
| macOS | The same desktop app for Mac users. |
| iOS | Companion: pocket reference and second screen on iPhone and iPad. |
| Android | Companion: pocket reference and second screen; currently the most complete build. |

All five platforms are planned; none is "not planned".

## 2. Current state

| Platform | Status | Technology | How it is built | What is verified | Distribution |
|---|---|---|---|---|---|
| Ubuntu/Debian | Development preview; desktop/VERSION is 1.0.0 | Compose Multiplatform 1.9.0 desktop, Kotlin 2.2.20, Material 3 (1.8.2, the version the plugin's `compose.material3` alias selects); JDK 17 runtime bundled by jpackage | `cd desktop && ./gradlew packageDeb` → `etabli-plume_1.0.0-1_amd64.deb` | Locally on Ubuntu 26.04 (2026-11-08): 31 unit tests; headless launch under Xvfb of the app image and of the launcher extracted from the .deb; a keyboard session on that launcher driven with `xdotool` (type, Enter, Shift+Enter, Ctrl+D, Esc, ↓, Alt+↓, Ctrl+T) with the clipboard and preferences file read back (window stacking for always-on-top needs a window manager and was not checked); `.deb` checked with `dpkg-deb -I/-c`. **Not** installed with `dpkg`/`apt`, not tried in a real desktop session or on Ubuntu 24.04 / Debian 13 | None yet. `desktop.yml` uploads an unsigned `.deb` as a workflow artifact once pushed |
| Windows | Development preview; desktop/VERSION is 1.0.0 | Same code as Linux | `./gradlew packageMsi` on `windows-latest` in `desktop.yml` | Not verified locally; [three-platform baseline CI passed](https://github.com/etabli-dev/etabli-plume/actions/runs/42533357152); current packaging refinements require their own run | None; unsigned MSI artifact from CI |
| macOS | Development preview (source only) | Same code as Linux | `./gradlew packageDmg` on `macos-15` (Apple silicon) in `desktop.yml` | Not verified locally; [three-platform baseline CI passed](https://github.com/etabli-dev/etabli-plume/actions/runs/42533357152); current packaging refinements require their own run; unsigned and not notarized | None; unsigned DMG artifact from CI |
| iOS | In development | SwiftUI, iOS 17+, `ios/EtabliTeX.xcodeproj` | Xcode; CI job `swift-model` in `android.yml`: Swift model checks + unsigned simulator build | CI passed on 2026-10-31 (model checks, full unsigned simulator build). No device run, no signing, no TestFlight | Not available; App Store planned |
| Android | In development; development pre-release | Kotlin 1.9.24, Jetpack Compose, DataStore; AGP 8.13.2; API 26–36 | `cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease` (`android.yml`) | CI passed on 2026-10-31 (unit tests, lint, debug APK, unsigned release AAB); emulator journeys on API 35 on 2026-11-02 ([refinement record](refinement-2026-10.md)) | `v0.1.0` pre-release APK on GitHub Releases (2026-06-24, older than the October/November changes); Google Play and F-Droid planned (store text in `fastlane/metadata`) |

The desktop app offers a section list (all, favourites, the ten command
categories, snippets), instant ranked search over commands and snippets, a detail
pane with an approximate preview, the exact source, usage example and package line,
favourites, one-key copy, and an optional always-on-top window. It starts with the
search field focused:

| Key | Action |
|---|---|
| typing | search (commands, names, symbols, categories, packages, snippet code) |
| ↑ / ↓, Page Up / Page Down, Ctrl+↑ / Ctrl+↓ | move the selection |
| Enter | copy the selected command or snippet |
| Shift+Enter | copy the usage example, when there is one |
| Ctrl+D | toggle favourite |
| Alt+↑ / Alt+↓ | previous / next section |
| Ctrl+T | keep the window on top |
| Ctrl+F or Ctrl+L, Esc | focus search; Esc also clears it |

On macOS, Cmd replaces Ctrl and Option replaces Alt (not verified on a Mac).

## 3. Architecture

### Now

- **Catalogue data.** One JSON format, `Dictionary.json` (123 commands, 10
  snippets, 10 categories). It is committed twice — `android/app/src/main/assets/`
  and `ios/App/Resources/` — and `scripts/check_dictionary.py` (run in CI) fails if
  the two differ. The desktop build adds no third copy: its Gradle
  `processResources` task packages `android/app/src/main/assets/Dictionary.json`
  itself into the desktop JAR, and a unit test checks that the packaged resource is
  byte-identical to that asset. Nothing in `android/` was changed for this.
- **Logic.** Model, validation and search exist three times: Android
  `data/Dictionary.kt` (org.json), iOS `Models.swift` (Codable) and desktop
  `desktop/src/main/kotlin/com/raban/etabli/tex/data/Dictionary.kt`
  (kotlinx.serialization). The desktop file deliberately keeps the Android class
  names, validation rules and search semantics (its tests include the Android test
  cases) so that it can become the shared implementation. Desktop-only logic —
  sections, combined command/snippet results and match ranking — lives in
  `data/Catalog.kt`, also free of UI and JVM-desktop imports.
- **Favourites.** All platforms store the exact command source (for example
  `\alpha`) as the key: Android DataStore `tex_favorites`, iOS UserDefaults
  `texcompanion.favorites`, desktop `preferences.json` in the per-user
  configuration directory (`$XDG_CONFIG_HOME/etabli-plume` or
  `~/.config/etabli-plume`; `%APPDATA%\Etabli Plume`;
  `~/Library/Application Support/com.raban.etabli.tex`). Writes are atomic; an
  unreadable file is kept as `preferences.json.corrupt`. There is no sync.
- **Design.** `design/coder-design-system.json` is the token source. The desktop
  theme uses its colours, spacing, radii and type scale, with light, dark and
  system modes, and `DesignTokensTest` fails if the Kotlin constants drift from the
  JSON. The mobile apps still render with their in-app "Coder" twin
  (`ios/App/Theme.swift`, `android/.../tex/ui/theme/CoderTheme.kt`), whose
  neutral colours and spacing differ slightly from `design/` (for example
  background `#FBFBF9` vs `#FFFFFF`, medium spacing 12 vs 16); the accent
  `#28A745` is the same everywhere.
- **Builds.** `android/` and `desktop/` are separate Gradle builds with the same
  wrapper (Gradle 8.13). Android uses Kotlin 1.9.24; desktop uses Kotlin 2.2.20
  with a JDK 17 toolchain (provisioned automatically if missing). The app icon for
  all desktop packages is rendered at build time from the iOS 1024 px icon
  (itself the raster of `design/icon-plume.svg`).

### Target: a shared Kotlin Multiplatform module

```
shared/    KMP library: Dictionary.json (single copy), TeXDictionary, Catalog,
           ranking, favourite rules; targets jvm (desktop) and android
android/   Compose UI + DataStore, depends on shared
desktop/   Compose Multiplatform UI + preferences file, depends on shared
ios/       SwiftUI unchanged; the Xcode project references shared's Dictionary.json
```

- Start `shared/` from the desktop `data/` package (already platform-neutral and
  tested) and keep the package `com.raban.etabli.tex.data`.
- Ship the JSON once, as a Compose Multiplatform resource
  (`composeResources/files/Dictionary.json`, read with `Res.readBytes`) or a common
  Java resource; Android then stops reading `assets/`.
- Wire it with Gradle composite builds (`includeBuild("../shared")` in
  `android/settings.gradle.kts` and `desktop/settings.gradle.kts`), so the existing
  `cd android && ./gradlew …` and `cd desktop && ./gradlew …` commands and any
  F-Droid recipe keep working.
- iOS keeps its small Swift model; only the JSON becomes shared. A Kotlin/Native
  framework for iOS is not planned.
- Prerequisite: Android moves from Kotlin 1.9.24 to the Kotlin 2.2 line with the
  Compose compiler Gradle plugin. This needs an Android SDK and an emulator or
  device check, which is why it was not done together with the desktop app.

## 4. Per-platform execution

All desktop commands run in `desktop/` with JDK 17 or 21 as `JAVA_HOME` (Gradle
8.13 does not run on JDK 25). The build compiles with, and packages, the JDK 17
toolchain.

### Ubuntu/Debian

- **Build and run:** `./gradlew run`; tests `./gradlew test`; package
  `./gradlew packageDeb`; headless check
  `./gradlew createDistributable && scripts/launch-smoke.sh` (needs `xvfb-run`
  and `xprop` from x11-utils, plus `xdotool`). After `packageDeb`, run
  `python3 scripts/refine_deb.py build/compose/binaries/main/deb/*.deb` to finish
  the menu/dock metadata using the WM_CLASS measured by that smoke check.
  The isolated Xvfb smoke uses Skiko's supported software renderer and waits up
  to 30 seconds for a visible window owned by its launcher PID. It reads WM_CLASS
  by that window ID, independent of title encoding; no guessed class is used.
- **Package formats:** `.deb` now. jpackage takes the dependency names from the
  build host, so the CI package (built on Ubuntu 24.04, for example
  `libasound2t64`) targets Ubuntu 24.04+ and Debian 13+. For older distributions
  and a proper store presence, add a **Flatpak** on Flathub (preferred over an
  AppImage: sandboxed, updates, AppStream metadata).
- **Signing prerequisites:** none for `.deb` files attached to GitHub Releases;
  publish SHA-256 checksums with them. An apt repository would need a GPG signing
  key (optional). Flathub needs an application ID whose domain or code-hosting
  namespace the owner controls (for example `io.github.etabli_dev.Plume`).
- **CI job:** `desktop.yml`, matrix entry `ubuntu-24.04`: dictionary check, unit
  tests, Xvfb launch check, `packageDeb`, artifact `etabli-plume-desktop-deb`.
- **Menu/dock metadata:** CI measures WM_CLASS from the running app under Xvfb
  and refines the jpackage Debian entry to `Name=Établi Plume` plus that exact
  `StartupWMClass`, updating package integrity checksums. It validates the final
  desktop entry before uploading. A raw `packageDeb` without the refinement
  still has jpackage's default name; the published artifact uses the refined one.
  Interactive GNOME dock acceptance remains a separate check.
- **Acceptance:** on clean Ubuntu 24.04 and Debian 13 machines,
  `sudo apt install ./etabli-plume_<version>-1_amd64.deb` succeeds; the menu entry
  starts the app with the search field focused; Enter pastes the exact command into
  a text editor; a favourite survives a restart; always-on-top works in an X11 and
  a Wayland session; `sudo apt remove etabli-plume` removes the menu entry.

### Windows

- **Build and run:** `.\gradlew.bat run`; `.\gradlew.bat packageMsi` (the Compose
  plugin downloads WiX). Only built in CI so far.
- **Package format:** MSI, per-user install (no administrator rights), Start menu
  group "Etabli", desktop shortcut, fixed upgrade UUID so newer MSIs replace older
  ones. MSIX only if a Microsoft Store listing is wanted later.
- **Signing prerequisites:** an Authenticode code-signing certificate (OV
  certificate or a cloud signing service such as Azure Artifact Signing), stored as
  GitHub Actions secrets and applied with `signtool` to the MSI. Unsigned MSIs
  trigger SmartScreen warnings.
- **CI job:** `desktop.yml`, matrix entry `windows-latest`: unit tests,
  `packageMsi`, artifact `etabli-plume-desktop-msi`.
- **Acceptance:** on Windows 11 the MSI installs without administrator rights;
  the Start menu entry opens the app; Enter pastes into Notepad and a LaTeX editor;
  favourites persist in `%APPDATA%\Etabli Plume\preferences.json`; installing a
  newer MSI upgrades in place; uninstalling from Settings removes the app.

### macOS

- **Build and run:** `./gradlew run`; `./gradlew packageDmg` on a Mac. Built
  locally on Apple silicon and in CI; see the [audit](apple-audit-2026-11-08.md).
- **Package format:** DMG containing `etabli-plume.app` (Dock name "Établi
  Plume"), bundle ID `com.raban.etabli.tex` (the iOS bundle ID). macOS requires a
  major version of at least 1. All desktop packages now read the same `1.0.0`
  value from `desktop/VERSION`; this version does not imply store release readiness.
- **Signing prerequisites:** Apple Developer Program membership; a "Developer ID
  Application" certificate (`.p12` and password as secrets); notarization
  credentials (App Store Connect API key, or Apple ID with app-specific password,
  plus team ID). Configure `nativeDistributions.macOS.signing` and
  `notarization` in `desktop/build.gradle.kts`, then run `notarizeDmg`.
- **CI job:** `desktop.yml`, matrix entry `macos-15`: unit tests, `packageDmg`,
  artifact `etabli-plume-desktop-dmg`. `macos-15` builds for Apple silicon only;
  Intel Macs need a second build on an Intel runner.
- **Acceptance:** the notarized DMG passes `spctl -a -vv` and opens without a
  Gatekeeper warning; the app copies into TeXShop or another editor with Cmd
  shortcuts; favourites persist in
  `~/Library/Application Support/com.raban.etabli.tex/`.

### iOS

- **Build and run:** open `ios/EtabliTeX.xcodeproj` in Xcode, select a team and an
  iOS 17+ destination. CI already builds the unsigned simulator app.
- **Package format:** TestFlight, then App Store.
- **Signing prerequisites:** Apple Developer Program (the same membership as
  macOS); an App Store Connect record for `com.raban.etabli.tex`; distribution
  certificate and provisioning profile (or automatic signing); an App Store
  Connect API key as a CI secret for uploads.
- **CI job:** extend the `swift-model` job with a signed archive and TestFlight
  upload that runs only when the signing secrets exist.
- **Acceptance:** on an iPhone and an iPad, search, copy into Notes, favourites
  after relaunch and VoiceOver labels work; an external TestFlight tester installs
  the build; App Review passes with the privacy label "Data Not Collected".

### Android

- **Build and run:** see the README and `android/SUBMISSION.md`
  (`./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease`).
- **Package formats:** signed APK on GitHub Releases (a `v0.1.0` pre-release
  exists), AAB for Google Play, F-Droid (store text is prepared in
  `fastlane/metadata/android`; no F-Droid build recipe yet).
- **Signing prerequisites:** the private upload keystore via
  `android/keystore.properties`, Play App Signing, and — if releases are built in
  CI — the keystore and passwords as secrets.
- **CI job:** `android.yml` (tests, lint, debug APK, unsigned AAB).
- **Acceptance:** as listed in `android/SUBMISSION.md`; after the shared-module
  milestone, repeat the emulator journeys from the refinement record.

## 5. Milestones

1. **M1 — Desktop packages in CI.** The recovery refinement [passed all three platforms](https://github.com/etabli-dev/etabli-plume/actions/runs/42533357152); the new version/menu/release changes require their own exact-commit workflow. Exit: all
   three jobs green; `.deb`, `.msi` and `.dmg` artifacts downloadable; run URL
   recorded in `docs/current-status.qmd`.
2. **M2 — Desktop acceptance on real machines.** Install the CI artifacts on
   Ubuntu 24.04, Debian 13, Windows 11 and macOS and walk the acceptance lists in
   section 4 (unsigned packages; bypass SmartScreen/Gatekeeper for the test).
   Exit: results recorded per platform; defects filed.
3. **M3 — Linux desktop entry.** Ship a desktop entry named "Établi Plume" with a
   matching `StartupWMClass` (custom jpackage resources or the Flatpak manifest).
   Implemented through the measured-class Xvfb smoke plus `refine_deb.py`;
   exact-commit Linux packaging CI and interactive GNOME acceptance are pending.
   Exit: menu and dock show the right name and icon in GNOME.
4. **M4 — Release workflow.** On a `v*` tag, build all desktop packages, write
   `SHA256SUMS`, and attach everything to a **draft** GitHub Release (only that job
   gets `contents: write`); the owner publishes. Settle one version source for
   Gradle. Implemented: `desktop/VERSION` is now the canonical 1.0.0 source for
   all desktop packages; `desktop.yml` validates the exact `v<version>` tag,
   requires all three packages, writes SHA256SUMS and creates only a draft.
   Manifest checks reject missing, empty and stale-version artifacts. No tag or
   release was created during the audit. Exit still pending: a test tag produces
   a complete draft release; the owner reviews and publishes it.
5. **M5 — Shared module.** Upgrade Android to Kotlin 2.2 with the Compose compiler
   plugin; create `shared/` from the desktop `data/` package; one
   `Dictionary.json`; Android and desktop consume it via composite builds; the iOS
   project references the same file; update `scripts/check_dictionary.py`. Exit:
   one catalogue file in the repository; Android, desktop and iOS CI green; Android
   emulator journeys repeated.
6. **M6 — Windows signing.** Exit: the release workflow produces a signed MSI that
   installs without an "unknown publisher" warning.
7. **M7 — macOS signing and notarization.** Exit: notarized, stapled DMG passes
   `spctl`; decide and, if wanted, add the Intel build.
8. **M8 — Mobile store releases.** Android: signed AAB to Play internal testing,
   then production; F-Droid submission. iOS: TestFlight, then App Store. Exit:
   listings live with the "Établi Plume" name.
9. **M9 — Flatpak (optional).** Flathub manifest building from source, AppStream
   metainfo with screenshots. Exit: Flathub listing accepted.
10. **M10 — Design token alignment.** Decide whether the mobile "Coder" twin
    palette or `design/coder-design-system.json` is canonical and bring all three
    UIs to it. Exit: one token set, checked by tests on every platform.

## 6. Owner decisions and prerequisites

- **Apple Developer Program** membership (needed for macOS Developer ID signing,
  notarization, TestFlight and the App Store) and who holds it.
- **Windows code signing:** OV certificate or a cloud signing service, or ship
  unsigned previews with a SmartScreen warning.
- **Google Play** developer account; whether to submit to **F-Droid**.
- **Version scheme:** desktop packages use `desktop/VERSION` (`1.0.0`), matching
  the existing macOS bundle version; mobile marketing version remains `1.0`.
  Future desktop tags must be exactly `v` followed by the canonical version.
- **Debian file name:** jpackage writes `etabli-plume_1.0.0-1_amd64.deb` (Debian
  revision `-1`); keep it or rename in the release workflow.
- **Intel Mac support:** add a second macOS build or ship Apple silicon only.
- **Linux channels:** `.deb` on GitHub Releases only, or also Flathub (needs an
  application ID such as `io.github.etabli_dev.Plume`) or an apt repository (needs a
  GPG key).
- **Identifiers:** the desktop app reuses `com.raban.etabli.tex` for the macOS
  bundle ID; keep it, or choose a separate ID if a Mac App Store version is ever
  planned.
- **Naming:** keep "Établi Plume" as the product name on every store and package;
  use "LaTeX" only descriptively, with the non-affiliation note, and never in
  product, package or bundle names.
