# Applying the UD source patches

The download contains one Android APK for fresh installation or a same-signer
UD update, and separate **source patches** for developers. There is no binary
patch that can replace the official developer's Android signature.

## Exact starting revisions

| Component | Official source revision |
| --- | --- |
| Android app | `feab12e597402565d245796669b2d0597f4b860d` |
| Android core | `431ab4bd0003c4356e25fce89640ae1004579b9b` |

The Android app patch includes `.gitmodules` and the core gitlink, so it
reconstructs the complete app source revision. Start with a clean checkout of
the stated Android app base. Keep your working project and its data backed up.

```sh
git apply --check /path/to/melonDS-UD-2.1.11-app-source.patch
git apply /path/to/melonDS-UD-2.1.11-app-source.patch
git submodule update --init --recursive
```

The submodule command fetches the exact pinned core and other dependencies.
Do not then apply the core patch to that already updated core checkout.

The separate core patch is an alternative for developers working directly
from the official core revision listed above:

```sh
git apply --check /path/to/melonDS-UD-2.1.11-core-source.patch
git apply /path/to/melonDS-UD-2.1.11-core-source.patch
```

Stop if a check fails. Do not force patches onto a different revision. Each
patch was applied to a temporary index at its stated base; the resulting
complete index, including file modes and app gitlinks, matched the fork.

The self-contained source archive is an alternative: it includes the pinned
core and other submodule sources, with their licenses. A plain GitHub automatic
source ZIP does not include submodule contents.

## Build and sign

Use Java 21 and the SDK, NDK and CMake versions declared by the source. Configure
your own signing key in untracked `local.properties`, following the main
[build instructions](../../README.md#building), then run:

```sh
sh ./gradlew :app:assembleGitHubNightlyRelease
```

The distributed UD binary uses version code 52 and the display version
`2.1.11-UD GH (NIGHTLY)`. Reproduce the exact source/build metadata in
`BUILD-MANIFEST.json`; do not lower the version code of an existing installation.

Use the existing signing identity for the installation you intend to update.
The original developer can sign the patched source with their official key;
another developer's key cannot update that official installation. UD downloads
use the persistent UD key. An APK also needs the same application ID and an
allowed versionCode to update an existing package.

Before distributing a rebuilt APK, repeat the release checks and data-preserving
update tests. A successfully applied source patch is not Android runtime
acceptance. ROMs, saves, BIOS, firmware, NAND, private keys and personal backups
are not part of these patches or source packages.
