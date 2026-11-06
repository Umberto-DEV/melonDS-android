# Current release: UD Nightly 2.1.11/52

[Download and release assets](https://github.com/Umberto-DEV/melonDS-android/releases/tag/ud-nightly-2.1.11-r2).
Only this Android release is offered. Previous Android release packages were
withdrawn on 6 November 2026; source tags and published history remain.

| Field | Current public artifact |
| --- | --- |
| Immutable tag | `ud-nightly-2.1.11` |
| Released app source | `ac42561495ee6351d7f119f68760509c268f582e` |
| Embedded core | `06f3df985f67416368e0716174b7f06934a10229` (native code unchanged from `ac5e6ba0`) |
| Integrated official app | `feab12e597402565d245796669b2d0597f4b860d` |
| Package / code | `me.magnum.melonds.nightly` / 52 |
| Display version | `2.1.11-UD GH (NIGHTLY)` |
| APK SHA-256 | `d3cf208e4380c4e3060ca9f33a10d8d6d1c4621c3f36c1a037bbfff11a01a928` |
| APK size | 20,786,147 bytes |
| Certificate SHA-256 | `832a0223e29802245c0e9f8ca24c8358e7d21cfe0dbea347845fda751f950d86` |
| Native frontends | ARM64, ARM32 and x86-64 |

The release page's `BUILD-MANIFEST.json` and `SHA256SUMS` retain exact source,
CI, packaging, signature and alignment evidence. The fork uses immutable UD
version tags; official nightly uses its rolling `nightly-release` convention.
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
