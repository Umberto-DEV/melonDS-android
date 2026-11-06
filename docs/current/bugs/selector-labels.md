# Use labels that match the cheat option type

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Low functional impact: misleading selector instructions. Functional impact labels are not CVSS/security ratings.

Historical UI evidence collected on 4 November 2026 for the final 2.1.8 UD release. It is retained as a bug regression, not a current download/version claim.

## Problem and reachable path

Non-species options displayed species prompts or game-specific help. This was a fork-added UI defect, independent of cheat value selection.

## Correction

Use option-specific level/species/nature/starter labels and present one clear radio action without changing codes, search/activation behavior or data formats.

## Evidence and controls

The 4 November actual release UI reproduced the wrong labels. Four Compose cases were RED; 42 focused cases and 349 full JVM/API tests passed after correction, and final release UI was checked again.

## Limits and provenance

This usability correction is outside the Security occurrence count. Physical controller/spoken TalkBack acceptance is separate.

## Source and regression tests

- [ModifierFamilyDialog.kt](../../../app/src/main/java/me/magnum/melonds/ui/cheats/ui/ModifierFamilyDialog.kt)
- [ModifierFamilyDialogTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/cheats/ModifierFamilyDialogTest.kt)
