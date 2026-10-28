# What's new compared to the official nightly

Plain-language summary of everything the `ventuno` branch adds on top of
rafaelvcaetano/melonDS-android `master` (upstream commit c42995ca, 12 Sep 2026).
Written for someone who just plays games, not for developers.
Technical notes live in `STATO-VENTUNO.md`; this file is the one to paste into a
release note or a pull request.

## Main features

**Guided BIOS setup.** Instead of a folder picker and a cryptic error, the app
walks you through picking your BIOS and firmware files, tells you exactly which
file is missing, wrong-sized or from the wrong console, and copies everything
into place for you.

**Online settings made manageable.** A new "Online connections" screen lets you
configure three WFC slots and their DNS addresses without editing firmware by
hand. It supports generated DS settings and compatible imported DS/DSi firmware.
The pause menu can enable or disable the slots; DNS editing happens in Settings
with the game closed. Service availability and game support depend on the server.

**Back up and restore your settings.** One button saves your whole configuration
to a file, another restores it — useful before reinstalling or moving to a new
device.

**Optional clock sync.** The console clock can follow your device clock, which
fixes games that think no time has passed. Off by default, and it never moves
the clock backwards.

**Hold to fast forward, and combo hotkeys.** Fast forward can now work only while
you hold the button, and a hotkey can be assigned to two buttons pressed
together. Audio can optionally mute while fast forwarding.

**Putting the device to sleep closes the DS lid.** Turning the screen off now
behaves like closing a real DS, so games play their sleep jingle instead of being
cut off.

**Pick your wild Pokémon from a list.** When the cheat database you imported
has "wild Pokémon" codes for a game — HeartGold, SoulSilver, Diamond, Pearl,
Platinum, Black, White, Black 2, White 2, Sacred Gold Plus — the cheat list
shows one entry with a search box instead of hundreds of near-identical
codes: pick the Pokémon (and the level, where the game allows it), done.
Starters, natures and level lists collapse the same way, and turning one on
turns the conflicting ones off for you. On Sacred Gold Plus the in-game L+R
toggle keeps working exactly as before.

## Important fixes

**Safer save writes and visible errors.** Save writes prefer opening the existing
file without emptying it first, and failed writes are reported and retried.
Some storage providers require a truncating fallback. This reduces data-loss
risk; writes are not atomic replacements and interruption can still corrupt a save.

**Sound comes back.** Audio no longer stays silent after you leave the app and
return, and a crash when the audio device failed to open is gone.

**Better input handling.** Interrupted gestures and layout changes release held
touch buttons, and two fingers can press the button pad independently. Touch input
requests unbuffered dispatch; no general latency improvement is claimed without
device measurements. The October follow-up also clears controller combinations
and axes after focus loss.

**The game list loads more smoothly.** Icons load independently of directory
scanning. 7z decoding is serialized and memory-bounded, with failure handling for
archives that exceed the limit; oversized archives may need to be extracted first.

**Better launches from other apps.** Document-ID matching and retained URI
permissions resolve supported external-launch cases. Raw file paths can still be
inaccessible under Android scoped storage.

**Cheat database import reads every entry correctly.** The XML importer could
read the wrong slice of the parser's buffer; it never showed on Android but
was wrong all the same.

**The LCD and scanline filters look right** — they now line up with the console's
real pixel grid instead of drifting with the window size.

## Smaller fixes, grouped

Crash and stability work throughout the emulator bridge (save-state failures are
now reported instead of silently ignored, the camera is released on exit, and
several race conditions between the UI and the emulator thread are gone); the
console no longer mistakenly reports itself as a DSi to DS cartridges; Wi-Fi
power saving only kicks in when the game asks for it; small speed-ups in the 3D
renderer and at app startup; a stale graphics-cache warning removed; and a
sizeable set of automated tests added around saves, input, BIOS handling and the
online settings.
