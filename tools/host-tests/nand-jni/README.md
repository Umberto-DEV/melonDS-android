# DSi NAND JNI local-reference regression

Run `sh tools/host-tests/nand-jni/run.sh` from the repository root. Requires a C++17 compiler with ASan/UBSan (tested with Apple clang). No Android SDK, game ROM or NAND image is needed.

The runner copies the current **unchanged production** `MelonDSNandJNI.cpp` into a temporary build directory so its quoted dependency headers resolve to the adjacent boundary stubs. The test executes real `getTitleData` and `listTitles`; it does not reimplement those functions or grep their source. It models local JNI handles separately from objects retained by Java fields/list entries. It verifies the returned list remains a live local, preserves every title ID/order, name/producer and 4096-byte icon, and uses constant temporary local references at 0, 1, 300 and 1500 titles. The direct helper must return exactly one local.

The JNI/core boundary is simulated. This test is **not** Android CheckJNI, an ART reference-table limit measurement, real NAND/title parsing, or an allocation/exception-injection test. The actual JNI method signatures and constructor remain unchanged; the Kotlin `DSiWareTitle` stores its constructor inputs.

For a regression demonstration an optional source-file argument can point to an exported pre-fix production file:

```
git show 39a9fe56:app/src/main/cpp/MelonDSNandJNI.cpp > /tmp/MelonDSNandJNI-before.cpp
sh tools/host-tests/nand-jni/run.sh /tmp/MelonDSNandJNI-before.cpp
```

Recorded 2026-10-24 with ASan+UBSan:

| Case | Before live/peak locals | After live/peak locals |
|---|---:|---:|
| Direct one-title helper | 5 / 5 | 1 / 5 |
| Empty title list | 2 / 2 | 1 / 2 |
| One-title list | 7 / 7 | 1 / 6 |
| 300-title list | 1502 / 1502 | 1 / 6 |
| 1500-title list | 7502 / 7502 | 1 / 6 |

Before exits 1 (`temporary JNI local references leaked or grew with title count`); patched source exits 0 and all content checks pass. No arbitrary 512-local ART limit is assumed.
