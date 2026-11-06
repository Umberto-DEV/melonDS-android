# Keep JIT teardown and invalidation ownership valid

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: native lifetime error or stale guest code. Functional impact labels are not CVSS/security ratings.

The original source/test identities and limitations are retained below. The current release and exact-APK execution are recorded separately.

## Problem and reachable path

JIT teardown could use already destroyed state; DTCM alias/literal invalidation could leave compiled code using an old value. These shared-core causes predate the current app documentation.

## Correction

Preserve explicit JIT lifetime order, precise invalidation granules and address ownership checks; distinguish the later Thumb first-fold correction from earlier DTCM mapping and mask changes.

## Evidence and controls

Strict UBSan teardown regression and original synthetic invalidation controls are retained. The synthetic test rewrites a guest literal, checks 16-byte granule precision and refuses folding an unowned DTCM address; the separate Thumb dossier retains the fresh before/after source pair.

## Limits and provenance

Shared-core issue #2537 is technical provenance, not an upstream closure claim. The real-ROM JIT/interpreter differential requires MELONDS_TEST_ROM and skips without it; synthetic controls do not establish AiRace/Super Mario256 compatibility.

## Source and regression tests

- [JitInvalidationTest.cpp](../../../tools/host-tests/jit/JitInvalidationTest.cpp)
- [JitDifferentialTest.cpp](../../../tools/host-tests/jit/JitDifferentialTest.cpp)
- [README.md](../../../tools/README.md)
