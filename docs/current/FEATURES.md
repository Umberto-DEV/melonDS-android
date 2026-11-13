# Features and contributor provenance

Current UD Nightly 2.1.1 integrates official Android source `27321156`.
The original fork comparison used app `c42995ca` and core `431ab4bd`.
The [upstream reconciliation](UPSTREAM.md) records later source integration;
[release identity](RELEASE.md) and [runtime smoke](POST-RELEASE-SMOKE.md)
identify the downloadable binary separately.

| Feature | Current behavior | Attribution, evidence and limits |
| --- | --- | --- |
| Guided BIOS/firmware setup | Folder recognition, separate DS/DSi results, validation before active preferences, named errors and retry | UD implementation; [copy/lifecycle dossier](bugs/bios-guided-copy.md). Dumps are not authenticated. |
| Firmware Wi-Fi editor | Three connection slots, DNS addresses and pause-menu enable/disable | UD implementation; WfcAccessPointSlot/WfcSettingsStorage/checksum controls. Configuration is not server/trade/GTS qualification. The editor does not invoke the optional core hostname-override API. |
| Settings/layout backup | Expose the existing upstream manager, validate restores and refresh controller cache | Upstream foundation retained; responds to [#264](https://github.com/rafaelvcaetano/melonDS-android/issues/264). SettingsBackupManagerTest covers preservation. This does not back up all external game/system files. |
| Cheat search | Toolbar and folder search over names, folders and recognized options; explicit plain-cheat actions and navigation to selector options | Integrates the merged official version of [takanuva15 #1677](https://github.com/rafaelvcaetano/melonDS-android/pull/1677). Search/index/navigation tests retained. |
| Grouped cheat selectors | Recognize compatible species/level/nature/starter families and exclusive choices | UD implementation; parser/fixture/ViewModel/persistence tests. The supported Sacred Gold Plus profile retains its L+R encounter toggle; exact ROM/code compatibility remains required. [Layout](bugs/selector-layout.md) and [labels](bugs/selector-labels.md) are separate usability corrections. |
| Selective library removal | Forget an exact URI and its settings/play history while preserving files and other entries | UD implementation; [full dossier](bugs/library-entry-removal.md). A later scan can rediscover a file still present. |
| Combination hotkeys / held fast-forward | Independent hold and toggle behavior, combined bindings, interrupted-input cleanup | Adapts [WailAbou #1606](https://github.com/rafaelvcaetano/melonDS-android/pull/1606) and [#1608](https://github.com/rafaelvcaetano/melonDS-android/pull/1608). Physical-controller acceptance is separate. |
| Optional fast-forward mute | User-selected mute policy while fast-forwarding | Adapts [dysprosium #1625](https://github.com/rafaelvcaetano/melonDS-android/pull/1625). It does not implement dynamic resampling or pitch correction. |
| External launches | Resolve intended ROMs with explicit URI/launch ownership and granted content authority | Adapts [N3MI-DG #1648](https://github.com/rafaelvcaetano/melonDS-android/pull/1648), with later security restrictions. Raw filesystem access may still be blocked by scoped storage. |
| Sleep/lid handling | Preserve the emulated close/open sequence and pause deadline during sleep/wake | Adapts [DLCSharp #1589](https://github.com/rafaelvcaetano/melonDS-android/pull/1589); LidClosePauseControllerTest and stop/teardown controls. |
| Session lifecycle | Explicit coroutine/resource ownership and selected recovery fixes | Selected parts of [corigne #1666](https://github.com/rafaelvcaetano/melonDS-android/pull/1666). Complete Android process-death recovery is not claimed. |
| Optional RTC catch-up | Default off, forward only, 30-second threshold, existing setting key retained | Upstream resume synchronization by [Benjamin BONVARLET/Komalis #1664](https://github.com/rafaelvcaetano/melonDS-android/pull/1664), merged by Rafael Caetano; UD policy preserved. [Resolution and tests](bugs/rtc-resume.md). |
| Archive handling | Heap-aware decoder limits, bounded traversal/cache, explicit extraction-failure notices | Current source integrates official archive work including [#1524](https://github.com/rafaelvcaetano/melonDS-android/pull/1524) while retaining tighter UD limits. [Archive dossier](bugs/archive-memory.md). |
| Android update installer | PackageInstaller session flow, failure status and localized download label | Official source through `feab12e5`; [integration evidence](UPSTREAM.md). Android package/signer/version checks still apply; inherited update discovery follows the original project. |

DS/DSi emulation, OpenGL/Compute rendering, resolution scaling, DAT import and
the underlying backup manager retain their upstream authorship. Existing ABXY
multi-pointer handling, same-ROM intents, rewind position, multiple ROM folders
and Android game category are upstream behavior retained by this fork.

Compared but not adopted wholesale: [#1657](https://github.com/rafaelvcaetano/melonDS-android/pull/1657)
(idle rendering), [#1676](https://github.com/rafaelvcaetano/melonDS-android/pull/1676)
(dynamic audio rate/pitch), and Android-core [#17](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/17)
(larger audio buffer). No energy, fidelity or latency improvement follows from
those comparisons. The [issue coverage register](ISSUE-COVERAGE.csv) preserves
selected historical checks and partial/unimplemented scope without claiming
fresh tracker states.
