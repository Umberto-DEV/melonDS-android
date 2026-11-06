# Release every successful JNI UTF acquisition

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: native resource leak. Functional impact labels are not CVSS/security ratings.

The original source/test identities and limitations are retained below. The current release and exact-APK execution are recorded separately.

## Problem and reachable path

DSiWare import/export released GetStringUTFChars only when isCopy was true. That flag describes representation and does not waive the JNI release requirement. Failed/null acquisitions could also continue into the core operation.

## Correction

Release every non-null acquisition exactly once regardless of isCopy and return false when acquisition fails.

## Evidence and controls

The production JNI translation unit with an instrumented JNIEnv returned isCopy=false: before correction acquired=2/released=0. A second RED showed null acquisition still reporting success. UtfLeaseTest and the full JNI suite passed under ASan/UBSan; Android native compilation/execution coverage is separated in the validation record.

## Limits and provenance

The initial host regression is a JNI contract test, not an ART pin/lease profiler measurement. Original evidence identity is the 31 October app/core pair recorded in the save-replacement dossier.

## Source and regression tests

- [MelonDSNandJNI.cpp](../../../app/src/main/cpp/MelonDSNandJNI.cpp)
- [UtfLeaseTest.cpp](../../../tools/host-tests/nand-jni/UtfLeaseTest.cpp)
