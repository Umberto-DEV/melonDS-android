# Stage replacement DSi saves before removing the prior generation

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** High functional impact: previous DSi save loss. Functional impact labels are not CVSS/security ratings.

The original source/test identities and limitations are retained below. The current release and exact-APK execution are recorded separately.

## Problem and reachable path

`NANDMount::ImportTitleData` removed the existing public/private/banner save before opening/copying the replacement. The Android DSiWare picker reaches this path through AndroidDSiNandManager and JNI.

The remove-before-open order originated in Arisotura's core commit
`235da420c8ac0bcec5a21db6f3cce4ae190fb7c5` (24 August 2021). That is
source provenance, not attribution of the later transactional correction.

## Correction

Copy to a temporary FAT file, preserve the old generation in a backup, commit the staged file only on success and recover an interrupted prior transition. Metadata-fault refinement is documented separately.

## Evidence and controls

On the real encrypted core/FatFs volume, a missing source made the prior entry disappear (FR_NO_FILE=4); a read fault truncated prior bytes. ExistingSaveReplacementTest reruns missing/read/write/full-volume failures, successful retry, interrupted transition and private/banner equivalents with remount/readback. The later rename-fault dossier records one-shot metadata failures.

## Limits and provenance

Historical proof started at app 1cf4fa6adf3c7a8621734754d907ea0d27824fe9/core fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e with recorded NAND diffs. FatFs rename is not universally atomic against power loss; persistent I/O failure, actual HOME/logical quota and an end-to-end ART failing-provider import remain separate.

## Source and regression tests

- [ExistingSaveReplacementTest.cpp](../../../tools/host-tests/nand-fat/ExistingSaveReplacementTest.cpp)
- [README.md](../../../tools/host-tests/nand-fat/README.md)
