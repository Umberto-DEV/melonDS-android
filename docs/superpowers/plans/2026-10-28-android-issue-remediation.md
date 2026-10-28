# Android Issue Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for implementation, with independent diagnostic agents and a final fresh review. User explicitly requests continuous execution on main.

**Goal:** Correct confirmed Android defects with genuine baseline failures and category/integrated checks, preserving traceability for an unpublished future PR.

**Architecture:** Reuse production C++/Kotlin paths in focused regressions; mock only external audio, network or storage boundaries. Run real NDK binaries and Android instrumentation on ARM64. Keep hardware/game-dependent claims open.

**Tech Stack:** Kotlin, AndroidX tests, Robolectric, C++17, Oboe, NDK 28, Android API35 ARM64 4/16KiB.

**Spec:** `docs/superpowers/specs/2026-10-28-android-issue-remediation-design.md`

## Global Constraints
- Main only in app/core; preserve local changes and attribution.
- No new PR, Daybreak or release publication. Own-main pushes for CI remain authorized.
- No production change without its failing regression; no commercial ROM/BIOS download or original-save modification.
- Report Android, host diagnostics, skipped checks and hardware limitations separately.
- No broad cleanup or performance claims without measurement.

## Review Focus
- Loud microphone samples retain polarity at both signed limits; quiet samples and silence stay correct (Task 1).
- Repeated microphone/session changes never reuse another buffer's cursor or stale samples (Task 2).
- NAND access failure, partial import and mismatched metadata never report success or delete another title (Task 3).
- Network short reads and EOF cannot yield fabricated metadata; streams close on failure (Task 4).
- Disconnect restores both screens without overriding an intentionally saved single-screen layout; reconnect preserves compatible custom layouts (Task 5).

### Task 1: Saturating microphone PCM conversion

**Files:** `app/src/main/cpp/MicInputOboeCallback.cpp`; create `tools/host-tests/microphone/MicInputGainTest.cpp`, boundary stubs and `run.sh`; extend `tools/android-tests/{CMakeLists.txt,build.sh,run.sh}` and `tools/host-tests.sh`.
**Interfaces:** Existing `onAudioReady(AudioStream*, void*, int32_t)` remains unchanged; Android runner gains a whitelisted `--test` selector.
- [x] Preserve and run constant-input regression against baseline production callback. Literal cases: 0→0, ±123→±1230, ±3276→±32760, +3277/+4000/+32767→32767, -3277/-4000/-32768→-32768. Expected: six behavior failures; strict sanitizer rejects 32770→short.
- [x] Clamp rounded amplified sample before narrowing, preserving gain and resampling.
- [x] Run `tools/host-tests/microphone/run.sh` and Android NDK target `MicInputGainTest` via ADB. Expected: all eleven cases pass, exit0, no sanitizer errors.
- [x] Commit source and permanent regression together.

### Task 2: Microphone buffer cursor ownership

**Files:** `app/src/main/cpp/{MelonDSAudio.cpp,MicInputOboeCallback.h}`; create permanent source-switch regression from diagnostic harness.
**Interfaces:** Existing setupAudio/updateAudioSettings/readMic remain; device cursor belongs to its callback buffer, separate from synthetic blow cursor.
- [x] Run production lifecycle regression: read 1500 blow samples, switch to device, capture 1024 samples, read8. Expected baseline: negative memcpy size/crash.
- [x] Add session-recreation and wraparound scenarios, including silence/no data and switching back. Expectations use supplied PCM and independent literal values.
- [x] Give each device ring its own initialized read cursor; retain synchronization around ring reads.
- [x] Run microphone group on host and Android; expected no crash, returned samples match current input and do not expose stale-session data.
- [x] Commit independently.

### Task 3: NAND error and import integrity

**Files:** `app/src/main/cpp/MelonDSNandJNI.cpp`; extend `tools/host-tests/nand-jni/{NandJniTest.cpp,stubs/*,run.sh}` and Android harness; add Android instrumentation for native failure paths where possible.
**Interfaces:** Existing JNI result codes remain; malformed input returns existing failure codes before mutation.
- [x] Run baseline regressions: missing NAND returns0 instead of3; failed partial import leaves category00030004/title4b414141; mismatched TMD installs another title. Confirm fake boundary matches real core ImportTitle source.
- [x] Test short header/TMD and unaffected-title preservation where the production path accepts external buffers.
- [x] Return NAND failure correctly, pass category/id in their actual order, reject invalid or mismatched metadata before destructive calls.
- [x] Run host JNI suite, NDK boundary suite and real Android JNI invalid-file scenario. Expected errors are reported and unrelated title files survive.
- [x] Commit with source/issue limits; no claim of DSi logical quota completion.

### Task 4: Complete or fail metadata downloads

**Files:** `app/src/main/java/me/magnum/melonds/impl/NusDSiWareMetadataRepository.kt`; create JVM and Android instrumentation regression for metadata transport.
**Interfaces:** `getDSiWareTitleMetadata(UInt, UInt): ByteArray` still returns520 bytes or throws; no zero-filled partial success.
- [x] Run real repository against controlled full, 17-byte-fragmented and15-byte-truncated responses. Expected baseline: fragmented mismatch at17 and truncated response incorrectly succeeds.
- [x] Read exactly the required520 bytes with EOF detection, preserving timeout and stream cleanup.
- [x] Assert full/fragmented byte identity, EOF on truncated/empty response, no bytes beyond required prefix, streams close on both outcomes.
- [x] Run storage category JVM and Android tests. Expected all pass.
- [x] Commit independently.

### Task 5: Display disconnect fallback

**Files:** `app/src/main/java/me/magnum/melonds/impl/layout/UILayoutProvider.kt`; add `UILayoutProviderDisplayLifecycleTest.kt` in JVM and Android test sources, sharing fixtures if useful.
**Interfaces:** Existing currentLayout flow/updateDisplays remain; fallback must match display topology before reusing coordinates.
- [x] Run real public-flow regression with split custom layout then secondary disconnect. Expected baseline: only bottom screen remains on primary.
- [x] Keep exact intentionally single-screen layout and equivalent-display reconnect controls.
- [x] Restrict final fallback to compatible display configuration and use existing default behavior when none exists.
- [x] Run display/input group and Android instrumentation. Expected both default screens return on disconnect and controls pass.
- [x] Commit independently.

### Task 6: Stationary multi-touch transitions

**Files:** `app/src/main/java/me/magnum/melonds/ui/emulator/input/TouchscreenInputHandler.kt`; `TouchscreenPointerTransitionTest.kt` plus Android counterpart.
**Interfaces:** Existing IInputListener remains; POINTER_DOWN updates the resistive centroid and POINTER_UP excludes only the pointer being lifted.
- [x] Run baseline MotionEvent tests: second stationary finger at192 after first at64 must report128; lifting it must report64 without requiring MOVE. Expected baseline: no new touch event in either case.
- [x] Cover lifting pointer index0 and a three-pointer centroid with independent literal coordinates, preserving one press/release gesture.
- [x] Handle pointer transitions and exclude actionIndex only on POINTER_UP.
- [x] Run all input tests and the Android transition cases. Expected no stale position, duplicated press or premature release.
- [x] Commit independently; do not claim Rhythm Heaven latency/Thor offset resolved.

### Task 7: Robust usrcheat.dat streaming

**Files:** `app/src/main/java/me/magnum/melonds/common/cheats/{UsrCheatDatabaseParser.kt,ProgressTrackerInputStream.kt}`; shared JVM/Android parser regressions.
**Interfaces:** Existing parser/listener contracts stay; valid fragmented input decodes identically and truncated input throws rather than looping.
- [x] Preserve synthetic356-byte valid fixture with one game and code02000000 00000001; same bytes capped at7 per read fail baseline address-book parsing. Truncate to288 bytes with advertised game offset320; expected baseline timeout.
- [x] Use exact reads for address-book records, detect EOF while skipping and avoid subtracting EOF from progress. Assert content and total bytes, not parser internals.
- [x] Run full, fragmented, EOF/truncated and offset-boundary cases on JVM and Android with bounded failure detection that cannot leave a runaway thread.
- [x] Check existing XML/modifier-family/search tests remain green. Commit independently; arbitrary cheat/game crashes remain separate.

### Task 8: Atomic cheat database replacement

**Evidence:** Actual worker/parser/Room regression loses the old imported code and retains a partially imported replacement on malformed XML, on both JVM and Android. Existing custom cheat survives; imported data does not.
**Files:** `CheatImportWorker.kt`, parser event adapter, `CheatsRepository.kt`, `RoomCheatsRepository.kt`; shared SQLite/worker regressions.
**Interfaces:** Add a bounded stream of import events consumed inside one Room transaction. The parser remains synchronous and runs outside the database executor; no whole-database buffering or schema changes.
- [x] Keep the malformed XML RED and valid replacement control; add DAT, missing header, parser/write failure and cancellation cases.
- [x] Commit deletion and all imported rows atomically only after completed parsing. Roll back on parser, storage or cancellation failure; retain custom cheats. Reject unavailable input immediately.
- [x] Exercise actual SQLite and worker import path on JVM/Android; test streaming cancellation and bounded buffering rather than mocking repository success.
- [x] Commit separately, with exact scope and source traceability.

### Task 9: Integrated verification and issue disposition

**Files:** New report directory `docs/maintenance/2026-10-28-android-bugfixes/`; retain prior PR archives and update unpublished drafts only.
**Interfaces:** Reports consume actual task logs/commits, not inferred results.
- [x] Revisit all13 categories, list already covered versus our proven corrections, partial reports and missing reproduction prerequisites. Review community PRs before related implementations; keep credits/links.
- [x] Run complete JVM suite, ARM64 APK, lint, native groups and Android instrumentation; run affected NDK suites on4/16KiB environments. Use user-provided ROMs only when available and preserve originals.
- [x] Fresh independent code review; reproduce and fix important findings, rerun affected/integrated checks.
- [ ] Push authorized main commits for CI; inspect exact final-head app/core CI.
- [ ] Preserve regression evidence and source-linked future-PR notes; stop with publication blocked and explicit device/game limits.
