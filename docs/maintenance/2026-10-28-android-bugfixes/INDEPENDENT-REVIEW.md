> Historical review of `3e0a67af`. R1–R3 were subsequently reproduced on JVM/Android and corrected in `19071123`; see VALIDATION.md. This original verdict is preserved, not replaced by an invented second review.

# Final senior review — Android issue remediation

Reviewed immutable app range `05c38d61cf5eb124105dddc6e2358c70b4448300..3e0a67af0fa9a331bc58b04f4e5e95caa4cfeb82`, its review package, plan/spec and SDD rulings. Core remains `fd758240`. Production, index, HEAD and branch state were not changed. No Gradle, ADB, subagents, Daybreak, branches, worktrees or publication were used for this review. The executor authorized writing the diagnostic script/log below.

## Strengths

- Microphone amplification now saturates before narrowing, preserving signed limits and existing gain. The read cursor belongs to the capture callback/ring, so a blow-recording position and an earlier session cannot index a new ring. Literal PCM and lifecycle regressions exercise production source.
- NAND checks read the real 520-byte `TitleMetadata` representation, compare ROM/TMD identity before mutation, preserve duplicate installed titles, and correct the actual core `DeleteTitle(category, id)` argument order. The missing-NAND/short-BIOS instrumented tests use actual Android JNI, separately from modeled NAND import tests.
- Metadata transport uses `readFully` and closes the stream on both success and failure. Full, fragmented, truncated, empty and extra-prefix cases are covered without changing its public result type.
- Touch transitions use Android pointer indices correctly: POINTER_UP excludes `actionIndex`, POINTER_DOWN includes all current pointers, and only final UP/CANCEL releases the stylus. Two- and three-pointer tests cover lifting index zero as well as later indices.
- Atomic replacement uses one Room transaction, retains custom cheats, remaps imported database IDs, validates event order/completion, and avoids buffering the whole parsed database. Real SQLite failure/rollback tests are substantially stronger than mocked repository results.
- Native documentation distinguishes Android NDK execution, real JNI instrumentation, boundary adapters and physical/game validation. The change stays narrow and preserves the core and contributor history.

## Issues

### Critical

None identified.

### Important

**R1 — Real DAT parser cancellation can hang the replacement transaction.**

- References: `app/src/main/java/me/magnum/melonds/common/cheats/CheatDatabaseImportEvents.kt:19` and `:20`; interaction with `UsrCheatDatabaseParser.kt:67`, `:102` and `:114`.
- Trigger: import a valid DAT with enough games to fill the bounded queues, then cancel its consumer. The new callback's `trySendBlocking(...).getOrThrow()` throws a cancellation exception inside one of the DAT parser's child workers. The parser's separate `runBlocking` scope is not linked to the outer import job. Cancellation of that child does not fail its parent; eventually the workers can all exit while the producer remains suspended on its full internal `GameData` channel. The outer flow waits for the synchronous parser forever.
- Impact: cancellation need not finish, the input stream remains owned, and a Room transaction can remain open rather than completing rollback. Later imports/cheat writes can be blocked. The existing bounded-producer test uses a single-threaded fake parser, while the cancellation/Room test uses a plain `flow`, so neither covers the production combination.
- Reproduction: the saved probe creates 100 valid game records from the permanent synthetic fixture and collects `importEvents(...).take(2)`. At 3 seconds it reports `REAL_DAT_CANCEL: terminated=false threadAlive=true`; the production parser thread is parked in `runBlocking` at line 67. JShell then exits its separate process, leaving no probe threads behind.
- Fix: propagate callback cancellation/failure to the complete parser producer/worker scope and tie outer import cancellation to that scope; ensure abnormal worker termination cancels/unblocks the producer and joins all workers. Add real multi-game DAT consumer-cancellation and failure tests, plus a real Room rollback check. A single-game callback throwing `CancellationException` must not be swallowed as successful completion.

**R2 — A DAT truncated exactly at an advertised game offset is accepted and replaces the old database with no imported games.**

- References: `app/src/main/java/me/magnum/melonds/common/cheats/UsrCheatDatabaseParser.kt:94`, `:176` and `:180`; destructive consequence at `app/src/main/java/me/magnum/melonds/impl/RoomCheatsRepository.kt:160` and `:169`.
- Trigger: `UsrCheatFixture.bytes().copyOf(288)`. Its complete address book advertises a game at offset 288, but EOF occurs at that offset. `readBytesOrUntilEnd` returns an empty block, `parseGame` treats its empty name as a game to skip, and `onParseComplete` is still emitted. An address entry with an offset before the current stream position is similarly skipped at lines 79–81.
- Impact: the replacement stream contains Start and Complete, so the new transaction's final check passes and deletion of the previous imported cheats commits. Custom cheats survive, but the imported database is lost after a malformed/truncated file was reported as successful. Transaction atomicity alone cannot protect against a parser that misclassifies malformed input as complete.
- Reproduction: the saved production-parser probe prints `TRUNCATED_AT_GAME_START: complete=true games=0`. The database consequence follows directly from the reviewed transaction; this review did not separately execute SQLite for the additional fixture.
- Fix: reject an advertised game with no readable game data and invalid/backward offsets instead of skipping them. Validate required block structure before declaring the file complete. Extend both streaming and real worker/Room integrity regressions with this exact EOF boundary and keep the complete valid fixture as control.

**R3 — Reconnect plus inset rotation discards a compatible custom layout.**

- Reference: `app/src/main/java/me/magnum/melonds/impl/layout/UILayoutProvider.kt:180`.
- Trigger: a saved dual-display layout uses external display ID 1. That same type/resolution display reconnects as ID 42 while primary insets rotate 180 degrees (for example top 40 becomes bottom 40), with the same orientation and UI size. The earlier equivalent-display path requires exact inset equality, so it misses; the final fallback now requires exact raw display equality and also misses. `getOptimalLayoutForVariant` therefore selects the default layout instead of the compatible custom layout.
- Impact: reconnecting and rotating replaces the user's custom screen/control positions even though neither the topology nor display specifications changed. This contradicts the reconnect compatibility requirement and regresses the previous inset-adjusting fallback.
- Reproduction: the saved probe calls the production selection method with the two independent controls: reconnect alone `reuses=true`, rotation alone `reuses=true`, combined `returned=null`. It uses only selection logic; it does not claim device rendering validation.
- Fix: restrict fallback by compatible topology/type/size while ignoring the relevant transient display ID, then retain the existing inset adjustment. Keep the exact saved-layout preference and the disconnected single-display distinction. Add a public-flow regression combining reconnect and rotated insets.

### Minor

None worth blocking or adding cleanup scope for.

## Verification examined

- `git diff --check 05c38d61..3e0a67af` passes; the core submodule is unchanged in this range.
- Existing captured 4 KiB reports contain 217 JVM tests and 32 Android instrumentation tests, with zero failures/errors/skips. Task 4/5/6/8 logs inspected also report successful builds. These are existing executor reports, not fresh runs by this reviewer, and do not cover the three new triggers.
- Independent probes run against the already compiled production JVM classes, with no Gradle/ADB: `build/android-bug-audit/final-review-probes.py`; output `build/android-bug-audit/final-review-probes.log`. These are host logic diagnostics, not Android runtime validation.
- Final integrated JVM, lint, APK, Android 4/16 KiB and native validation belongs to the executor after its correction pass. This review makes no final-head CI or device-pass claim.

## Declined to judge

The executor should explicitly retain or revise each scope ruling; none is silently considered fixed.

- Physical microphone sensitivity, permissions, routing/Bluetooth, real Oboe callback timing and concurrent lifecycle races: the supplied lifecycle boundary substitutes stream opening, and no physical microphone test was available.
- Nonconstant microphone resampler fidelity, unusually short hardware callback buffers, and freshness of queued samples across mute/re-enable: interpolation and that same-session queue policy are unchanged; the implemented scope is gain saturation and new-ring/new-session cursor ownership.
- Real NAND encryption/filesystem mounting, complete ROM-header validity beyond the title-ID prefix, TMD authenticity, and pending JNI allocation/null-string exceptions: the changed checks establish readable identity and metadata size/match; the modeled import tests do not establish those broader properties.
- FAT full-disk/short-write/close failure propagation, durability of real NAND rollback and DSi logical quota/HOME Menu boot: the unchanged core's `ImportFile`/metadata writers do not check every write result, and the boundary model deliberately injects a false `ImportTitle` result instead. The reviewed fixes correct handling of reported failure; they do not prove every underlying partial write is reported. Real NAND/authorized title fixtures were unavailable, and this remains distinct core/storage work.
- Live NUS endpoint availability/authenticity and cancellation during a blocked provider/network read: transport tests substitute an InputStream; this patch verifies exact-length success/EOF cleanup rather than live service or interruptibility behavior.
- Physical external-display reconnect timing, Thor offsets, fold/OEM window behavior and game touch latency: the shared tests cover production layout selection and real Android MotionEvents, not actual hardware routes or game timing. Manually fabricated impossible pointer counts are outside Android's valid MotionEvent transition contract.
- Arbitrarily large/malicious cheat resource exhaustion, the existing complete address-book allocation and 32-bit total-byte counter, and game-specific cheat correctness: these existing parser/resource and gameplay questions were not expanded into this bounded streaming/atomicity review. Real cancellation and malformed EOF affecting the new replacement transaction were judged and are reported above.
- Mac frontend/JIT defects, other issue-inventory categories, publication readiness and future upstream PR coordination: outside the specified Android patch review; publication remains gated by the user.

## Assessment

**Ready to merge: No, at reviewed head `3e0a67af`.** The production-source fixes and test boundaries are mostly sound, but the two real DAT failure paths violate safe replacement/cancellation, and display compatibility regresses under a combined normal lifecycle transition. Fix R1–R3, run the corresponding permanent regressions and the executor's integrated checks, and preserve the stated hardware/core limitations. No duplicate reviewer or second review is requested.
