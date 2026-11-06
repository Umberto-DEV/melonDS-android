# Persist the newly selected library sort order

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Impact:** Low functional impact: wrong ordering after restart. Functional impact labels are not CVSS/security ratings.

Historical refinement acceptance: app `89053f0fd96ef2bc616849deb15900e1d6b09f43`, core `fd7582400bb7e35fe0d36dbb0c89b6766c1a8f2e`; earlier eight-group corrections were verified at app `19071123` with the same core. Counts below belong to their named historical campaign, not a fresh execution on every later APK.

## Problem and reachable path

The UI persisted the previous sort value instead of the newly selected value.

## Correction

Persist the new sort selection and update the visible state consistently.

## Evidence and controls

The production RomListViewModel sorting regression covers selection and persistence as part of the historical nine scan/sort tests.

## Limits and provenance

No storage/provider or startup-performance improvement is inferred.

## Source and regression tests

- [RomListViewModelSortingTest.kt](../../../app/src/test/java/me/magnum/melonds/ui/romlist/RomListViewModelSortingTest.kt)
