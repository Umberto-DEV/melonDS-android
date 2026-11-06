# Serialize library scans and preserve stable snapshots

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: stuck scanning or inconsistent lists. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

An exception could leave SCANNING active; refreshes overlapped; already emitted lists could mutate. Failed provider queries risked replacing a valid cache.

## Correction

Return to idle in finally, serialize scans, emit stable snapshots and preserve the prior cache on failed queries.

## Evidence and controls

Six initial regressions preceded the correction; nine scan/sort tests passed in the refinement campaign. Production repository tests retain serialization/error/retry and later selective-removal controls.

## Limits and provenance

This is not a normal-startup speed benchmark or proof that every large Thor library/provider/OOM report (#1519/#1378/#540) is resolved.

## Source and regression tests

- [FileSystemRomsRepository.kt](../../../app/src/main/java/me/magnum/melonds/impl/FileSystemRomsRepository.kt)
- [FileSystemRomsRepositoryTest.kt](../../../app/src/test/java/me/magnum/melonds/impl/FileSystemRomsRepositoryTest.kt)
