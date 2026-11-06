# Bug dossiers

These records document corrections retained in the current Android source.
They keep the original tested identities and limitations; moving the evidence
does not rerun old tests on the current APK. Functional impact labels describe
consequences rather than security scores. Related issue numbers are context,
not claims that upstream reports were closed.

[Security: 101 individual occurrence dossiers](../security/README.md) ·
[Current release](../RELEASE.md) · [Exact-APK smoke](../POST-RELEASE-SMOKE.md)

| Dossier | Impact |
| --- | --- |
| [Microphone gain must saturate before narrowing](microphone-saturation.md) | Moderate functional impact: distorted microphone input |
| [Microphone capture cursor belongs to its buffer](microphone-buffer-cursor.md) | High functional impact: crash or stale capture |
| [Serialize microphone source and session changes](microphone-concurrency.md) | High functional impact: unsafe concurrent lifetime |
| [Retain pending save writes when file I/O fails](save-write-retry.md) | High functional impact: lost game progress |
| [Validate BIOS and DSi title metadata before NAND mutation](nand-import-integrity.md) | High functional impact: invalid or partial title data |
| [Read the complete TMD prefix or reject it](tmd-fragmented-read.md) | Moderate functional impact: incomplete metadata adoption |
| [Publish only a complete guided BIOS/NAND copy](bios-guided-copy.md) | High functional impact: incomplete system file adoption |
| [DAT parsing must make progress on fragmented input and EOF](cheat-dat-streaming.md) | Moderate functional impact: import hang or rejected data |
| [Failed cheat imports preserve the previous database](cheat-import-transaction.md) | High functional impact: cheat database loss |
| [Cancel a cheat import blocked in provider I/O](cheat-import-cancellation.md) | Moderate functional impact: stuck work and notification |
| [Restore both DS screens after display disconnection](external-display-lifecycle.md) | Moderate functional impact: missing DS screen |
| [Update touchscreen coordinates when stationary fingers change](touch-pointer-transition.md) | Moderate functional impact: incorrect input position |
| [Check audio stream ownership on open and resume](audio-open-resume.md) | High functional impact: crash or silent output |
| [Dispatch frames using the renderer actually created](renderer-fallback.md) | High functional impact: invalid native cast or blank output |
| [Serialize library scans and preserve stable snapshots](library-scan.md) | Moderate functional impact: stuck scanning or inconsistent lists |
| [Persist the newly selected library sort order](library-sort.md) | Low functional impact: wrong ordering after restart |
| [Load library icons without blocking unrelated rows](library-icons.md) | Low functional impact: blocked or stale library icons |
| [Bound archive decoding and traversal](archive-memory.md) | High functional impact: memory exhaustion or unbounded work |
| [Keep cheat selector actions visible with large text and keyboard](selector-layout.md) | Moderate functional impact: inaccessible controls |
| [Use labels that match the cheat option type](selector-labels.md) | Low functional impact: misleading selector instructions |
| [Stage replacement DSi saves before removing the prior generation](nand-save-replacement.md) | High functional impact: previous DSi save loss |
| [Release every successful JNI UTF acquisition](jni-utf-lease.md) | Moderate functional impact: native resource leak |
| [Keep JIT teardown and invalidation ownership valid](core-jit-lifetime.md) | High functional impact: native lifetime error or stale guest code |
| [Anchor LCD and scanline filters to the native pixel grid](lcd-scanline-grid.md) | Low: visual moiré and incorrect filter spacing |
| [Limit coroutine effects to the validated emulator session](session-coroutine-scope.md) | Moderate: stale or premature session work |
| [Clear obsolete constraints when moving the FPS counter](fps-counter-position.md) | Low: counter positioned in the wrong location |
| [Preserve the native lid-pause deadline across Activity stop](lid-stop-deadline.md) | Moderate: missed or repeated emulated pause |
| [Bound JNI local references while listing DSi titles](jni-title-references.md) | Moderate: native reference exhaustion |
| [Selective library-entry removal](library-entry-removal.md) | Moderate usability impact; ROM/save files retained |
| [RTC resume and stopped-session guard](rtc-resume.md) | Wrong guest clock or resume dispatch |
| [Thumb/DTCM literal ownership](core-thumb-literal.md) | Stale guest code/data |
| [DS Wi-Fi frame lengths](core-ds-wifi-length.md) | Native buffer write |
| [DSi WMI payload lengths](core-dsi-wmi-length.md) | Native buffer write |
| [Action Replay payload bounds](core-action-replay-length.md) | Native overread |
| [NAND cartridge page reads](core-nand-cart-read.md) | Native overread |
| [NAND metadata-fault recovery](core-nand-rename-fault.md) | Wrong committed save generation |

The original eight A1/A2/N1/N2/D1/I1/C1/C2 rows were groups of demonstrated
code defects, not eight closed GitHub issues. Later microphone concurrency,
blocked-I/O cancellation, BIOS staging, renderer, cache and UI controls remain
separate contracts above. Repeated sanitizer/Android executions are evidence
runs, not new independent bugs. Security duplicates retain their occurrence
IDs and canonical normalization rather than inflating the cause count.
