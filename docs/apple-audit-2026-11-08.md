# Plume: Apple build and recovery audit, 2026-11-08

Repeated recovery from unreadable desktop preferences now preserves each damaged file. Previously, a second recovery replaced the first `.corrupt` backup. The first backup retains its existing name; subsequent backups receive unique names. A regression test exercises two recoveries and verifies both originals and the usable preferences remain.

## Native iOS validation

The existing Swift validation scripts passed, and the full Xcode target built for the generic iOS Simulator with Xcode 27.2 and `CODE_SIGNING_ALLOWED=NO`. This includes the SwiftUI application, not only the standalone model checks. No live account was used. Simulator interaction, real-device acceptance, signing, notarization and store distribution are separate remaining checks.

## Desktop validation

On the final source, `cd desktop && ./gradlew --no-daemon --max-workers=2 test packageDmg` passed: 32 tests, zero failures/errors/skips, and a macOS DMG built with the bundled Java runtime.

Artifact: `desktop/build/compose/binaries/main/dmg/etabli-plume-1.0.0.dmg` (67,500,650 bytes). SHA-256: `4a68074dc9920e68e44197e62684ef8250c5a42abede4637b8de2cc446bb6a14`.

The pre-refinement desktop package workflow also [passed on all three CI platforms](https://github.com/etabli-dev/etabli-plume/actions/runs/42525618194). A fresh workflow at the new commit remains separate from that baseline result. The local DMG is not Developer ID signed or notarized; installed interactive acceptance remains.

## Desktop packaging roadmap follow-up

`desktop/VERSION` is the single desktop version source (1.0.0, matching the
existing macOS bundle). Linux CI measures the running app's WM_CLASS under Xvfb,
then refines the Debian desktop entry's display name and startup class and
regenerates its integrity metadata. The final entry is validated before upload.
The raw Gradle Debian artifact requires this documented refinement step.

The desktop workflow now also handles exact `v<version>` tags. Only its final
release job has write permission; it downloads the three named package
artifacts, rejects missing/empty/stale-version packages, writes SHA256SUMS and
creates a draft for owner review. No tag or release was created during this
audit. Eight Python checks and a synthetic smoke-script harness passed; actual
Linux package/WM_CLASS validation and draft-release acceptance require their
respective CI run and tag.

The local `test packageDmg` check succeeded after these packaging changes. The
Kotlin test task reused its unchanged, passing 32-test result; the eight Python
checks ran afresh. The rebuilt arm64 DMG passed `hdiutil verify`: 67,510,485 bytes,
SHA-256 `3dead557f84f8261981a5c09792d49f2c1b157926256a63d2ae16f41c6728ffd`.
This supersedes the earlier artifact above for the current version-source change.

The first packaging follow-up CI passed Windows/macOS but Linux's old fixed
10-second/title lookup found no matching window. The smoke now waits for a
visible window owned by the launched PID (30-second bound), looks up WM_CLASS by
window ID and uses Skiko's supported software renderer only in the isolated
Xvfb process. Synthetic success and missing-window tests verify PID cleanup on
both paths. The real Linux repeat remains the acceptance gate.
