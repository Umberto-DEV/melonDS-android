# Retain pending save writes when file I/O fails

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: lost game progress. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Native SaveManager flushes could use truncating opens or clear pending work despite a failed write. The path reaches the current raw save through native file-mode/provider access.

## Correction

Prefer non-truncating access to the existing file, report failures and retain pending work for retry. Save-range checks reject invalid offset/length requests.

## Evidence and controls

File-mode and SaveManager production tests cover failure/retry and range controls; invalid offset=33/length=4/size=32 is rejected while a valid 32-byte control persists. Current public-APK raw Continue → SAVE → cold Continue evidence is recorded separately in the post-release smoke.

## Limits and provenance

Some SAF fallback providers can still truncate. This is not universal atomic replacement, power-loss durability or proof that every provider preserves prior bytes.

## Source and regression tests

- [SaveManager.cpp](../../../app/src/main/cpp/SaveManager.cpp)
- [SaveManagerFlushTest.cpp](../../../app/src/main/cpp/SaveManagerFlushTest.cpp)
