# October 2026 refinement

## First published CI run and follow-up

On 2026-10-31, the [first published verification run](https://github.com/etabli-dev/etabli-plume/actions/runs/41200127938) failed in the Android SDK installation step with `sdkmanager: command not found` (exit 127), before Android tests, lint or packaging ran. The workflow now explicitly installs Android command-line tools using `android-actions/setup-android` v4.0.4, pinned to its verified commit, before requesting API 36 and build-tools 35.0.0. Existing checks and artifact collection remain intact. YAML and the action reference were checked locally before the follow-up run. Earlier local build and device evidence below remains separate from this failed remote run.

Later on 2026-10-31, the [follow-up verification run](https://github.com/etabli-dev/etabli-plume/actions/runs/41200545481) passed for [commit `c0b8183c4b35`](https://github.com/etabli-dev/etabli-plume/commit/c0b8183c4b35e048f7b90d3da98d54c8d9dce1c1): Android `testDebugUnitTest lintDebug assembleDebug bundleRelease` and the full unsigned iOS simulator build succeeded. The [documentation build and deployment](https://github.com/etabli-dev/etabli-plume/actions/runs/41200545528) also passed for that commit. These results verify compilation and the recorded automated checks; installed application journeys, release signing and store acceptance remain separate gates.

The first run passed the Swift model checks but failed the full iOS simulator build: the search-clear button referenced an out-of-scope `entry.command` in its accessibility label. Its label is now `Clear search`, matching the action and removing the invalid reference. Local Swift syntax parsing passed, and the follow-up macOS CI run above subsequently passed full unsigned iOS simulator compilation. Local Xcode remains unavailable.

## Purpose, findings and acceptance

Plume is an offline command/snippet reference with persistent favourites. Preserve
its bundled content and existing favourite keys across platforms.

Confirmed findings: Android displays commands and snippets without any clipboard
action; its search does not trim input or search categories/packages; favourite
writes can fail without an actionable message and use 20 dp tap targets. Both
bundles contain the same 123 unique commands and 10 snippets, but decoding does
not validate category references or stable IDs before lazy-list rendering.

Plan: add explicit copy actions for source commands and snippet code, accessible
favourite toggles and visible persistence errors; centralise/test filtering and
validate bundled content; retain the platform IDs and improve first-use/privacy
information. Validate Kotlin unit tests, independent cross-platform JSON checks,
Android lint/debug/release packaging, and inspect Swift changes. The later unsigned
iOS compilation result is recorded above; device clipboard/restore checks remain
separate gates.


## Android submission compatibility milestone

The baseline targeted API 34, below the [current Google Play submission requirement](https://developer.android.com/google/play/requirements/target-sdk). Compile/target SDK now move to 36 while retaining minimum SDK 26. AGP 8.13.2, Gradle 8.13 (distribution SHA-256 pinned), and JDK 17 are a [documented compatible toolchain](https://developer.android.com/build/releases/agp-8-13-0-release-notes). The API 27 navigation-bar attribute is now in qualified resources; Android 8.0 uses a dark navigation bar so its white icons remain visible. Validate tests, lint, debug APK and unsigned release packaging after migration. Device checks must cover insets, predictive back, rotation and existing saved data.

## Completed and verified

Android now copies commands and whole snippets, searches trimmed input across
names/previews/categories/packages/usage, provides labelled 48 dp favourite
controls, and surfaces failed favourite writes. Both loaders validate IDs and
category references before presenting the bundled content. Existing favourite
keys and all source dictionary entries are preserved.

Observed on 2026-10-30: Android `testDebugUnitTest lintDebug assembleDebug
bundleRelease` passed (two behavioural unit tests); Python confirmed both bundles
contain the same 123 commands and 10 snippets with intact source escaping; the
actual Swift model and bundled iOS JSON compiled and passed the standalone
validation/search checks. Full SwiftUI compilation was not performed at this local
milestone because Xcode is absent here; it subsequently passed in the 2026-10-31
remote CI run above. Device clipboard/accessibility/backup verification, release
signing and store acceptance remain separate gates.

Next release step: run both device journeys from the README, resolve any device
findings, configure signing, then assemble the store package for approval.

The 2026-10-31 follow-up CI run above passed the full unsigned iOS simulator build in addition to the Swift model checks. This compilation result does not establish installed iOS clipboard, accessibility or restore behavior.

## Local Android installation review

The final manifest still labelled the installed Android application “Établi TeX”, while the repository and iOS application use Plume. The launcher and About screen now display Établi Plume; application IDs, data paths and signing identities are unchanged so existing installs retain their data. Verify the rebuilt APK's application label and the installed About screen.

Local verification on 2026-10-30: `testDebugUnitTest lintDebug assembleDebug bundleRelease` passed in `plume-android-device-prep` (2 tests; zero failures). APK badging confirms package `com.raban.etabli.tex.debug`, minimum API 26, target API 36 and application label `Établi Plume` across locales. The unsigned AAB is prepared; installed About-screen review and distribution acceptance are separate checks.
