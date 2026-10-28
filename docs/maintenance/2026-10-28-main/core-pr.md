This fixes DS/DSi register handling, Wi-Fi power saving and JIT literal invalidation, and avoids redundant renderer work.

- **DS/DSi compatibility:** apply the appropriate SCFG register initialization and access restrictions when running DS software in DSi mode.
- **Wi-Fi:** automatically power down the transceiver only when automatic power-saving mode is selected. Disabled power saving must not leave the radio off after the beacon countdown.
- **JIT:** fold literals only when their memory is tracked for invalidation, and use the actual write mask when invalidating a block. Both ARM64 and x86_64 backends check whether a literal was registered.
- **Rendering:** skip unchanged compute-renderer configuration and stop generating/uploading edge data or compiling shaders for the inactive edge pass.
- **Optional networking API:** let a frontend provide hostname-to-IPv4 answers to the built-in DNS responder. The table is empty by default; ordinary resolver behavior is preserved.

### Integration

Companion Android PR: https://github.com/rafaelvcaetano/melonDS-android/pull/1682. The Android proposal pins this core revision. Merge the core first, then point the app's submodule at the accepted upstream commit if merging changes its SHA. The WFC settings screen edits firmware DNS settings and does not depend on the optional hostname API.

### Validation and limits

- The source tree is byte-for-byte identical to the previously reviewed core proposal; only contribution metadata was cleaned up.
- [Hosted Android integration build](https://github.com/Umberto-DEV/melonDS-android/actions/runs/40112578134) passed against this exact core revision: ARM64 APK compiled, 180 Android JVM tests passed, and the test APK/report artifacts were uploaded.
- Earlier ARM64 host validation covered sanitizers, synthetic JIT invalidation and DNS packet handling. Test harnesses are included in the companion Android proposal under `tools/host-tests/`; those host checks were not rerun for this metadata-only core resubmission.
- A prior paired Android build received five weeks of user testing on an AYN Thor. This is separate from testing newly added Android changes.
- The JIT/interpreter framebuffer comparison has a known discrepancy reproduced identically with the upstream and proposed cores. x86_64 runtime validation is still outstanding.

Supersedes #18, whose original source fork was removed. No emulation changes have been added during this resubmission.
