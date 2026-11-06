# Nightly Build — UD 2.1.11

6 November 2026. Fork version **2.1.11/52** packages the reconciled source
including official `feab12e5` through `7f8b9026`. The pinned core remains
`06f3df985f67416368e0716174b7f06934a10229`; native code is unchanged at `ac5e6ba0`.

The standalone APK keeps package `me.magnum.melonds.nightly`, the persistent
UD signing identity and ARM64/ARM32/x86-64 frontends. Its display version is
`2.1.11-UD GH (NIGHTLY)`. The immutable tag is `ud-nightly-2.1.11`; this is
the fork's own version sequence, not an official stable 2.0.2 release. Official
nightlies use a rolling `nightly-release` tag; UD retains immutable versioned
releases. The [official nightly workflow](https://github.com/rafaelvcaetano/melonDS-android/blob/feab12e597402565d245796669b2d0597f4b860d/.github/workflows/main.yaml)
records the upstream title, package filename, commit suffix and rolling-tag convention.
See the [release page](https://github.com/Umberto-DEV/melonDS-android/releases/tag/ud-nightly-2.1.11)
for availability, source/binary digests, exact successful CI and build checks
in `BUILD-MANIFEST.json` and `SHA256SUMS`.

[Integration 7f8b9026](../2026-11-06-upstream-reconciliation/README.md) passed
367 app + 4 API tests, 371 total, ARM64 debug assembly and lint 0 errors/479
warnings; its exact CI passed. Those are historical source checks. The
versioned release head requires its own successful CI before publication.
No ROM, emulator, raw-save, update-installation or physical-device tests are
performed for 2.1.11. Signature/build/ABI/alignment verification is not runtime
data-preservation or universal game/device acceptance.

The published 2.1.10 remains immutable at production `bd546465`/distribution
`e5184229` and official baseline `fa214944`. Its APK and source archive do not
contain the later integration. Its [runtime evidence](../2026-11-06-library-entry-removal/README.md)
remains attached to those exact APKs and is not transferred to 2.1.11.

The new cumulative app patch starts at official `feab12e5`; the core patch
starts at `431ab4bd`. The full source archive includes all four pinned
submodules and licenses. No ROM, personal save, BIOS/firmware/NAND, credentials
or signing material is distributed. No separate core or legacy Thor APK is
provided by this delivery. In-app update discovery still follows the original
project; UD updates use this fork's release downloads.

## Subsequent execution

The release-time limits above are historical. See the separately dated [post-release Android smoke](POST-RELEASE-SMOKE.md) for the exact public APK update and copied-save execution on 6 November 2026.
