# Thumb/DTCM literal ownership on AArch64

**Functional impact:** stale guest data. No security score is assigned to this
functional audit. Evidence was collected on 2 November 2026, app
`1cc24ead4ba0f1e733b5d828df6a4b23ccf081b0`, initial core `403c4683`, then
corrected core `f76940fc`. Current release identities are recorded separately.
The corrected unit below retains its historical test outputs and limitations;
it does not claim current device acceptance or repository-wide safety.

## Closed unit: CORE-LITERAL-A64

### Finding and proof contract

- ID/origin: `CORE-LITERAL-A64`, derived from the local JIT audit and compared
  with official GitHub PRs #11/#12 and shared-core #2746/#2747.
- Owner/caller/consumer: `T_Comp_LoadPCRel` calls
  `Compiler::Comp_MemLoadLiteral`; the generated AArch64 block consumes the
  folded value and the guest program publishes it to main RAM.
- Reachable prerequisites: Android production enables the AArch64 JIT and
  literal optimization; an ARM9 Thumb PC-relative literal can address DTCM.
- Expected: each load reads the current DTCM word A, then B, then C.
- Observed before: with JIT/folding ON/fastmem OFF the result and R0 stayed A
  after B and C. Exit 1. Folding OFF and interpreter read A/B/C; the analogous
  ARM literal and DTCM mapping/remapping controls were green.
- First wrong transition: `LocaliseCodeAddress` returns zero for DTCM, but the
  direct Thumb helper still embeds the value. Zero denotes no invalidation
  owner and must not be foldable.
- Confutation 1: identical Thumb guest with folding OFF, green.
- Confutation 2: identical Thumb guest through the interpreter, green.
- Additional control: ARM PC-relative DTCM load, green. Fast memory remained
  OFF for the selected target.
- Proof level: shared-core component executed on host AArch64 and Android NDK
  API35 ARM64. No game attribution, ART integration or physical-device claim.

The official GitHub API was reread on 2 November. PRs
`rafaelvcaetano/melonDS-android-lib#11` and `#12`, and
`melonDS-emu/melonDS#2746` and `#2747`, remain closed and unmerged. #12/#2746/
#2747 align the second lookup after invalidation; #11 fixes a distinct
invalidation mask. Their patches do not reject this first unlocalisable fold.
Original authorship and the distinction between fixes are preserved.

### Minimal correction

`Comp_MemLoadLiteral` now returns `false` when `LocaliseCodeAddress` returns
zero. This makes the existing runtime load path handle addresses without an
invalidation owner. No decoder, persistent format, vendor or frontend code was
changed.

Correction commit: core `f76940fc`. This identifies the historical correction,
not the current repository or publication state.

### Reproducible commands and results

```text
REQUIRE_CORE_TESTS=1 ./tools/host-tests.sh
./tools/core-host-sanitize.sh
clang++ -std=c++17 -Wall -Wextra -fsanitize=address,undefined -g -O1 ...
./build/host-tests/JitInvalidationTest
cmake --build app/build/android-native-tests/arm64-v8a --target JitInvalidationTest --parallel 8
./tools/android-tests/run.sh --serial emulator-5554 --abi arm64-v8a --test JitInvalidationTest
./tools/android-tests/run.sh --serial emulator-5556 --abi arm64-v8a --test JitInvalidationTest
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home bash ./gradlew :app:testGitHubProdDebugUnitTest :app:lintGitHubProdDebug :app:assembleGitHubProdDebug --stacktrace --console=plain
```

| Layer | Before | After |
| --- | --- | --- |
| Host AArch64 ASan/UBSan | valid out-of-sandbox run: target RED, exit 1; controls green | target and controls PASS, exit 0 |
| Host full suite | first sandbox run invalid because `shm_open` was denied | exit 0; all named tests PASS except ROM differential SKIP |
| NDK ARM64 build | historical RED binary only | build PASS, ELF SHA-256 `d36384dd5497a0ddb1415b35dd524836971dfa44d87809f29f7b14cdb88bf5f2` |
| API35 ARM64 4 KiB | historical RED | executed PASS, exit 0; page size 4096 |
| API35 ARM64 16 KiB | historical RED | executed PASS, exit 0; page size 16384 |
| JVM/Robolectric | not causal to RED | `testGitHubProdDebugUnitTest` PASS |
| Android lint | not causal to RED | PASS; warnings retained in generated report |
| APK build/package | not causal to RED | GitHubProdDebug assemble and signature verification PASS |
| ART/instrumentation | not run | SKIP: no JNI/Kotlin behavior changed |
| Physical device/game | not run | SKIP: not needed and not authorized |

The Android runner found no residual `melonds-core-tests.*` directory before
each AVD was stopped. Repeated `ftruncate(EINVAL)` is an AVD fast-memory
diagnostic, not the oracle; the selected case asserts fastmem OFF.

Host `JitDifferentialTest` is **SKIP** because `MELONDS_TEST_ROM` was not set;
no ROM was introduced. Toolchain warnings that the selected macOS libc++ target
is no longer supported and existing Kotlin deprecations/unchecked casts remain
warnings, not a pristine-output claim.

## Artifacts, hashes and receipt

- Reproducer source SHA-256:
  `4af1fd18eea42710fd7a66db89e5dfce293c4446c99eeaef318b78e97bb949ae`.
- Fixed production source SHA-256:
  `2dc060183d40ca0d437d9c8ad0b1f77a254f76e0acff412d9900d035e3bdbbd6`.
- Host ASan/UBSan executable SHA-256:
  `0c2d301387dd7f297dda2913454300cf7f41de95b253171c6d30ddf9ced1f428`.
- Android NDK executable SHA-256:
  `d36384dd5497a0ddb1415b35dd524836971dfa44d87809f29f7b14cdb88bf5f2`.

## Remaining limits

The real-ROM JIT differential was skipped without `MELONDS_TEST_ROM`.
No ART, physical device or game-specific failure is established here. The later
Linux x86-64 first-fold omission and its real CI RED/GREEN are documented in
[the second security campaign](../security/SECOND-CAMPAIGN.md#post-push-controls-and-counterevidence);
it is separate from this Android ARM64 correction and outside the scan count.
[Scan provenance](../security/SCAN-PROVENANCE.md) retains the canceled historical
scan status independently of this dynamically verified functional unit.
