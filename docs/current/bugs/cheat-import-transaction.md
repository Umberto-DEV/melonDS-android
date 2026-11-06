# Failed cheat imports preserve the previous database

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: cheat database loss. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

XML/DAT import deleted the old database before parsing/writing completed. Parse failure, SQLite error or cancellation could therefore remove working data.

## Correction

Commit replacement in a Room transaction, bound producer buffering and run parsing outside the transaction executor. Preserve customized cheat rows and support a later successful retry.

## Evidence and controls

The real worker/parser/ContentResolver/SQLite run on JVM and ART with valid XML/DAT, truncation, missing header, SQLite failure and producer/consumer cancellation. Tests compare all rows, retain custom cheats and retry. The 100-game import protects against executor deadlock.

## Limits and provenance

WorkManager scheduling was initially an adapter; actual scheduling has its own dossier. Historical #1367 was already closed/duplicated and is not claimed as a newly fixed issue.

## Source and regression tests

- [CheatImportWorker.kt](../../../app/src/main/java/me/magnum/melonds/common/workers/CheatImportWorker.kt)
- [CheatImportIntegrityContract.kt](../../../app/src/sharedTest/kotlin/me/magnum/melonds/common/workers/CheatImportIntegrityContract.kt)
