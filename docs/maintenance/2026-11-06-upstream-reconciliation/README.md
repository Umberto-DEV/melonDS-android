# Reconcile official Android updates while retaining UD fixes

6 November 2026. Source integration joins UD `e5184229bf90c45b1762fcca132f9987813f73c7` with official `feab12e597402565d245796669b2d0597f4b860d`, including its six commits after `fa214944`. The pinned core remains `06f3df985f67416368e0716174b7f06934a10229`; no native-core change is part of this integration.

## Resulting behavior

- Integrate the official PackageInstaller session update flow, explicit download-failure status, localized download label, cursor lifetime and complete removal of the old UpdateContentProvider/manifest/XML paths. Retain ContextCompat receiver registration. Android remains responsible for package/version/signing compatibility.
- Integrate archive failure messages with the selected ROM URI and the upstream large-heap request. Retain serialized extraction, heap-aware SevenZ memory limits, expanded-byte limits, nullable ZIP-size handling, bounded traversal and transactional cache replacement. A large-heap request does not establish that a device grants additional memory or that arbitrary archives will fit.
- Retain immutable Action revisions and minimal explicit SDK provisioning in all workflows, already implementing the official setup-android v4 change with the pinned v4.0.4 action. Keep fork verification, signing and publication boundaries.
- Retain default-off, forward-only RTC correction and the stopped-session resume guard. Preserve library removal, save/import recovery, settings and all earlier UD corrections.

## Distribution identity

The published [UD Nightly 2.1.10](https://github.com/Umberto-DEV/melonDS-android/releases/tag/ud-nightly-2.1.10) remains immutable at its documented production/distribution commits and checksums. Its archive and exact-base patches intentionally exclude these later official commits. This source integration does not replace that APK or its source archive. A future public binary needs a new version/release and its own acceptance; the existing runtime evidence is not transferred to this source.

## Verification

Local verification passed with Java21 and the configured Android SDK: **367 app +4 API tests,371 total**, zero failures/errors/skips; ARM64 GitHub Nightly debug APK assembled; lint reported zero errors and 479 warnings. The changed source/configuration stayed unchanged throughout the accepted run. The source diff against the UD parent, excluding documentation, has SHA-256 `c7fedafbaf62ba3f4abd109d91e839b4a43c8ef6f9daff85993204f4d6dbcc37`.

The first integration test run exposed four API28 fixture failures: Robolectric did not grant the app's declared/requested signature-level dynamic-receiver permission. The fixture now verifies that declaration/request and grants exactly that permission in the shadow; all eight updater cases then passed before the full run. Production receiver guards were unchanged. Archive component tests cover URI/name/throttle behavior, decoder closure and best-effort notification failure handling on API28/35. Updater callback tests cover API28/33; they do not execute real APK copy/fsync/commit or Android signing decisions.

No remote CI or physical runtime acceptance is claimed for this merged source. Its debug APK is a build check and does not replace the published2.1.10 APK. The withdrawn upstream submissions remain closed; source changes remain prepared locally. Future submission requires its own completed remote verification before publication.

The [2.1.10 candidate report](../2026-11-06-library-entry-removal/README.md) preserves the earlier 355-test, public/legacy APK, copied-save and physical preservation evidence under their exact source identities. This source update has separate verification.
