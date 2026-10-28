# Single main consolidation and prepublication verification

> **For agentic workers:** Use superpowers:executing-plans. The user has authorized execution and explicitly requires working only on main.

**Goal:** Preserve the most complete app/core implementation on one local and fork branch, main, archive and close the old-source PRs, and verify before any replacement publication.

**Architecture:** Compare all local and remote refs and the uncommitted working tree. Preserve recoverable bundles outside the checkout. Consolidate app and core separately, retaining attribution, source changes and upstream merge ancestry. Keep upstream repositories unchanged.

**Tech Stack:** Git, GitHub, Kotlin/Gradle, C++, Android, host sanitizers.

**Spec:** User request of 28 October 2026 in this chat; phases 1–3 authorized, phase 4 publication explicitly blocked pending coordination with a named chat.

## Global Constraints
- Only main in local app/core heads and the two user-owned GitHub forks; no additional development branches.
- Preserve existing local changes and original contribution credits.
- New PR source will be main; target remains the actual upstream default master.
- Archive exact existing PR bodies, refs and comments before closing them.
- Do not publish replacement PRs or contact another chat until authorized/identified.
- Report actual checks and outstanding limitations; never describe skipped or unapproved CI as passed.

## Review Focus
- Unique commits on stale/divergent branches must survive by ancestry or documented equivalent source.
- App gitlink must resolve in the current public core fork from a clean clone.
- CI must run safe verification on main and must not publish releases from forks.
- Community changes need focused regression evidence and authorship.
- Lint, host tests and supported ABI build failures must be surfaced and repaired before declaring green.

### Task 1: Inventory and preservation
- [x] Inspect branches, dirty files, remotes and current PR metadata.
- [x] Save bundles, dirty patch and untracked files outside the repository.
- [x] Compare all histories and tree differences; choose lossless consolidation.
- [x] Preserve updated remote refs in backup bundles.

### Task 2: Consolidate branch names and PR state
- [x] Rename existing development branches to main and consolidate relevant app/core content.
- [x] Correct core fork URL and app gitlink.
- [x] Close archived PR 1682 and core PR 19; create no replacements.
- [x] Publish main, set fork default main, remove redundant branch refs only after preservation checks.
- [x] Record branch invariant verification.

### Task 3: Community review and validation
- [x] Refresh open PR/issue inventory; compare against prior October 24 audit.
- [x] Integrate only reviewed relevant changes with focused regression tests.
- [ ] Run JVM tests, ARM64 Android build, Android lint, native sanitizer/JNI/DNS checks and relevant x86_64 validation.
- [ ] Run fork CI on final main commit and inspect completed results.
- [ ] Review final changes and preserve evidence and updated unpublished PR descriptions.

### Task 4: Publication gate
- [ ] BLOCKED BY USER: no new PR publication until additional debugging and coordination with the chat the user will name.

## Progress and decisions
- Initial app HEAD: 39a9fe56; published app PR HEAD: aedb719f. They are different histories; ventuno alone is incomplete.
- Initial core HEAD: bc060f4c; published core PR HEAD: f950b5e6. Compare trees before choosing.
- Original backup: /Users/umbertocipriano/Developer/melonds-backup-20261028-125946.
- Ruling: execute the explicitly authorized consolidation without another planning approval; preserve existing changes and avoid new branches per user instruction.

- Consolidation complete: all original local branch tips remain ancestors of main; both user-owned forks and local repositories have exactly main. Core source equality verified before adding CI.
- PRs 1682 and19 closed after complete body/metadata/comment archives. CLI used after connector403 (insufficient integration access). No new PR created.
- Local integrated verification:190 JVM tests,0 failures/errors/skips, ARM64 debug APK built, lint0 errors/474 warnings. Three focused regressions observed failing then passing.
- Review found recoverable UBSan reports could exit0; reproduced with a signed-overflow fixture (exit0 vs strict abort), enabled strict runtime failure for all native checks.
- Ruling: retain the existing presentation-context behavior. The SDK excludes AOSP's valid hidden TYPE_PRESENTATION from its IntDef, so annotate only that call with its source rationale. A trial public-type rewrite was rejected; dual-display Robolectric geometry failed identically before/after and is archived as non-diagnostic, not claimed passing.
- Other reviewed deferrals: whole audio stretching/paired buffer rewrite (#1676/core17), recovery state machine (#1666) and broad focus gating (#1657) lack safe bounded integration/device evidence; retain existing targeted adaptations.
- Sacred Gold Plus coordination received. Daybreak must not be launched until the user confirms authentication/authorization; ordinary verification continues.
- The first native CI attempt exposed a runner-specific hardcoded CMake path; the second reached tests and exposed a missing standard memory include and a genuine JIT teardown lifetime error on both Linux architectures. These failures were archived and corrected without disabling checks. Fast-memory mappings now reset in NDS destruction while ARM9 is alive, with unchanged memory ownership and member order; independent review confirmed the DSi and no-JIT paths. Local native checks and the no-JIT core build passed, and Android tests/build/lint remain green. Updated Linux CI is required before completing Task 3.
