# Microphone gain must saturate before narrowing

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: distorted microphone input. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Amplified PCM conversion exceeded signed 16-bit range and inverted sample signs. The reachable path is the production Oboe microphone callback and gain conversion.

## Correction

Saturate the amplified value before converting to `s16`; retain the configured gain.

## Evidence and controls

Eleven independently calculated PCM inputs were checked; six failed before correction and UBSan reported overflow. The real callback also executed with Oboe on Android.

## Limits and provenance

Numerical/callback acceptance does not calibrate a physical microphone, add a gain slider or certify Bluetooth. Related upstream #1679/#1285 remain broader reports.

## Source and regression tests

- [MicInputOboeCallback.cpp](../../../app/src/main/cpp/MicInputOboeCallback.cpp)
- [MicInputGainTest.cpp](../../../tools/host-tests/microphone/MicInputGainTest.cpp)
