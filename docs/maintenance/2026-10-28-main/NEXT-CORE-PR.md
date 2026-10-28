# Unpublished core PR draft

**Publication blocked by the user pending coordinated debugging review.**

Source: `Umberto-DEV:main` → target: `rafaelvcaetano/melonDS-android-lib:master`.

Title: Fix DS/DSi compatibility, Wi-Fi power saving and JIT invalidation

This fixes DS/DSi register handling, Wi-Fi power saving and JIT literal invalidation, and avoids redundant renderer work.

- **DS/DSi compatibility:** apply the appropriate SCFG register initialization and access restrictions when running DS software in DSi mode.
- **Wi-Fi:** automatically power down the transceiver only when automatic power-saving mode is selected. Disabled power saving must not leave the radio off after the beacon countdown.
- **JIT:** fold literals only when their memory is tracked for invalidation, and use the actual write mask when invalidating a block. Both ARM64 and x86_64 backends check whether a literal was registered.
- **JIT teardown:** release fast-memory mappings while ARM9 is still alive. Strict Linux UBSan exposed a read of ARM9's DTCM configuration after its destruction; backing memory remains owned by the JIT until all other components have been destroyed.
- **Rendering:** skip unchanged compute-renderer configuration and stop generating/uploading edge data or compiling shaders for the inactive edge pass.
- **Optional networking API:** let a frontend provide hostname-to-IPv4 answers to the built-in DNS responder. The table is empty by default; ordinary resolver behavior is preserved.

### Integration

The replacement Android proposal will originate from `Umberto-DEV/melonDS-android:main`; insert its URL only after publication is authorized. Merge the core first, then update the app gitlink and submodule URL to the accepted upstream revision. The WFC settings screen edits firmware DNS settings and does not depend on the optional hostname API.

Earlier paired proposals, closed during branch consolidation: Android #1682 (https://github.com/rafaelvcaetano/melonDS-android/pull/1682) and core #19. Their descriptions and source histories have been preserved.

### Validation and limits

- Consolidation retains both local and previously published histories from the earlier `f950b5e6` proposal. The additional runtime correction is the fast-memory teardown fix described above. A new CI workflow builds the core with ASan/UBSan on Linux x86_64 and ARM64; the companion app runs native regressions on both architectures.
- Companion app validation: 190 JVM tests, ARM64 debug APK build and zero lint errors (474 warnings). Host sanitizer checks cover synthetic JIT invalidation; companion DNS and JNI tests cover packet behavior and reference lifetimes.
- **Insert final reviewed commit and successful paired CI links after the coordinated audit.** The original 180-test run remains historical evidence, not verification of the updated app.
- The optional real-ROM JIT/interpreter framebuffer comparison has a discrepancy reproduced identically upstream; it remains unresolved. Core host leak detection is disabled. ARM64/x86_64 synthetic checks do not replace game-specific or device validation.
- A prior paired Android build received five weeks of user testing on an AYN Thor. New Android corrections still require Thor acceptance testing.

Supersedes #19 and the earlier #18. New PR publication remains blocked until the coordinated review is complete and the user authorizes it.
