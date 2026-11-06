# Guest-corrupted R4 FAT geometry causes native division by zero

[Security index](../README.md) · [Disposition register](../FINDINGS.csv)

**Canonical disposition:** `NOT_APPLICABLE_ANDROID`.

- **Identity and status:** `occ_5aa0cdd63e52e8bf6e5c5f58` · scan `ab26b107-97dd-4557-9c4b-2942e5df879d` · severity `medium` · layer `core`. Not applicable to the documented Android target: the required runtime precondition is not connected; no desktop correction is counted. The Security UI state may remain **Open**; it is separate from the technical disposition.
- **Summary:** R4 command 0xBB lets the guest overwrite sector zero on a writable SD image. A later save-read path parses bytesPerSector and sectorsPerCluster from that sector without validation and uses either zero value as a divisor.
- **Attack path:** R4 guest writes zero geometry into SD sector 0 -&gt; B2 save-read invokes SDFATEntrySectorGet -&gt; division by zero in native code.
- **Evidence and proof level:** **static** in the sealed finding (`validated`): Manual guest-write-to-parser validation.; Guest write can replace the boot sector; A zero aligned sector passes the compatibility check and persists guest bytes. Disposition evidence: **static**, from Android caller review — Android construction/caller review; no Android runtime claim beyond that scope. No dynamic RED is inferred unless the report records it explicitly.
- **Counterevidence:** Normally formatted images have valid geometry; the guest can invalidate that assumption through the emulated writable card. A read-only SD image prevents the source write.
- **Limitations:** SIGFPE behavior was not dynamically observed on a device.
- **Recommended correction:** Validate the FAT signature and require supported nonzero power-of-two sector and cluster sizes before arithmetic. Bound all products and cluster traversal to media capacity.
- **Disposition, patch and before/after:** [R4-FAT-ANDROID-NOT-APPLICABLE] Android constructs the core without SD storage; no reachable Android division path was found **Before:** Core-only hypothesis required an SD-backed R4 path **After:** Android construction/caller review; no Android runtime claim beyond that scope Recorded integration pair: [app 0018cddb](https://github.com/Umberto-DEV/melonDS-android/commit/0018cddb04278f2bca5054804de501a459eb1de9); [core ac5e6ba0](https://github.com/Umberto-DEV/melonDS-android-lib/commit/ac5e6ba0fb795b7aa8a43fdefed62eed8a37eff8).
- **Source and evidence:** [melonDS-android-lib/src/NDSCartR4.cpp:168 @ 65952aec](https://github.com/Umberto-DEV/melonDS-android-lib/blob/65952aec868bedd2dc33151ed850804e3e13df05/src/NDSCartR4.cpp#L168); [Canonical CSV](../FINDINGS.csv); [Second-campaign report](../SECOND-CAMPAIGN.md).
