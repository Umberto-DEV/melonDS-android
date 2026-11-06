# Keep cheat selector actions visible with large text and keyboard

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: inaccessible controls. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Titles/controls clipped with large text, touch targets were 36 dp, radio actions were duplicated and landscape IME could leave an effectively empty page. Light dialog status icons also became white on white.

## Correction

Use 48 dp targets, a single radio action, adaptive scrollable content and explicit Done behavior; update dialog-owned system-bar contrast and restore it on disposal.

## Evidence and controls

Twenty-four UI tests were part of the 267-test refinement. Native IT/EN captures covered 320 dp portrait/640 dp landscape, light/dark themes, 100/130/200% text and the real IME. Done restored commands. The final contrast regression was captured and corrected.

## Limits and provenance

Spoken TalkBack, physical gamepad navigation and all display sizes remain outside that acceptance. Screenshots are inspected evidence, not a substitute for behavioral accessibility checks.

## Source and regression tests

- [ModifierFamilyDialog.kt](../../../app/src/main/java/me/magnum/melonds/ui/cheats/ui/ModifierFamilyDialog.kt)
- [ModifierFamilyDialogTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/cheats/ModifierFamilyDialogTest.kt)
