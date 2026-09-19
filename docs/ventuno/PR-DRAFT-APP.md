# PR draft — app (melonDS-android)

**Status: draft, not submitted.** Prepared 19 Sep 2026 so a future PR only needs
reviewing and pushing.

- Branch: `pr-prep/app` (local only, never pushed)
- Base: `upstream/master` = rafaelvcaetano/melonDS-android `c42995ca`
- 14 thematic commits, 147 files, +8167/-548
- **Blocked on the engine PR** (`PR-DRAFT-CORE.md`): this branch still points the
  submodule at upstream's `431ab4bd`, so it does not build until the engine
  changes are merged and the pointer is bumped. That bump is deliberately left
  out of the branch.

For the plain-language feature list to paste into the PR body, see
`WHATS-NEW-VS-UPSTREAM.md`.

---

## Title

melonDS nightly "ventuno": guided BIOS setup, online connection manager, settings backup, and a batch of stability fixes

## Body

This is several months of work on an AYN Thor (Adreno 740, Android 13), split
into one commit per area so each can be reviewed — or dropped — on its own.
Every change here was seen working on the device; anything that only worked in
theory was removed from the branch before this PR.

### Features

- **Guided BIOS setup** — classifies the files you point it at, explains what is
  missing, wrong-sized or from the wrong console, and copies them into place off
  the main thread.
- **Online connections (WFC) manager** — three independent connection slots with
  their own DNS, editable from the settings and from the pause menu, for every
  firmware. Requires the engine's DNS override support.
- **Settings backup and restore.**
- **Optional console clock sync with the device**, off by default, applied on
  resume, never moving the clock backwards.

### Fixes

- **Saves are no longer truncated before the new data is written**, with a
  fallback path and a user-visible error when a write fails.
- **Audio**: serialised stream state changes, reopen on resume, no dereference of
  a stream that failed to open, and previous streams closed before a new session
  starts (a use-after-free found with HWASan).
- **Input**: multi-touch on the button pad, keys released on cancelled gestures
  and layout changes, no opposing d-pad directions, and
  `requestUnbufferedDispatch` on touchscreen `ACTION_DOWN` for lower latency.
  Zero allocations on the touch path.
- **ROM handling**: icons loaded off the directory-scan thread, a 7z memory limit
  sized on the Java heap with one decoder at a time, and file names resolved for
  non-document URIs.
- **Video**: LCD and scanline filters anchored to the native DS grid.
- **Emulator bridge**: guarded instance access, serialised state operations, an
  atomic event pipe, checked save-state results, camera handler cleanup on stop.

### Upstream pull requests integrated

Five open PRs by other contributors were applied by hand and adapted, each with
corrections (details in commit `138baa2b` on the source branch):

- #1608 hold to fast forward (WailAbou) — held state reset on pause/focus loss
- #1606 combo hotkeys (WailAbou)
- #1625 mute while fast forwarding (dysprosium) — flags are atomics read without
  the audio mutex
- #1648 ROM not found from external apps (N3MI-DG) — matched by SAF document id,
  launch URI permission persisted
- #1589 sleep/wake mapped to the DS lid (DLCSharp) — lid reopened on resume, text
  moved to resources

Not taken: #1657 (too invasive), #1666 (rejected, only its three side fixes),
#1664 (superseded).

### Testing

~114 JVM/Robolectric unit tests plus four C++ host suites under ASan/UBSan.
Built and run on the device as `2.1.3` (versionCode 44).

## Commits

```
3ba3f502 build: string, array and test resources for the changes that follow
a7644498 fix(core-bridge): guard the emulator instance and make the event pipe safe
cf1d81a3 fix(save): never truncate a save file before the new data is written
09f4b7b7 fix(audio): serialise stream state changes and reopen the stream on resume
b5de523f fix(input): multi-touch, stuck keys and lower touchscreen latency
80c00944 feat(rtc): optional clock sync with the device, off by default
4e0d850c fix(roms): archive handling, icon loading and file name resolution
db986311 feat(bios): guided BIOS setup and clearer firmware diagnostics
b942e9fd fix(video): keep the LCD and scanline filters on the native DS grid
0b5810e1 feat(wfc): manage the online connection slots from the settings
e945b3f7 feat(cheats): SGP wild encounter picker with an in-game toggle
468a73b8 feat(settings): back up and restore the settings
dcc6789c fix(app): lid handling, pause menu and lifecycle gaps
6c6f79dd build: unit test dependencies and Robolectric test options
```

---

## Note per la revisione (NON fa parte del corpo della PR)

### Come è stato costruito il ramo
Non è un rebase della storia di `ventuno`. La storia originale non si riapplica:
l'upstream ha riscritto `MultiButtonInputHandler.kt` il 12/09 e il cherry-pick
confligge a catena. Il ramo è ricostruito **partendo dallo stato finale di
`ventuno`** (quello installato e collaudato sulla Thor) e spezzato in commit
tematici per file. Verifica fatta: `git diff pr-prep/app ventuno` restituisce
solo i file volutamente esclusi (sotto).

Conseguenze da tenere a mente:
- I singoli commit **non sono compilabili in isolamento** (partizione per file,
  non per hunk). Niente `git bisect` su questo ramo.
- Un file toccato da più aree finisce tutto nel commit della sua area primaria:
  qualche commit contiene un po' più di quanto dice il titolo.
- Gli autori dei commit originali (`MELONDS-INTEGRA <melonds-integra@local>`) non
  compaiono: i 14 commit sono firmati con l'identità git corrente.

### Volutamente esclusi dal ramo
- `.github/workflows/*` — modifiche CI specifiche del fork
- `README.md` — riferimenti al fork
- `buildSrc/.../AppConfig.kt` — versionCode 44 / versionName 2.1.3, il versioning
  è dell'upstream
- `melonDS-android-lib` — puntatore submodule, da bumpare solo dopo il merge del
  motore
- `docs/ventuno/**` e `tools/**` — documentazione interna, host test runner,
  sanitizer script, wrap.sh HWASan
- da `app/build.gradle.kts`: il build type `profiling` e l'opzione
  `-PnativeSanitize`. Del file resta solo `testOptions` + le dipendenze di test.

### Decisioni aperte prima di aprire la PR
1. **Spezzare la PR.** 147 file in un colpo solo è tanto da rivedere. Ordine
   suggerito, dal meno controverso: save → audio → input → roms → video →
   core-bridge, poi le feature (bios, wfc, settings backup, rtc) separatamente.
   I 14 commit sono già i pezzi.
2. **`feat(cheats)` SGP picker: probabilmente da togliere.** È il supporto a una
   singola ROM hack (Sacred Gold Plus), 20 file. Difficile che l'upstream lo
   voglia in-tree. Va tolto per primo se si vuole una PR snella.
3. **PR di terzi integrate.** Il corpo le attribuisce, ma l'upstream potrebbe
   preferire che quelle PR vengano merge-ate dai loro autori. Da valutare se
   rimuoverle dal ramo e lasciarle ai rispettivi PR.
4. **Attribuzione.** I commit originali hanno `Co-Authored-By: Claude`. Decidere
   se mantenerlo nei commit ricostruiti (ora non c'è).

### Sequenza per aprire le PR quando sarà il momento
1. Aprire la PR del motore (`pr-prep/core`, vedi `PR-DRAFT-CORE.md`) e aspettare
   il merge.
2. Nel submodule: `git fetch origin && git checkout <nuovo master>`.
3. Su `pr-prep/app`: commit del nuovo puntatore submodule + verifica build
   (`sh ./gradlew :app:testGitHubNightlyDebugUnitTest`, poi `assembleGitHubNightlyDebug`).
4. `git fetch upstream` e merge/rebase se `upstream/master` è avanzato.
5. Push su `origin` come ramo nuovo, PR verso `rafaelvcaetano:master`.
