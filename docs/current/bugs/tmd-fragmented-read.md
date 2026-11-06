# Read the complete TMD prefix or reject it

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Moderate functional impact: incomplete metadata adoption. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

The DSiWare metadata downloader used one stream read. A valid fragmented response produced a 520-byte buffer with an unwritten zero suffix.

## Correction

Read exactly the required 520-byte prefix or reject EOF, close the stream on all outcomes and reject unapproved transport. No production source is enabled by default without an explicit HTTPS base.

## Evidence and controls

Five identical JVM/ART cases cover a complete response, 17-byte fragments, a 15-byte/truncated response, empty input and extra data beyond the prefix; byte contents and stream closure are asserted. The transport-security occurrence has its own security dossier.

## Limits and provenance

Complete length is not proof of publisher authenticity or DSi HOME/quota acceptance. No enabled metadata service is inferred.

## Source and regression tests

- [NusDSiWareMetadataRepository.kt](../../../app/src/main/java/me/magnum/melonds/impl/NusDSiWareMetadataRepository.kt)
- [NusDSiWareMetadataRepositoryTest.kt](../../../app/src/sharedTest/kotlin/me/magnum/melonds/impl/NusDSiWareMetadataRepositoryTest.kt)
- [NusDSiWareMetadataRepositorySecurityTest.kt](../../../app/src/test/java/me/magnum/melonds/impl/NusDSiWareMetadataRepositorySecurityTest.kt)
