# Applying the UD source patches

The download contains one Android APK for fresh installation or a same-signer
UD update, and separate **source patches** for developers. There is no binary
patch that can replace the official developer's Android signature.

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

The distributed UD binary uses version code 53 and the display version
`2.1-UD GH (NIGHTLY)`. Reproduce the exact source/build metadata in
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
