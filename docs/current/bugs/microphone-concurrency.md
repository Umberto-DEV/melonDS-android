# Serialize microphone source and session changes

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: unsafe concurrent lifetime. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Toggle/configuration/teardown paths accessed capture state and streams without coherent ownership while callbacks could still be pending. Atomic flags alone did not serialize source/session transitions.

## Correction

Serialize control operations, retain atomic callback flags and avoid adding a new mutex in the real-time callback.

## Evidence and controls

Eight controlled TSan scenarios exercise the production translation unit, with negative controls and ASan/UBSan checks. Reproducer scripts retain their production-boundary extraction/adapters.

## Limits and provenance

Controlled schedules do not cover every real-time interleaving or an actual driver. No latency or energy improvement was measured.

## Source and regression tests

- [MelonDSAudio.cpp](../../../app/src/main/cpp/MelonDSAudio.cpp)
- [MicConcurrencyTest.cpp](../../../tools/host-tests/microphone/lifecycle/MicConcurrencyTest.cpp)
- [README.md](../../../tools/host-tests/microphone/lifecycle/README.md)
