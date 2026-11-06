# Restore both DS screens after display disconnection

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: missing DS screen. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

External display removal could leave one DS screen absent. Reconnection with another display ID combined with rotated insets also broke custom layout selection.

## Correction

Use external layouts only for compatible displays, fall back when the target disappears and preserve intentional hidden-screen/custom-layout semantics on reconnect/rotation.

## Evidence and controls

The real provider and layout generator cover disconnect, intentionally hidden screen, reconnect and reconnect plus inset rotation on JVM and Android. The review-found reconnect/rotation case was reproduced before the correction.

## Limits and provenance

Physical HDMI/casting behavior is not certified. This does not implement the mirroring/toggle features requested in #1663/#1641/#1619.

## Source and regression tests

- [UILayoutProviderDisplayLifecycleContract.kt](../../../app/src/sharedTest/kotlin/me/magnum/melonds/impl/layout/UILayoutProviderDisplayLifecycleContract.kt)
- [UILayoutProviderDisplayLifecycleTest.kt](../../../app/src/test/java/me/magnum/melonds/impl/layout/UILayoutProviderDisplayLifecycleTest.kt)
