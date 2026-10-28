# Android issue remediation

User mandate: continue the existing Android audit, fix reproducible defects individually, test related categories and the integrated app, retain sources and contributor credit, then stop before PR publication. The user explicitly delegated implementation and asked not to pause for intermediate approvals. Work directly on main in app/core. Daybreak and new PRs remain gated.

Baseline: app 05c38d61, declared version 2.1.4 (Nightly flavor); core fd758240, derived from 1.1. The 246-issue inventory is a triage record, not a claim that every report is reproducible or fixed.

## Approach

Use root-cause investigation and production-code RED→GREEN regressions, then category tests, Android execution and whole-app checks. Host diagnostics isolate logic; NDK compilation and Android execution are reported separately. An Android emulator runs on the Mac; the Mac frontend is outside scope. Existing native Android harness and AndroidX instrumentation are reused. Physical Thor, game-specific files, Bluetooth and real display routes are explicit additional evidence, never inferred from host passes.

Alternatives rejected: bulk cherry-picking community PRs would obscure cause and regressions; broad refactoring before reproduction would conflate fixes and design changes; waiting for hardware would block deterministic defects that are already testable. These are implementation choices under the user's continuous-execution mandate.

## Scope and acceptance

Fix all bounded defects confirmed during this pass. For each retain the source issue/PR, trigger, actual failure on baseline, minimal production change, green result, related-category tests and limits. A source report may stay partial even after a proven sub-defect is fixed. Features, broad optimization, resolution changes and hardware-specific reports without reproducible fixtures remain documented follow-up work. No claims of lower latency/FPS improvement without comparable measurements.

Never modify original ROMs, saves, NAND or firmware. Use synthetic inputs or disposable copies of user-authorized files. Do not download commercial ROMs/BIOS. Preserve original untracked audit files and previous PR archives.

## Current isolated defects

1. Microphone gain narrows values outside signed 16-bit range, reversing polarity; reproduced in the real callback on Android ARM64.
2. Blow and device microphone share a cursor despite different buffer sizes, causing negative memcpy length after switching sources; production lifecycle regression exists.
3. NAND open failure returns success; failed import cleanup reverses category/title arguments; header/TMD mismatch can write another title. Model the documented core boundary and separately verify Android JNI errors.
4. TMD download assumes InputStream.read fills 520 bytes; fragmented/truncated responses become zero-padded metadata.
5. Display-layout fallback can reuse a dual-display custom layout after its secondary display disappears; public flow reproduction must fail before changing it.

Further candidates enter the implementation plan only after concrete reproduction. Native tests must compile production code, not copied algorithms; network/filesystem doubles substitute external failures, not the behavior being asserted.

## Evidence sources

- [Android testing fundamentals](https://developer.android.com/training/testing/fundamentals): separate local, component, integrated and Android instrumented tests.
- [Android Emulator](https://developer.android.com/studio/run/emulator): repeatable virtual Android environments, complemented by physical devices.
- [Android benchmarking](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview): measure end-user interactions before claiming performance improvements.
- Existing source catalog and exact PR bodies: `docs/maintenance/2026-10-28-main/`.
