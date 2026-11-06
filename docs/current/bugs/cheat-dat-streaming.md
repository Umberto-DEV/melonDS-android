# DAT parsing must make progress on fragmented input and EOF

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: import hang or rejected data. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Fragmented reads were rejected; skip at EOF could loop; invalid offsets/missing blocks were accepted as empty games; progress could move backward and a canceled consumer could leave the producer suspended.

## Correction

Complete fixed reads, validate offsets/blocks and EOF, enforce monotonic progress and propagate cancellation across the parser pipeline.

## Evidence and controls

An authored valid 356-byte DAT, seven-byte fragments, truncations/offsets and expected code bytes exercise the real parser on JVM and ART. A real synthetic 100-game stream and callback cancellation cover the producer/consumer schedule. Review-found exact-EOF/cancellation cases were rerun RED before correction.

## Limits and provenance

DAT was already supported upstream (#1631). Parser correctness does not make a code compatible with a different game/ROM revision or establish a game-specific cheat-crash fix.

## Source and regression tests

- [UsrCheatDatabaseParser.kt](../../../app/src/main/java/me/magnum/melonds/common/cheats/UsrCheatDatabaseParser.kt)
- [UsrCheatStreamingTest.kt](../../../app/src/sharedTest/kotlin/me/magnum/melonds/common/cheats/UsrCheatStreamingTest.kt)
