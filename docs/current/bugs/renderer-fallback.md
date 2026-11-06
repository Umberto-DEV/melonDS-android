# Dispatch frames using the renderer actually created

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: invalid native cast or blank output. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

An accelerated factory could fail and the core install Software while the frontend retained the requested accelerated enum. Later settings calls downcast the Software object as an accelerated renderer.

## Correction

Record the actual Software renderer and return before incompatible settings calls. Frame/screenshot dispatch follows the actual renderer; keep the requested preference for a later supported launch.

## Evidence and controls

Ten extracted-production-body host/Android scenarios retain typed factories. The original function had seven sanitizer failures and three success controls; initial factory failures reproduced invalid downcasts on Android. Whole-app API33/GLES3.1 Software, Compute-failure and OpenGL-failure launches separately passed two advancing original-homebrew phases and presentation.

## Limits and provenance

The adapter controls differ from real GLES. The whole-app check does not establish accelerated quality/speed, every driver-specific resume report, or the cause of an older Thor APK crash.

## Source and regression tests

- [README.md](../../../tools/host-tests/renderer-fallback/README.md)
- [README.md](../../../tools/android-tests/renderer-fixture/README.md)
