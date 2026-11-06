# Limit coroutine effects to the validated emulator session

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Functional impact:** Moderate: stale or premature session work. No CVSS/security score is assigned.

## Problem and path

The emulator scope was active before the first validated session; suspended work could survive session replacement or cleanup and affect another session.

## Correction

Activate a scope only for a validated session, cancel the prior generation on replacement and reject work after cleanup until reactivation.

## Evidence and limits

The permanent production-class test checks no effects before activation, cancellation of a suspended previous generation while new work proceeds, and cancellation/rejection after cleanup. Historical refinement app 89053f0fd96ef2bc616849deb15900e1d6b09f43/core fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e included these controls. It does not prove complete Android process-death restoration.

The current release identity and exact-APK smoke are separate from these
historical regression results. No new build/device run is claimed by this
documentation consolidation.

## Source and regression tests

- [EmulatorSessionCoroutineScope.kt](../../../app/src/main/java/me/magnum/melonds/ui/emulator/EmulatorSessionCoroutineScope.kt)
- [EmulatorSessionCoroutineScopeTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/emulator/EmulatorSessionCoroutineScopeTest.kt)
