# Check audio stream ownership on open and resume

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: crash or silent output. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

A failed audio open could dereference a null stream. Resume/mute/session transitions had fragile ownership and policy ordering.

## Correction

Check stream-open results before access and apply resume/fast-forward policies in ownership order, including teardown.

## Evidence and controls

Production audio-policy and lifecycle controls pass, with host/native evidence kept distinct from listening tests. The original source review found and guarded the failed-open path.

## Limits and provenance

Headphone/Bluetooth routes and physical fidelity are not certified. Optional mute adapted from dysprosium #1625 is a user option, not a resampler/rate-matching fix; #1644/#1635 remain broader.

## Source and regression tests

- [MelonDSAudio.cpp](../../../app/src/main/cpp/MelonDSAudio.cpp)
- [AudioOutputPolicy.cpp](../../../app/src/main/cpp/AudioOutputPolicy.cpp)
- [AudioOutputPolicyTest.cpp](../../../app/src/main/cpp/AudioOutputPolicyTest.cpp)
