# UD 2.1.11: post-release Android smoke — 6 November 2026

This records new execution after the release-time build report. It does not
change the published APK, source archive, tag or checksums, and does not
transfer evidence from an older candidate.

## Exact identities

- Released app source: `ac42561495ee6351d7f119f68760509c268f582e`.
- Embedded Android core: `06f3df985f67416368e0716174b7f06934a10229`.
- Public APK SHA-256: `d3cf208e4380c4e3060ca9f33a10d8d6d1c4621c3f36c1a037bbfff11a01a928`, 20,786,147 bytes.
- Android package: `me.magnum.melonds.nightly`, version code 52.
- Preserved UD certificate SHA-256: `832a0223e29802245c0e9f8ca24c8358e7d21cfe0dbea347845fda751f950d86`.
- Update baseline: public UD 2.1.10/51, APK SHA-256 `3b00f81647875408343f1e5895ccb36f39bf3bd77748de6f1f40c376c47bd10b`.
- Runtime: Android 15/API 35, ARM64, measured 16,384-byte pages, Android Emulator.
- Game copy: English Sacred Gold Plus 1.3.1, ROM SHA-256 `185dc5c53a75652c021ced65157e1c3388cd661ebe90775e5b49314aee753b08`.

The APK was freshly downloaded from the public release URL and hashed before
installation. Tests used an existing diagnostic Android installation and a
separate copy of an archived raw game save. No personal device installation,
original ROM or original raw save was modified.

## Results

| Check | Observed result |
| --- | --- |
| Install the standalone APK over public UD 51 | Android accepted the same-package, same-certificate update to 52. UID and first-install identity remained unchanged. |
| Compare before first launch | All three existing library records, logical contents of 15 database tables, protected preferences, runtime permissions and persisted URI grants were preserved. |
| Compare after normal first launch | The same protections passed. The typed `last_version` preference advanced 51→52; regenerable cache/profile changes were listed separately. No updater-timestamp exemption was needed. |
| Normal game Continue | The exact current ROM opened its existing copied adventure through the native Continue screen. No savestate was loaded. |
| Guide and Options | Category/list/detail navigation and Options/Shiny pages rendered. A 100 ms virtual Down press moved the Guide selection from 1/4 to 2/4. This is a bounded input observation, not a native-menu timing benchmark. |
| Native game SAVE | The selected native save generation advanced once, using the other valid bank. Twenty-five semantic checks passed, including complete party, native Bag, story flags/variables, field data, Pokédex, Day Care, Pal Pad, game-stat and 540-box-slot arrays, Plus settings and expanded Bag payload. |
| Expected state changes | Native RTC/new-console metadata, berry-pot timestamp, roamer location and playtime/journal fields changed. Native playtime and the training journal advanced by the same 596 seconds. Changed byte offsets remain in the private test record; this semantic smoke does not certify every changed byte. |
| Cold restart | After force-stop, a new app PID opened native Continue and returned to the same field. The test raw-save hash stayed unchanged across cold Continue. |
| Presentation captures | Five unmodified native DS framebuffer PNGs were exported from laboratory-only slots, for Guide categories, evolution list/detail and Options/Shiny. |

## Limits and installation

This verifies an update from the earlier public UD package, not a fresh Android
user installation, an official-author APK replacement, the legacy `.perf`
package or a physical Thor update. It is one bounded game/save smoke, not a
complete game playthrough, multiplayer/DSi qualification, performance benchmark,
power-loss test or universal compatibility claim. The copies used no active
Day Care course or Pro shiny hunt; those populated workflows are not inferred.

Use the [current release APK](https://github.com/Umberto-DEV/melonDS-android/releases/tag/ud-nightly-2.1.11-r2).
The source patches are for rebuilding source code. They cannot convert the
original author's differently signed APK into an in-place UD update. In-app
update discovery still follows the original project; obtain UD updates from
this fork's release page. Historical Android packages were withdrawn at the
maintainer's request; their source tags and original evidence identities remain.
