# Load library icons without blocking unrelated rows

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Low functional impact: blocked or stale library icons. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

One icon load could delay unrelated rows and stale asynchronous results could outlive the current request.

## Correction

Load icons independently with explicit request ownership and preserve the applicable icon/cache contract.

## Evidence and controls

RomIconProvider production regressions cover independent loading and cancellation/cache boundaries; source and tests remain available below.

## Limits and provenance

No generalized scan/startup latency gain, arbitrary provider responsiveness or new artwork support is claimed.

## Source and regression tests

- [RomIconProviderTest.kt](../../../app/src/test/java/me/magnum/melonds/impl/RomIconProviderTest.kt)
