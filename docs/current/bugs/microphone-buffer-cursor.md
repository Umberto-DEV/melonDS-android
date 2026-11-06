# Microphone capture cursor belongs to its buffer

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: crash or stale capture. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Switching blow input to microphone, or starting another capture session, reused a cursor owned by a different buffer. The observed invalid copy length was −952 bytes; old samples could also reappear.

## Correction

Keep each capture cursor with its owning session buffer; reset and advance it according to that buffer, including wrap and disabled-input paths.

## Evidence and controls

Production C++ source-switch/session tests reproduced the negative copy before correction, then passed source switch, new session, wrap, silence and disabled controls on host and Android native targets. Stream opening is replaced by a declared adapter in lifecycle tests.

## Limits and provenance

Hardware stream opening is an adapter; these results do not establish a Bluetooth/resume issue cause or physical-driver acceptance.

## Source and regression tests

- [MelonDSAudio.cpp](../../../app/src/main/cpp/MelonDSAudio.cpp)
- [MicSourceSwitchTest.cpp](../../../tools/host-tests/microphone/lifecycle/MicSourceSwitchTest.cpp)
