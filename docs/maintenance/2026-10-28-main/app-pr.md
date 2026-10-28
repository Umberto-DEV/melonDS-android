This adds Wi-Fi connection settings, searchable cheat selectors and reliability improvements for saves, audio and controls. It also fixes the GitHub Actions setup failure that prevented the previous submission from reaching compilation.

### User-facing changes

- **Wi-Fi connections:** configure three WFC slots and primary/secondary DNS addresses in Settings, and enable or disable slots from the pause menu. DSi edits persist both basic and extended connection blocks.
- **Setup and preferences:** guided BIOS/firmware setup, settings/layout backup and restore, and optional forward-only clock synchronization.
- **Controls and cheats:** hold-to-fast-forward, combination hotkeys, optional fast-forward muting and grouped cheat selectors. Interrupted touches and lost controller focus release held inputs.
- **Cheat search:** search folders, individual cheats and generated selector options. Matching text is highlighted; selecting a result opens the matching row or selector without enabling it. Results use lazy scrolling, and the query survives navigation and loading/restoration transitions.
- **Reliability:** improve save-write failure handling, audio restoration after resume, ROM icon loading and bounded 7z decoding memory. Also fixes LCD/scanline alignment, XML cheat import and temporary JNI references while listing DSiWare titles.

### Actions correction

- Explicitly request `platform-tools` instead of the unavailable SDK package `tools`.
- Update checkout, Java setup, Android setup and artifact upload to supported Node 24 versions across the Android workflows.
- Run unit tests and an ARM64 debug APK build for forks and pull requests with read-only repository permissions and no release-signing secrets.
- Restrict signed Nightly publication to upstream `master`; upload verification APKs from their actual build output path.
- Remove the nonexistent `setup-ndk` output reference and place the manual Play Store actor restriction on the job, where GitHub evaluates it.

### Credits and issue coverage

The search UI is adapted from [#1677](https://github.com/rafaelvcaetano/melonDS-android/pull/1677) by [takanuva15](https://github.com/takanuva15), whose coauthor credit is preserved. This extends that work to grouped/generated options and precise navigation. Also includes adaptations of #1606/#1608 (WailAbou), #1625 (dysprosium), #1648 (N3MI-DG), #1589 (DLCSharp), and selected side fixes from #1666 (corigne).

- Fixes #1678: temporary JNI references accumulating while listing DSiWare titles.
- Implements the settings/layout backup and restore requested in #264.
- Partially addresses audio resume (#1644/#1635), clock drift (#1652/#1604), and 7z scanning crashes (#1519/#1378). Optional fast-forward muting is a workaround for #1602; remaining device-specific symptoms and audible distortion are not claimed fixed.

### Core dependency

Companion core PR: https://github.com/rafaelvcaetano/melonDS-android-lib/pull/19. This proposal pins core commit `f950b5e69c1c72dd309ec7af6ad9261d0076e5fc`, whose source tree is unchanged from the previously tested core. Review the pair together; merge the core first and update the app's gitlink to the accepted revision if its SHA changes. WFC firmware editing does not require the optional hostname-override API.

### Validation and limits

- **Hosted Actions passed:** [Linux verification run](https://github.com/Umberto-DEV/melonDS-android/actions/runs/40112578134) completed **180 JVM tests, 0 failures, 0 errors, 0 skipped**, including the search regressions, and built the ARM64 debug APK against the pinned core. JUnit results and the APK were uploaded successfully. The SDK failure and deprecated-action warnings are gone.
- Additional checks: workflow YAML parsing, `git diff --check`, source-tree equality, dependency alignment and contributor attribution. Earlier native JNI/DNS test results remain applicable to the unchanged sources; those host tests were not rerun for this workflow update.
- The [separate upstream PR run](https://github.com/rafaelvcaetano/melonDS-android/actions/runs/40112753066) is awaiting a maintainer's approval to start. The fork run above validates this exact proposed commit.
- A prior Android build received five weeks of user testing on an AYN Thor. The additional fixes and search have automated coverage but have not received a new Thor acceptance test. No emulator or APK was installed during this resubmission.
- Android Lint is **not clean**: 278 errors in the recorded run versus 207 upstream, with the 71 additional errors all missing translations. The known JIT/interpreter framebuffer discrepancy also reproduces upstream. These limitations are not hidden or described as passing checks.

### Screenshots

Wi-Fi settings captured on an AYN Thor from the earlier tested build:

![Wi-Fi connection settings with primary and secondary DNS](https://raw.githubusercontent.com/Umberto-DEV/melonDS-android/aedb719f56d50b5457c08de1172e166b23f5861e/docs/images/wfc-connection-settings.png)

![Wi-Fi connection settings overview](https://raw.githubusercontent.com/Umberto-DEV/melonDS-android/aedb719f56d50b5457c08de1172e166b23f5861e/docs/images/wfc-overview.png)

Supersedes #1681. The functional changes and community contributions are preserved; resubmission changes concern Actions, contribution metadata, screenshot filenames and the equivalent core revision.

