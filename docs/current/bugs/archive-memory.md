# Bound archive decoding and traversal

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: memory exhaustion or unbounded work. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

7z decoding and archive traversal could allocate excessive memory, overlap extractions or trust declared sizes without enforcing the actual decoded/cache budget.

## Correction

Serialize extraction, derive heap-aware SevenZ limits, bound traversal and actual expanded bytes, and use transactional cache publication. Current source also retains upstream large-heap and extraction-failure notices.

## Evidence and controls

SevenZ decoder/limit and ROM-cache regressions exercise memory budgets, null/unknown ZIP size, closure, quota and failure paths. The adjacent cache regression measured 40 bytes under a 32-byte budget before pre-write eviction; afterward the peak stayed at or below 32 bytes.

## Limits and provenance

Oversized archives may need manual extraction. The Android large-heap request is not proof of extra device memory; no universal OOM or real Thor-library acceptance is claimed. Upstream #1524 attribution is retained in the feature record.

## Source and regression tests

- [SevenZMemoryLimitTest.kt](../../../app/src/test/java/me/magnum/melonds/common/romprocessors/SevenZMemoryLimitTest.kt)
- [SevenZRomFileProcessorTest.kt](../../../app/src/test/java/me/magnum/melonds/common/romprocessors/SevenZRomFileProcessorTest.kt)
- [NdsRomCacheSecurityTest.kt](../../../app/src/test/java/me/magnum/melonds/impl/NdsRomCacheSecurityTest.kt)
