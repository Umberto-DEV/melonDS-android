# Preserve the native lid-pause deadline across Activity stop

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Functional impact:** Moderate: missed or repeated emulated pause. No CVSS/security score is assigned.

## Problem and path

Activity stop could lose the scheduled lid-close pause; repeated sleep events or stale delayed callbacks could affect the next session.

## Correction

Preserve the original native pause deadline, keep repeated screen-off from extending it, cancel on quick resume and consume/close pending work once on destruction.

## Evidence and limits

The permanent LidClosePauseControllerTest checks the 2,999/3,000 ms boundary, stop/render/grace-service ownership, quick resume, repeated sleep and idempotent destruction. These JVM assertions were included in historical refinement acceptance. The feature adapts DLCSharp PR #1589; physical sleep/audio/game-time behavior remains a separate test.

The current release identity and exact-APK smoke are separate from these
historical regression results. No new build/device run is claimed by this
documentation consolidation.

## Source and regression tests

- [LidClosePauseController.kt](../../../app/src/main/java/me/magnum/melonds/ui/emulator/LidClosePauseController.kt)
- [LidClosePauseControllerTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/emulator/LidClosePauseControllerTest.kt)
