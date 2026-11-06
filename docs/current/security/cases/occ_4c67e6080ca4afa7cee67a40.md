# Savestates restore unchecked cartridge transfer indices used in host memory writes

[Security index](../README.md) · [Disposition register](../FINDINGS.csv)

**Canonical disposition:** `CORRECTED`.

- **Identity and status:** `occ_4c67e6080ca4afa7cee67a40` · scan `4c41e000-6ba0-4453-aaa7-3cdbec542687` · severity `high` · layer `core`. Technical disposition from CSV: CORRECTED. The Security UI state may remain **Open**; it is separate from the technical disposition.
- **Summary:** NDSCartSlot::DoSavestate restores TransferPos, TransferLen, TransferDir, and ROMCnt without validation. A resumed guest write can then store a u32 at TransferData[TransferPos] using only TransferPos &lt; TransferLen, even when TransferPos is beyond the fixed 0x4000-byte buffer.
- **Attack path:** A crafted savestate restores an active outbound cart transfer with an out-of-range position; resumed guest code writes the cartridge data register; native code writes the guest value beyond TransferData.
- **Evidence and proof level:** **static** in the sealed finding (`validated`): Manual source-to-sink review against the Android build and JNI call path.; Transfer state restored verbatim; No capacity, direction, or consistency validation follows restore. Disposition evidence: **recorded dynamic post-fix checks**; pre-fix evidence remains static unless the report records an explicit RED — last-word control passes; invalid direction/position/length fail. No dynamic RED is inferred unless the report records it explicitly.
- **Counterevidence:** The crafted state must remain compatible enough to resume guest execution. Cart type/checksum checks do not validate the transfer fields.
- **Limitations:** No separate item was recorded in the sealed finding.
- **Recommended correction:** Validate transfer direction, position, length, alignment, and ROMCnt consistency before committing restored state. At every read/write sink independently require TransferPos &lt;= TransferData.size()-sizeof(u32).
- **Disposition, patch and before/after:** Restore cartridge transfer fields into temporaries and commit only valid aligned spans **Before:** serialized positions reached fixed transfer arrays **After:** last-word control passes; invalid direction/position/length fail Recorded integration pair: [app a77ccaaa](https://github.com/Umberto-DEV/melonDS-android/commit/a77ccaaac5ca99f4ac1893b1dfb9b41326f75811); [core 65952aec](https://github.com/Umberto-DEV/melonDS-android-lib/commit/65952aec868bedd2dc33151ed850804e3e13df05).
- **Source and evidence:** [melonDS-android-lib/src/NDSCart.cpp:1480 @ 907849b3](https://github.com/Umberto-DEV/melonDS-android-lib/blob/907849b3544bb805b29644356d3386c670ad9aae/src/NDSCart.cpp#L1480); [Canonical CSV](../FINDINGS.csv); [First-campaign report](../FIRST-CAMPAIGN.md).
