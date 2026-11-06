# Publish only a complete guided BIOS/NAND copy

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: incomplete system file adoption. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Guided setup could hide unreadable-source errors or adopt an invalid copy; a second setup could adopt a 1,024/32,768-byte partial NAND left by an interrupted operation.

## Correction

Validate before setting preferences, serialize setup ownership, use a unique noncanonical `.part` document, close both streams before rename, reject rename refusal and name any undeletable residual. Permit a complete retry.

## Evidence and controls

Thirty BIOS JVM tests include Fragment/verifier/preferences and production staging/lifecycle regressions. Four real SAF/Binder Android controls check exact bytes, source preservation, closure before publication, cancellation/error cleanup and retry. Prior lifecycle/staging REDs preceded the correction.

## Limits and provenance

Permanently blocked external I/O must return before cleanup can finish. Full Activity rotation during a slow external-provider copy, provider mutation races and dump authenticity remain separate.

## Source and regression tests

- [BiosGuidedFolderSetup.kt](../../../app/src/main/java/me/magnum/melonds/common/BiosGuidedFolderSetup.kt)
- [BiosGuidedFolderSetupTest.kt](../../../app/src/test/java/me/magnum/melonds/common/BiosGuidedFolderSetupTest.kt)
- [BiosGuidedSetupIntegrationTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/settings/BiosGuidedSetupIntegrationTest.kt)
