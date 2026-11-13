# Current release: UD Nightly 2.1.1/54

[Download the APK or complete ZIP](https://github.com/Umberto-DEV/melonDS-android/releases/tag/ud-nightly-2.1.1).
This is an independent fork of the official Android Nightly. Its visible release
name is 2.1.1; Android versionCode 54 preserves update ordering after code 53.

- [Android APK](https://github.com/Umberto-DEV/melonDS-android/releases/download/ud-nightly-2.1.1/app-gitHub-nightly-release.apk) for installation.
- [Complete ZIP](https://github.com/Umberto-DEV/melonDS-android/releases/download/ud-nightly-2.1.1/melonDS-UD-nightly-2.1.1.zip) with the same APK, complete recursive source, patches, license, build instructions and verification manifest.
- [Download checksums](https://github.com/Umberto-DEV/melonDS-android/releases/download/ud-nightly-2.1.1/DOWNLOADS-SHA256SUMS) for the APK and complete ZIP.

2.1.1 changes only cheat import: the per-document budgets introduced by the
3 November input hardening (250,000 cheats, 50,000 folders, 64 Mi text
characters, 128 MiB) rejected the complete DeadSkullzJr NDS(i) database
(709,658 cheats, 30,341 folders, about 95 M characters in 130 MB). The import
failed part-way and its transaction was rolled back, leaving the previous
database in place. XML and usrcheat imports now accept up to 2,000,000 cheats,
200,000 folders, 256 Mi characters and 256 MiB per document; per-game and
per-field limits are unchanged and parsing still streams one game at a time.
A regression test streams a synthetic document larger than the old budgets.

| Field | UD Nightly 2.1.1 |
| --- | --- |
| Tag | `ud-nightly-2.1.1` |
| Released app source | `7996706bb7355296435dba6e6a65abbd5df790c8` |
| Integrated official app | `273211564bea8f9da0c9fe71b4006a06f5d4a2b5` |
| Pinned core | `83d443c546e1c338eb69003ba27347ad5c5599f5` (unchanged from 2.1) |
| Package / code | `me.magnum.melonds.nightly` / 54 |
| Display version | `2.1.1-UD GH (NIGHTLY)` |
| APK SHA-256 | `b81a7ef6b10e0cd7da5c08874372e8f465c0eeba59124ae1ac9ff380a38bafd8` |
| Complete ZIP SHA-256 | `793f57981c50269c83d45c664a0c05f6bea6be9bce521ff4422d15337070825b` |
| Archived 2.1 assets SHA-256 | `c8cc1530a9decb6088dc9da77d41b98bf0fb2175cbb6c4ba97d145072eb4cd1a` (`archive-ud-nightly-2.1.zip`) |
| Certificate SHA-256 | `832a0223e29802245c0e9f8ca24c8358e7d21cfe0dbea347845fda751f950d86` |
| Native frontends | ARM64, ARM32 and x86-64 |
| Exact sources / payload checksums | `BUILD-MANIFEST.json` and `SHA256SUMS` inside the complete ZIP |

The complete ZIP layout, source patches and packaging checks are the same as
for 2.1 below. The app patch begins at official `27321156`; the core patch
begins at `431ab4bd`. Existing imported cheats, custom cheats, settings and
saves are not migrated or rewritten by this update. Re-importing a database
still replaces previously imported cheats and loads them disabled; custom
cheats are kept.

The released source passed [app CI](https://github.com/Umberto-DEV/melonDS-android/actions/runs/43427022552).
Local checks passed 383 JVM/API tests, seven packaging tests, three-ABI release
assembly and lint with zero errors / 483 warnings. The public downloads were
re-fetched anonymously and matched `DOWNLOADS-SHA256SUMS`. No Android runtime
acceptance of this exact public APK is claimed; the maintainer's physical Thor
runs an equivalent debug-signed ARM64 build of the same source.

---

# Historical release: UD Nightly 2.1/53

Superseded by 2.1.1. The original 2.1 APK, complete ZIP and `DOWNLOADS-SHA256SUMS`
are preserved byte-for-byte inside `archive-ud-nightly-2.1.zip`, attached to the
[2.1.1 release](https://github.com/Umberto-DEV/melonDS-android/releases/tag/ud-nightly-2.1.1).
The `ud-nightly-2.1` tag ref was removed when 2.1.1 was published; source commit
`e68e9459` remains in `main` history.

| Field | UD Nightly 2.1 |
| --- | --- |
| Tag at publication (subsequently removed) | `ud-nightly-2.1` |
| Released app source | `e68e9459b240e297407bbf166c520d5e39e6e02c` |
| Integrated official app | `273211564bea8f9da0c9fe71b4006a06f5d4a2b5` |
| Pinned core | `83d443c546e1c338eb69003ba27347ad5c5599f5` (native correction set unchanged from `ac5e6ba0`) |
| Package / code | `me.magnum.melonds.nightly` / 53 |
| Display version | `2.1-UD GH (NIGHTLY)` |
| APK SHA-256 | `8ace1b87edb37a4368f16b0e8b42e28e0d05235f2ae7722feb4c3266a7145187` |
| Complete ZIP SHA-256 | `4183edae58d653e6fcfbcdd3dad44c2f6e9dd6102ef2718150b5765d0a6d0d8f` |
| Certificate SHA-256 | `832a0223e29802245c0e9f8ca24c8358e7d21cfe0dbea347845fda751f950d86` |
| Native frontends | ARM64, ARM32 and x86-64 |
| Exact sources / payload checksums | `BUILD-MANIFEST.json` and `SHA256SUMS` inside the complete ZIP |

Extract the complete ZIP to obtain `melonDS-UD-nightly-2.1-source.zip`, the
app/core source patches, `LICENSE`, `README.txt`, `BUILD-MANIFEST.json` and
`SHA256SUMS`. The source ZIP includes every pinned dependency. The APK and
complete ZIP remain the original release bytes; later documentation or
packaging-tool commits on `main` do not change their app source identity above.
The bundled documentation is the original release snapshot; this page records
the current download layout. `tools/release_bundle.py` exports committed
Git objects including pinned core, oboe, faad2, enet and vendored dependencies.
It independently verifies source archive bytes/modes, both complete patched
Git trees and distribution ZIP readback. Six synthetic packaging tests include
executing the indexed patch/submodule recipe in a clean checkout.

The app patch begins at official `27321156`; the core patch begins at
`431ab4bd`. [Apply instructions](../user-guide/SOURCE-PATCHES.md) update the
index gitlinks before synchronizing/checking out submodules. Original licenses,
notices and human contributor attribution are preserved. The ZIP layout is a
fork distribution choice: no special upstream ZIP specification was found in
the official README or release workflows. Upstream build/signing instructions
and the GPL license remain applicable.

Fresh source checks include 378 app + four API tests, without failures, errors
or skipped tests, 56 focused cheat tests, successful three-ABI release assembly
and lint with zero errors / 483 warnings. Final APK checks, exact pushed CI
and any Android runtime acceptance are identified separately in the manifest.
Historical evidence below remains attached to its original binaries. Physical
Thor, all-game compatibility, DSi qualification and performance are not inferred.

The [historical 2.1.11 release metadata](https://api.github.com/repos/Umberto-DEV/melonDS-android/releases/437121674)
retains the identities of its seven unchanged original assets. After the old
tag ref was removed on 8 November, its browser page and ordinary download URLs
return 404. The complete corresponding source remains publicly retrievable by
asset ID using the [API download command below](#historical-source-download).
Published source commits and contributor history remain intact.

---

# Historical release: UD Nightly 2.1.11/52

[Original release and asset metadata](https://api.github.com/repos/Umberto-DEV/melonDS-android/releases/437121674).
The records below describe the historical code 52 binary and its package
revision 2. The original seven assets remain retained; their ordinary browser
download URLs no longer work after the old tag ref was removed.

## Historical source download

Retrieve the original complete source without authentication through the release
asset API. The `Accept` header requests the file content instead of asset metadata:

```sh
curl -fL -H 'Accept: application/octet-stream' \
  'https://api.github.com/repos/Umberto-DEV/melonDS-android/releases/assets/718764276' \
  -o historical-source.tar.gz
printf '%s\n' '47930af53582a7c5c8df289e26484d51ce90174a904ed17bace31849897b6ac9  historical-source.tar.gz' | shasum -a 256 -c -
```

This anonymous download was verified on 8 November 2026: 26,587,945 bytes,
matching the original source archive's SHA-256 above. The release metadata lists
the other original asset IDs, including its build/source manifest; retrieve
them through `/releases/assets/ASSET_ID` with the same `Accept` header.

| Field | Historical public artifact |
| --- | --- |
| Binary tag at publication (subsequently removed) | `ud-nightly-2.1.11` |
| Released app source | `ac42561495ee6351d7f119f68760509c268f582e` |
| Embedded core | `06f3df985f67416368e0716174b7f06934a10229` (native code unchanged from `ac5e6ba0`) |
| Integrated official app | `feab12e597402565d245796669b2d0597f4b860d` |
| Package / code | `me.magnum.melonds.nightly` / 52 |
| Display version | `2.1.11-UD GH (NIGHTLY)` |
| APK SHA-256 | `d3cf208e4380c4e3060ca9f33a10d8d6d1c4621c3f36c1a037bbfff11a01a928` |
| APK size | 20,786,147 bytes |
| Certificate SHA-256 | `832a0223e29802245c0e9f8ca24c8358e7d21cfe0dbea347845fda751f950d86` |
| Native frontends | ARM64, ARM32 and x86-64 |

The historical release's `BUILD-MANIFEST.json` and `SHA256SUMS` retain exact
source, CI, packaging, signature and alignment evidence. The old UD tag refs
were removed on 8 November 2026; official nightly uses its rolling
`nightly-release` convention.
This is not an official stable 2.0.2 release.

Source integration `7f8b9026` passed 367 app + four API tests (371 total),
ARM64 debug assembly and lint with zero errors / 479 warnings. Those are
historical source checks, distinct from release signing, packaging and CI.
The subsequent [post-release smoke](POST-RELEASE-SMOKE.md) freshly downloaded
this exact APK, passed an in-place public 51→52 update, checked protected data
and ran copied raw-save Continue → native SAVE → cold Continue on API35
ARM64/16 KiB. The release-time report itself did not include those later runs.
No physical Thor update, universal compatibility, DSi qualification or native
menu performance benchmark is implied.

The cumulative app source patch starts at official `feab12e5`; the core patch
starts at `431ab4bd`. The full source archive includes all four pinned
submodules and their licenses. No ROM, raw save, BIOS, firmware/NAND, credentials
or signing material is distributed. No legacy Thor `.perf` APK is provided by
this release. Use [the update guide](../user-guide/UD-NIGHTLY.md#installation-and-data-preservation)
to preserve package, signer and data; the inherited in-app checker still
follows the original project.

## Package revision 2

The `ud-nightly-2.1.11-r2` release updates the source/documentation package.
The signed code 52 APK and its app ac425614/core 06f3df98 binary identities
remain unchanged. The new source archive and source patches include current
English documentation and pin the core documentation checkpoint 83d443c5.
BUILD-MANIFEST.json identifies binary and packaged-source revisions separately.
