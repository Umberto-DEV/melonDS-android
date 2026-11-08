# Applying the UD source patches

Download the [complete UD Nightly ZIP](https://github.com/Umberto-DEV/melonDS-android/releases/download/ud-nightly-2.1/melonDS-UD-nightly-2.1.zip),
verify it against [DOWNLOADS-SHA256SUMS](https://github.com/Umberto-DEV/melonDS-android/releases/download/ud-nightly-2.1/DOWNLOADS-SHA256SUMS),
then extract it. It contains the Android APK, complete recursive source ZIP,
app/core **source patches**, build instructions, license, `BUILD-MANIFEST.json`
and payload `SHA256SUMS`. Use the extracted patch paths in the commands below.

The standalone APK is also available for fresh installation or a same-signer
UD update. A source patch must be rebuilt and signed; it cannot replace the
official developer's Android signature.

The bundle reconstructs released app `e68e9459b240e297407bbf166c520d5e39e6e02c`
and pinned core `83d443c546e1c338eb69003ba27347ad5c5599f5`. Later documentation
or packaging-tool commits on `main` do not change the published binary or ZIP.

## Exact starting revisions

| Component | Official source revision |
| --- | --- |
| Android app | `273211564bea8f9da0c9fe71b4006a06f5d4a2b5` |
| Android core | `431ab4bd0003c4356e25fce89640ae1004579b9b` |

The Android app patch includes `.gitmodules` and the core gitlink, so it
reconstructs the complete app source revision. Start with a clean checkout of
the stated Android app base. Keep your working project and its data backed up.

```sh
git apply --check --index /path/to/melonDS-UD-2.1-app-source.patch
git apply --index /path/to/melonDS-UD-2.1-app-source.patch
git submodule sync --recursive
git submodule update --init --recursive
```

Indexed patch application updates the core gitlink as well as files. The submodule
commands synchronize fork URLs and fetch the exact pinned core and other dependencies.
Verify `git submodule status --recursive`; no entry should begin with `+` or `-`.
Do not then apply the core patch to that already updated core checkout.

The separate core patch is an alternative for developers working directly
from the official core revision listed above:

```sh
git apply --check --index /path/to/melonDS-UD-2.1-core-source.patch
git apply --index /path/to/melonDS-UD-2.1-core-source.patch
```

Stop if a check fails. Do not force patches onto a different revision. Each
patch was applied to a temporary index at its stated base; the resulting
complete index, including file modes and app gitlinks, matched the fork.

To use the complete source instead of patches, extract
`melonDS-UD-nightly-2.1-source.zip` from the bundle. Its `source/` directory
includes the pinned core and other submodule sources, with their licenses.
A GitHub automatic source ZIP does not include submodule contents.

## Build and sign

Run the build from the extracted `source/` directory or the patched app checkout.
Use Java 21 and the SDK, NDK and CMake versions declared by the source. Configure
your own signing key in untracked `local.properties`, following the main
[build instructions](../../README.md#building), then run:

```sh
sh ./gradlew :app:assembleGitHubNightlyRelease
```

The distributed UD binary uses version code 53 and the display version
`2.1-UD GH (NIGHTLY)`. Reproduce the exact source/build metadata in the bundle's
`BUILD-MANIFEST.json`; do not lower the version code of an existing installation.

Use the existing signing identity for the installation you intend to update.
The original developer can sign the patched source with their official key;
another developer's key cannot update that official installation. UD downloads
use the persistent UD key. An APK also needs the same application ID and an
allowed versionCode to update an existing package.

Before distributing a rebuilt APK, repeat the release checks and data-preserving
update tests. A successfully applied source patch is not Android runtime
acceptance. Commercial ROMs, personal saves, BIOS/firmware/NAND dumps, private keys and personal backups
are not part of these patches or source packages.
