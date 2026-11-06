# Validate BIOS and DSi title metadata before NAND mutation

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: invalid or partial title data. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

Missing NAND was reported as success; short BIOS and TMD files or a TMD for another title could be adopted. Failed-import cleanup passed category/title identifiers in the wrong order.

## Correction

Validate required inputs and matching title metadata before mutation, check complete reads/writes and use the correct cleanup identifiers. Core/FatFs transactional title handling remains a separate layer.

## Evidence and controls

Real JNI in the diagnostic APK rejects invalid NAND/BIOS; production JNI with a declared NAND model covers temporary files, duplicate, partial-error and success paths. ASan reproduced a TMD overread. The JNI/native regressions are retained.

## Limits and provenance

The modeled NAND boundary is explicit. DSi HOME boot and logical title quota are not established by these checks; #1651/#1099 are only partially addressed.

## Source and regression tests

- [MelonDSNandJNI.cpp](../../../app/src/main/cpp/MelonDSNandJNI.cpp)
- [NandJniTest.cpp](../../../tools/host-tests/nand-jni/NandJniTest.cpp)
- [StorageNandTest.cpp](../../../tools/host-tests/nand-jni/StorageNandTest.cpp)
- [NandFatTest.cpp](../../../tools/host-tests/nand-fat/NandFatTest.cpp)
