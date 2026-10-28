# Android Native JIT Test Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for this already authorized implementation. Do not delegate, commit, or change production sources.

**Goal:** Build the existing JIT regression test for Android and execute it through ADB with strict UBSan.

**Architecture:** A standalone CMake project includes the unchanged core and test sources. An Android dynamic-library adapter supplies the platform functions missing from the existing host test adapter. Separate build and run scripts keep device access optional.

**Tech Stack:** Android NDK 28.0.13004108, CMake 3.22.1, C++17, Bash, ADB.

**Spec:** The parent task requests Android ARM64 first, optional x86_64, static libc++, strict UBSan, no APK installation, no production edits, and preservation of the failing DTCM regression.

## Global Constraints

- Create files only under `tools/android-tests/`; build into `app/build/android-native-tests/<abi>`.
- Reuse `JitInvalidationTest.cpp`, `HostPlatform.cpp`, and `FileModeString.cpp` unchanged.
- Disable Qt, GDB stub, OpenGL and ASan. Do not install or reset devices.

## Review Focus

- Reject accidental host builds or unsupported ABIs.
- Preserve the remote test exit status even when cleanup runs.
- Reject incompatible Android SDK or ABI before uploading anything.
- Ensure libc++ and UBSan do not require extra runtime files on the device.
- Keep cleanup limited to the directory this invocation created.

## Implementation and verification

- [x] Add CMake configuration and Android dynamic-library adapter; cross-build ARM64.
- [x] Add build/run scripts with explicit device selection, metadata checks and cleanup.
- [x] Check shell syntax, ELF architecture/dependencies and UBSan build flags.
- [x] Document commands, target scope and validation limits.

Verified baseline on `emulator-5554`, Android API 35, ARM64, 4096-byte pages: the first two cases pass, the existing DTCM case fails, and the runner preserves exit code 1. Cleanup left no test directories. Baseline output is recorded in `build/main-consolidation/android-jit-baseline.log`.

The parent task subsequently expanded the shared regression and corrected the production mapping. The strengthened suite passes on Android ARM64 with both 4096-byte and 16384-byte pages (`android-jit-final-4k.log` and `android-jit-final-16k.log`). Android x86_64 runtime remains separate and has not been run here.

The existing native regression test is the behavioral test. Its expected DTCM failure must remain visible; do not alter its assertions to make the harness green.
