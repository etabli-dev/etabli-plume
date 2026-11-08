# Plume: Apple build and recovery audit, 2026-11-08

Repeated recovery from unreadable desktop preferences now preserves each damaged file. Previously, a second recovery replaced the first `.corrupt` backup. The first backup retains its existing name; subsequent backups receive unique names. A regression test exercises two recoveries and verifies both originals and the usable preferences remain.

## Native iOS validation

The existing Swift validation scripts passed, and the full Xcode target built for the generic iOS Simulator with Xcode 27.2 and `CODE_SIGNING_ALLOWED=NO`. This includes the SwiftUI application, not only the standalone model checks. No live account was used. Simulator interaction, real-device acceptance, signing, notarization and store distribution are separate remaining checks.

## Desktop validation

On the final source, `cd desktop && ./gradlew --no-daemon --max-workers=2 test packageDmg` passed: 32 tests, zero failures/errors/skips, and a macOS DMG built with the bundled Java runtime.

Artifact: `desktop/build/compose/binaries/main/dmg/etabli-plume-1.0.0.dmg` (67,500,650 bytes). SHA-256: `4a68074dc9920e68e44197e62684ef8250c5a42abede4637b8de2cc446bb6a14`.

The pre-refinement desktop package workflow also [passed on all three CI platforms](https://github.com/etabli-dev/etabli-plume/actions/runs/42525618194). A fresh workflow at the new commit remains separate from that baseline result. The local DMG is not Developer ID signed or notarized; installed interactive acceptance remains.
