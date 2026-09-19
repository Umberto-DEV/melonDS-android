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

**Online play made manageable.** A new "Online connections" screen lets you keep
three separate online setups (for example Mystery Gift, GTS and battles) and
switch between them without editing firmware by hand. It works with every
firmware and can be changed from the pause menu while a game is running.

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

**Saved games can no longer be corrupted.** The old code emptied the save file
before writing the new data, so an interruption at the wrong moment lost
everything. Now the file is only replaced once the new data is safely written,
and you are told if a write fails.

**Sound comes back.** Audio no longer stays silent after you leave the app and
return, and a crash when the audio device failed to open is gone.

**Buttons behave.** Buttons no longer get stuck pressed when a gesture is
interrupted or the screen rotates, two fingers on two buttons finally work, and
touches register noticeably faster.

**The game list loads properly.** Cover icons now appear right away instead of
waiting for the whole folder scan, and large 7z archives no longer run the app
out of memory.

**Games launched from other apps open correctly** instead of reporting the ROM as
missing.

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
