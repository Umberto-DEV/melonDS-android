# Update touchscreen coordinates when stationary fingers change

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: incorrect input position. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Adding or lifting a stationary finger left old coordinates until the next MOVE event. Interrupted touch/controller ownership could leave input pressed.

## Correction

Recompute the contact center on pointer transitions, exclude only the lifted pointer and release interrupted input according to its owner.

## Evidence and controls

Real MotionEvent tests on JVM/Android cover two/three fingers, index zero/one, single press/release and cancellation. Separate virtual-button regressions retain existing upstream multi-pointer behavior.

## Limits and provenance

This does not measure input-to-photon latency or prove every fast-slide report (#1141/#1424/core #8). Physical controller acceptance is separate.

## Source and regression tests

- [TouchscreenPointerTransitionContract.kt](../../../app/src/sharedTest/kotlin/me/magnum/melonds/ui/emulator/input/TouchscreenPointerTransitionContract.kt)
- [InputProcessorTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/emulator/input/InputProcessorTest.kt)
