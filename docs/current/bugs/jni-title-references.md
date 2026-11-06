# Bound JNI local references while listing DSi titles

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Functional impact:** Moderate: native reference exhaustion. No CVSS/security score is assigned.

## Problem and path

DSiWare title enumeration retained temporary JNI references for every title, allowing local-reference use to grow with list length. This corresponds to the precise code cause reported in upstream Android #1678.

## Correction

Release each temporary local reference while retaining the returned list and its title objects/content. Keep string/metadata bounds and UTF leases as separate contracts.

## Evidence and limits

The retained production JNI translation-unit harness enumerates 0, 1, 300 and 1,500 titles, checks complete title/producer/icon content and order, one live returned-list reference, peak local references at most seven and zero after final deletion. It is a modeled JNIEnv/core boundary, separate from actual ART acceptance. Historical #1678 local correction does not assert upstream issue closure.

The current release identity and exact-APK smoke are separate from these
historical regression results. No new build/device run is claimed by this
documentation consolidation.

## Source and regression tests

- [MelonDSNandJNI.cpp](../../../app/src/main/cpp/MelonDSNandJNI.cpp)
- [NandJniTest.cpp](../../../tools/host-tests/nand-jni/NandJniTest.cpp)
