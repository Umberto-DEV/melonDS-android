# Repository workflow

- Work directly on `main` in this repository and `melonDS-android-lib`.
- Do not create additional branches or worktrees unless the user explicitly changes this instruction.
- Preserve existing local changes and original contributor attribution.
- Run the relevant JVM, Android build, lint and native checks before claiming a change is verified. Report skipped checks and device-only validation separately.
- Replacement upstream PR publication is currently blocked by the user pending a coordinated debugging review with the Sacred Gold Plus chat and a later explicit publication instruction. Pushing authorized work to the existing `main` branches for CI does not lift that gate.
- The upstream repositories use `master`; a future upstream PR should use this fork's `main` as its source and the upstream default as its target.
