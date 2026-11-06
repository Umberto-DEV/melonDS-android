# Cancel a cheat import blocked in provider I/O

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: stuck work and notification. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Cancellation joined a parser whose stream read ignored interrupts. Work and foreground notification could stay active, blocking later unique KEEP imports.

## Correction

Close the owned stream before joining and propagate cancellation while provider opening is pending. Keep a single operation owner and allow a fresh import after termination.

## Evidence and controls

Remote pipe/PFD provider fixtures establish actual blocked I/O, then require cancellation completion before providing EOF. Real WorkManager, unique KEEP behavior, notification removal, identical database contents and a later KEEP success passed on API33 ARM64 4 KiB and API35 ARM64 4/16 KiB.

## Limits and provenance

The provider cooperation contract is bounded: arbitrary metadata calls or permanently unresponsive external I/O are not guaranteed interruptible.

## Source and regression tests

- [CheatImportWorker.kt](../../../app/src/main/java/me/magnum/melonds/common/workers/CheatImportWorker.kt)
- [CloseAwareBlockingInputStream.kt](../../../app/src/sharedTest/kotlin/me/magnum/melonds/common/cheats/CloseAwareBlockingInputStream.kt)
