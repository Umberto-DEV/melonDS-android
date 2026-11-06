# Second independent security campaign

Evidence collected on 3 November 2026. Scan
`ab26b107-97dd-4557-9c4b-2942e5df879d` completed once, from
`2026-11-03T13:56:06.766246Z` to `2026-11-03T17:44:41.144905Z`, against app
`9929f530df6f60b86d50c324f4aec83747e5745b` and core
`65952aec868bedd2dc33151ed850804e3e13df05`.
Snapshot SHA-256: `9ff048237b8d1dae0db1f928f1ad74e89e50d1c13f23bd135df8aa77722c4c22`.
Coverage artifact SHA-256: `5d9167a65165b3e1a12a9f3f983decf2287042a3f2e932e536191ca2a9748a3c`.
The coordinator inventory covered 3,633 nonignored files, including 750 core
files. This inventory status is not a proof that every line or runtime path
was reviewed or free of defects.

## Cause counts and applicability

The 66 new occurrences normalize to 48 groups. Of these, 62 occurrences / 45
groups were corrected in repository code; two occurrences / one group retain
unverified external deployment controls; one occurrence / group is not
applicable to Android; one is informational reconciliation. The 46
correctable groups comprise 16 incomplete-fix/adjacent-boundary groups (31
occurrences) and 30 previously unrecorded independent causes (33 occurrences).
The prior 35 IDs are not counted again. These are neither 48 Android exploits
nor a claim that each trigger ran dynamically.

Important adjacent omissions included the R4 override of B7 bounds, inner
RCHV chunks despite a sized outer API, archive metadata offsets, backup load
mode during savestate rollback, native state invariants, restored firmware
text and the guided NAND copy budget. The latter two were fork-added paths.
Detailed source, correction and proof levels are in the [101 dossiers](README.md)
and [canonical CSV](FINDINGS.csv).

For the then-current Nightly build, 59 occurrences / 43 groups mapped to
potential runtime code after excluding informational, Android-inapplicable,
build/release and production-only updater paths. This is source/build
applicability rather than execution of every trigger in an APK.

## Corrected boundaries

| Boundary | Before | Correction and scope |
| --- | --- | --- |
| Core devices/cartridges | BPTWL, SD, NAND/R4, padding and NWiFi windows could exceed objects or expose uninitialized bytes | Exact spans, capacities, initialization and progress checked at consumers |
| Native state | Scheduler/FIFO/GPU/SPU/camera/AES/RTC/DMA fields admitted invalid combinations | Local and cross-field invariants; rollback restores the prior state on semantic failure |
| Guest work | Action Replay, G.711, NWiFi BMI and rumble could impose unbounded work/queues | Per-frame/command budgets, quarantine and backpressure |
| NAND/DSiWare | Short reads, geometry errors, unbounded copies and beyond-capacity writes | Exact I/O, bounded sizes, sector geometry and controlled publication |
| ROM/SAF | Provider names, legacy cache, archives and exported launch paths could collide or substitute files | Digest identities, isolated storage names, actual-copy validation and compressed/expanded budgets |
| Cheat/camera/RA | Aggregate text/count/pixel/response/JNI and memory-region bounds were incomplete | Document budgets, region limits and deterministic resource release |
| Lifecycle | NAND enumeration/native threads could outlive their owner; disabled production updates could still contact GitHub | Serialized ownership/leases and a no-op disabled repository |
| Build/release | Nested Actions/Gradle artifacts escaped checks; tag paths reached production | Immutable pins, dependency verification and source/actor/environment gates; external enforcement remains unverified |

The historical GitHub API readback found no fork environments or rulesets,
only upstream environment `copilot`, and HTTP 404 from legacy tag protection.
It did not establish effective production reviewer/tag protection. The two
release-control occurrences therefore remain externally incomplete. Android
omits R4 SD storage, so its FAT divide-by-zero prerequisite was unreachable;
this does not assess other frontends.

## Verification on the recorded final pair

Final app `0018cddb04278f2bca5054804de501a459eb1de9`, core
`ac5e6ba0fb795b7aa8a43fdefed62eed8a37eff8`:

| Level | Recorded result | Limit |
| --- | --- | --- |
| Host | Security, sanitizer-core and real Action Replay suites passed | Host diagnostics, synthetic inputs |
| JVM/API/build/lint | 346 tests across 55 XML suites; zero failures/errors/skips; ARM64 build/lint completed | Historical pair; compiler/manifest warnings retained |
| Android NDK | 11 native executables passed on API35 ARM64 4 KiB and 16 KiB | Component execution, separate from ART |
| ART/providers | 52 default passes + three opt-in skips; separate real WorkManager case passed | Diagnostic `.dev` APK; UI capture/GLES opt-ins not repeated in this final cycle |
| Exact optimized APK | Direct and ZIP homebrew launches; copied SGP 1.2.5 IT Continue → SAVE → new-process cold Continue and normal Exit passed | One ROM/save, one 16 KiB AVD; not Thor or DSi qualification |
| CI | [Core 41704485288](https://github.com/Umberto-DEV/melonDS-android-lib/actions/runs/41704485288) and [app 41704565464](https://github.com/Umberto-DEV/melonDS-android/actions/runs/41704565464) passed on the named final pair | Linux x86-64/ARM64 host checks are separate from Android runtime |

The historical optimized APK was version 2.1.8/49, SHA-256
`195b5748fc59bac74eadee148f0a1a7b440e47b51dcc5f70fdcd384e855932cb`,
85,676,746 bytes, package `me.magnum.melonds.nightly.perf`, signer
`41ad82cd4365c73195128975cf0b920699f8833093507957dc2d0f9d6bb0b4b1`.
It is an evidence identity, not a current download or a public `.nightly`
package update. Before final reinstall, an earlier 48→49 upgrade on the 16 KiB
AVD preserved synthetic database/layout/marker bytes, preferences except
version/update metadata, UID/firstInstallTime and persisted SAF grants. After
the last exact-APK reinstall only UID/firstInstallTime/runtime were rechecked;
private byte comparison was unavailable. The two checks are not equivalent.

The copied SGP 1.2.5 IT ROM SHA-256 was
`bdf50a4f327f8ea0d8a2ef312e1491d78460d34e978d9b15d160eaf140b313d4`;
original raw save SHA-256 was
`975bc4d18c5991a688c2dcefb97a959fc2a400027ea216d5a77bdaded2bf6f1a`.
After native SAVE the copied 524,288-byte raw hash was
`40771aa15de9fde49eb671887e3954a793b956757a56cec7cc348e479fe42f86`.
Cold Continue retained that hash. Original inputs were rehashed unchanged;
no savestate was substituted for the raw-save cycle.

## Post-push controls and counterevidence

The initial Linux x86-64 JIT check really failed: Thumb/DTCM retained an old
literal. Core `ac5e6ba0` rejects folding addresses without an invalidation
owner; the named final CIs passed. This functional gap is outside the 101
scan occurrences and is separate from Android ARM64 execution.

G.711 nominal NWRAM bounds were clipped to the actual ARM9 window
`[0x03000000, 0x04000000)`: helper RED exit 1, then 29 ASan/UBSan controls
passed. Production G.711 compilation passed; end-to-end Android G.711 was
not executed. Action Replay frame fairness failed with exit 10 before the
correction: a cheat consuming exactly 1,048,576 operations starved its
successor. The scheduler now resumes the deferred cheat next frame, preserves
`Enabled`, and does not renew the budget for repeated same-frame interrupts;
14 sanitizer modes passed. These refine existing occurrences.

A proposed future-DSP-timestamp bug was confuted: production already requires
`DSPTimestamp <= ARM9Timestamp` and bounds backlog by `2^24`; all 13
NativeRuntimeBounds controls passed. No correction or new bug was counted.
The first build's missing aapt2 verification checksum and a BIOS fixture
blocking the wrong source-open number were harness/configuration corrections,
separate from product REDs.

Remaining limits include ambiguous version-zero legacy cache identity for
same-name entries, partial SAF exports after provider failure, rejection of
some anomalous historical states, mutable providers after validation,
external GitHub enforcement, DSi HOME/logical quota, physical drivers,
power loss and broad game compatibility. A passing suite is not exhaustive
security acceptance.
