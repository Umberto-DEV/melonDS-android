# Guest-controlled Wi-Fi TX descriptors overwrite native state and trigger an oversized copy

[Security index](../README.md) · [Disposition register](../FINDINGS.csv)

**Canonical disposition:** `CORRECTED`.

- **Identity and status:** `occ_2bba18cf5ac157af5fc41bcb` · scan `4c41e000-6ba0-4453-aaa7-3cdbec542687` · severity `high` · layer `core`. Technical disposition from CSV: CORRECTED. The Security UI state may remain **Open**; it is separate from the technical disposition.
- **Summary:** A malicious DS title controls Wi-Fi TX slot addresses, descriptor lengths, RAM and transmit state. Descriptor-relative reads and sequence/WEP/status writes occur before complete range validation. A concrete WEP path writes four bytes beyond the 0x2000-byte Wi-Fi RAM onto the native TXSlots array, corrupts the active slot address, then derives a negative signed length that is converted to a massive memcpy size.
- **Attack path:** A malicious ROM programs Wi-Fi RAM/MMIO to start an invalid TX descriptor.
- **Evidence and proof level:** **static** in the sealed finding (`status not specified`): Independent native source validation with concrete boundary arithmetic; Unchecked slot descriptor; Addr can be 0x1ffe, while descriptor fields require bytes beyond the 0x2000-byte RAM. Disposition evidence: **recorded dynamic post-fix checks**; pre-fix evidence remains static unless the report records an explicit RED — exact-boundary and one-byte-overflow tests pass. No dynamic RED is inferred unless the report records it explicitly.
- **Counterevidence:** The payload memcpy is later clamped. Write value and target range are constrained.
- **Limitations:** No sanitizer reproduction or exploitability proof was performed. No ASan/device proof of concept was run in this read-only audit.
- **Recommended correction:** Copy TXSlot::Addr and TXSlot::Length into immutable unsigned locals, then reject the transmission unless the full descriptor, frame, sequence/status fields and WEP FCS span fit inside RAM using checked arithmetic. Use bounded RAM accessors, never re-read slot state after a memory write, and never derive an unsigned copy size from a negative signed value.
- **Disposition, patch and before/after:** Check TX descriptor, frame, sequence and WEP spans before every access/copy **Before:** guest descriptor could cross Wi-Fi RAM/LAN buffer **After:** exact-boundary and one-byte-overflow tests pass Recorded integration pair: [app a77ccaaa](https://github.com/Umberto-DEV/melonDS-android/commit/a77ccaaac5ca99f4ac1893b1dfb9b41326f75811); [core 65952aec](https://github.com/Umberto-DEV/melonDS-android-lib/commit/65952aec868bedd2dc33151ed850804e3e13df05).
- **Source and evidence:** [melonDS-android-lib/src/Wifi.h:176 @ 907849b3](https://github.com/Umberto-DEV/melonDS-android-lib/blob/907849b3544bb805b29644356d3386c670ad9aae/src/Wifi.h#L176); [Canonical CSV](../FINDINGS.csv); [First-campaign report](../FIRST-CAMPAIGN.md).
