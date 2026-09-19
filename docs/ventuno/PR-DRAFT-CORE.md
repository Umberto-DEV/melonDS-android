# PR draft — engine (melonDS-android-lib)

**Status: draft, not submitted.** Prepared 19 Sep 2026 so a future PR only needs
reviewing and pushing.

- Branch: `pr-prep/core` in the `melonDS-android-lib` submodule
- Base: `origin/master` (rafaelvcaetano/melonDS-android-lib), fast-forward, no conflicts
- 7 commits, 14 files, +322/-169
- Must be merged **before** the app PR: the app branch depends on the DNS
  override and SCFG work below.

---

## Title

Core fixes: DSi register screening, Wi-Fi power saving, JIT literal guard, 3D renderer waste, optional DNS overrides

## Body

Seven independent changes, each one testable on its own. They come out of
several months of running this core on an AYN Thor (Adreno 740, Android 13).

**`fix(dsi)` — DS carts no longer detect a DSi.** The SCFG registers were
readable in DS mode, so some cartridges took a DSi code path on a plain DS.
They are now screened by console mode.

**`fix(wifi)` — the transceiver only auto powers down in automatic power saving
mode**, as the hardware does, instead of whenever the game left it idle.

**`fix(jit)` — do not fold a literal whose address is not tracked for
invalidation.** A literal load could be constant-folded from memory that the JIT
was not watching, so a later write left stale code behind. Includes the
follow-up that skips untracked literals and removes a "cache rejected" log line
that fired on healthy runs.

**`perf(gpu3d)` — two pieces of work nothing consumed:** an edge index list that
was built and uploaded every frame but never read, and a full compute renderer
rebuild triggered even when no setting had actually changed.

**`feat(net)` — optional host name overrides for the built-in DNS responder.**
Lets a frontend point specific host names at a chosen resolver without patching
firmware. Off unless a table is supplied. Covered by synthetic-packet tests
(6/6).

## Commits

```
c552b389 fix(dsi): screen the SCFG registers so DS carts stop detecting a DSi
53aff8bc fix(wifi): only auto power down the transceiver in automatic power saving mode
b876d3b3 perf(gpu3d): skip the compute renderer rebuild when the settings did not change
769d61de perf(gpu3d): stop building and uploading an edge index list nothing reads
3ad4e940 fix(jit): do not fold a literal whose address is not tracked for invalidation
137c4ba7 fix(jit,gl): skip untracked literals, drop the unused edge shaders and stop the false "cache rejected" log
bc060f4c feat(net): optional host name overrides for the built-in DNS responder
```

---

## Note per la revisione (NON fa parte del corpo della PR)

- I due commit JIT (`3ad4e940` + `137c4ba7`) sono uno il fixup dell'altro:
  valutare uno squash prima di aprire la PR. `137c4ba7` mescola anche la
  rimozione degli shader Edge (GL) con il log della cache — se si vuole essere
  scrupolosi, andrebbe spezzato in due.
- Questi 7 commit erano già stati proposti come lib#16 (ritirata il 19/09).
  Rispetto ad allora ci sono in più `769d61de`, `137c4ba7` e `bc060f4c`.
- Prima di aprire: `git fetch origin && git rebase origin/master` dentro il
  submodule (ad oggi è già fast-forward, 0 indietro).
- La PR va aperta dal fork `Umberto-DEV/melonDS` verso
  `rafaelvcaetano/melonDS-android-lib`. Non rinominare mai il ramo head di una
  PR aperta: la chiude.
