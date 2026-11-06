# Clear obsolete constraints when moving the FPS counter

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Functional impact:** Low: counter positioned in the wrong location. No CVSS/security score is assigned.

## Problem and path

Repositioning the same FPS counter retained old layout constraints, so a later selected corner/center did not reliably determine its final coordinates.

## Correction

Rebuild the relevant constraints for the new placement rather than accumulating the previous anchors.

## Evidence and limits

The permanent Robolectric test lays out the same counter in a 320×240 container and checks exact x/y coordinates for all six corner/center positions. Historical refinement acceptance includes this regression. Counter placement is separate from emulation FPS or frame-time performance.

The current release identity and exact-APK smoke are separate from these
historical regression results. No new build/device run is claimed by this
documentation consolidation.

## Source and regression tests

- [FpsCounterPositioning.kt](../../../app/src/main/java/me/magnum/melonds/ui/emulator/FpsCounterPositioning.kt)
- [FpsCounterPositioningTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/emulator/FpsCounterPositioningTest.kt)
