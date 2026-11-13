# Official source integration

## Cheat search reconciliation — 8 November 2026

UD Nightly 2.1/53 (and the current 2.1.1/54, which keeps the same official app) integrates official app
`273211564bea8f9da0c9fe71b4006a06f5d4a2b5` from fork parent
`a2668004e75d5f90f921d3a971f1c079a867a144`. The four incoming commits are
[takanuva15's PR #1677](https://github.com/rafaelvcaetano/melonDS-android/pull/1677)
and Rafael Caetano's merge. Their authorship and commits are retained.
Core `83d443c5` is unchanged; this integration changes no save or database format.

The upstream toolbar search, shared cheat options menu and pending-toggle
snapshots are retained. Toolbar search reuses UD's indexed selector destinations:
plain cheat rows support toggle/edit/delete, while family and generated option
rows open the explicit picker. Finding an option does not enable it.
The prior folder search and its navigation remain available. Index creation and
toolbar filtering run on Dispatchers.Default. A folder-name match includes its
cheats; folder-scoped searches use IDs so equal folder names remain separate.
Deletion retains effective pending state, removes obsolete pending IDs and undo
restores into the result's original folder with the existing exclusion rules.

Initial focused tests reproduced the missing toolbar and stale pending-folder
snapshot. The first integration additionally reproduced missing generated
options and results crossing identical folder names. An initial checkbox
assertion was an invalid test assumption about passive semantics, not a product
defect. [Current release evidence](RELEASE.md) records final checks and limits.
Cold-cache builds exposed missing dependency metadata checksums; each added
artifact was compared byte for byte with official Maven Central while strict
Gradle verification stayed enabled. Dependency versions were unchanged.

The cumulative app source patch starts at the current official app revision and
includes the full app tree, fork submodule configuration and exact core gitlink.
[Patch instructions](../user-guide/SOURCE-PATCHES.md) use indexed application.
The release packaging tool checks complete temporary-index tree equality and
reads back recursive source archives against committed Git objects.

## Earlier official reconciliation — 6 November 2026

6 November 2026. Source integration joins UD `e5184229bf90c45b1762fcca132f9987813f73c7` with official `feab12e597402565d245796669b2d0597f4b860d`, including its six commits after `fa214944`. The pinned core remains `06f3df985f67416368e0716174b7f06934a10229`; no native-core change is part of this integration.

## Resulting behavior

- Integrate the official PackageInstaller session update flow, explicit download-failure status, localized download label, cursor lifetime and complete removal of the old UpdateContentProvider/manifest/XML paths. Retain ContextCompat receiver registration. Android remains responsible for package/version/signing compatibility.
- Integrate archive failure messages with the selected ROM URI and the upstream large-heap request. Retain serialized extraction, heap-aware SevenZ memory limits, expanded-byte limits, nullable ZIP-size handling, bounded traversal and transactional cache replacement. A large-heap request does not establish that a device grants additional memory or that arbitrary archives will fit.
- Retain immutable Action revisions and minimal explicit SDK provisioning in all workflows, already implementing the official setup-android v4 change with the pinned v4.0.4 action. Keep fork verification, signing and publication boundaries.
- Retain default-off, forward-only RTC correction and the stopped-session resume guard. Preserve library removal, save/import recovery, settings and all earlier UD corrections.

## Verification

Local verification passed with Java21 and the configured Android SDK: **367 app +4 API tests,371 total**, zero failures/errors/skips; ARM64 GitHub Nightly debug APK assembled; lint reported zero errors and 479 warnings. The changed source/configuration stayed unchanged throughout the accepted run. The source diff against the UD parent, excluding documentation, has SHA-256 `c7fedafbaf62ba3f4abd109d91e839b4a43c8ef6f9daff85993204f4d6dbcc37`.

The first integration test run exposed four API28 fixture failures: Robolectric did not grant the app's declared/requested signature-level dynamic-receiver permission. The fixture now verifies that declaration/request and grants exactly that permission in the shadow; all eight updater cases then passed before the full run. Production receiver guards were unchanged. Archive component tests cover URI/name/throttle behavior, decoder closure and best-effort notification failure handling on API28/35. Updater callback tests cover API28/33; they do not execute real APK copy/fsync/commit or Android signing decisions.

The source checks above belong to integration `7f8b9026`, which subsequently
passed its exact CI. [Current release](RELEASE.md) and [post-release smoke](POST-RELEASE-SMOKE.md)
record the packaged successor and later execution. Earlier candidate evidence
is retained under its exact identities in the [library-entry dossier](bugs/library-entry-removal.md).
