# A DSiWare save size can trap NAND import in a nonterminating loop

[Security index](../README.md) · [Disposition register](../FINDINGS.csv)

**Canonical disposition:** `CORRECTED`.

- **Identity and status:** `occ_6fc7ac8a4bb1383c4b1bb230` · scan `4c41e000-6ba0-4453-aaa7-3cdbec542687` · severity `medium` · layer `core`. Technical disposition from CSV: CORRECTED. The Security UI state may remain **Open**; it is separate from the technical disposition.
- **Summary:** DSiWare import reads public/private save sizes from the attacker-supplied application header and passes them to 'CreateSaveFile()'. The permitted value '0x01FFFE00' equals 65,535 512-byte sectors. Both 'maxsectors' and the evolving 'totsec16next' are 16-bit integers, so the loop condition 'totsec16next &lt;= maxsectors' is permanently true when 'maxsectors == UINT16_MAX', hanging the in-process Android NAND import.
- **Attack path:** DSiWare header -&gt; save size -&gt; u16 sector maximum -&gt; nonterminating geometry loop
- **Evidence and proof level:** **static** in the sealed finding (`validated`): Static exact-value integer proof and Android import reachability trace; Accepted maximum-sector value makes the u16 loop tautological; For 'len=0x01FFFE00', 'maxsectors' is 65,535. Every possible u16 'totsec16next' is &lt;=65,535, so wrapping cannot exit the loop. Disposition evidence: **recorded dynamic post-fix checks**; pre-fix evidence remains static unless the report records an explicit RED — 65,535 terminates; 65,536/sub-sector fail. No dynamic RED is inferred unless the report records it explicitly.
- **Counterevidence:** TMD category/title identity checks protect destination identity but not save geometry. The Kotlin mutex serializes imports but cannot cancel the native loop.
- **Limitations:** No runtime hang was executed. No NAND corruption beyond the hang was demonstrated.
- **Recommended correction:** Use 'u32'/'u64' for sector and geometry calculations, reject lengths whose derived geometry cannot be represented by the FAT fields, use checked multiplication, and impose a strict iteration bound with a failure return.
- **Disposition, patch and before/after:** Compute DSi save geometry in wide types and reject zero progress/unrepresentable sectors **Before:** 65,536-sector geometry wrapped and could loop **After:** 65,535 terminates; 65,536/sub-sector fail Recorded integration pair: [app a77ccaaa](https://github.com/Umberto-DEV/melonDS-android/commit/a77ccaaac5ca99f4ac1893b1dfb9b41326f75811); [core 65952aec](https://github.com/Umberto-DEV/melonDS-android-lib/commit/65952aec868bedd2dc33151ed850804e3e13df05).
- **Source and evidence:** [melonDS-android-lib/src/DSi_NAND.cpp:968 @ 907849b3](https://github.com/Umberto-DEV/melonDS-android-lib/blob/907849b3544bb805b29644356d3386c670ad9aae/src/DSi_NAND.cpp#L968); [Canonical CSV](../FINDINGS.csv); [First-campaign report](../FIRST-CAMPAIGN.md).
