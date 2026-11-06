# Malformed savestate section metadata causes infinite loops and native overreads

[Security index](../README.md) · [Disposition register](../FINDINGS.csv)

**Canonical disposition:** `CORRECTED`.

- **Identity and status:** `occ_2be586dc4a21fbc0f60e65fd` · scan `4c41e000-6ba0-4453-aaa7-3cdbec542687` · severity `medium` · layer `core`. Technical disposition from CSV: CORRECTED. The Security UI state may remain **Open**; it is separate from the technical disposition.
- **Summary:** Savestate::FindSection scans attacker-controlled section headers without first requiring a complete 16-byte header, a minimum positive section length, a bounded remaining span, or cursor progress.
- **Attack path:** savestate file -&gt; exact vector -&gt; Savestate constructor -&gt; RCHV Section lookup -&gt; unchecked FindSection loop
- **Evidence and proof level:** **static** in the sealed finding (`validated`): Semantic reconciliation of already-validated Standard findings; no repository inspection or runtime execution.; Independent offline static source trace and minimal-file construction; Section loop accepts nonprogress; section_length=0 leaves offset unchanged forever. Disposition evidence: **recorded dynamic post-fix checks**; pre-fix evidence remains static unless the report records an explicit RED — 34 section boundary controls pass. No dynamic RED is inferred unless the report records it explicitly.
- **Counterevidence:** Global magic/version/total-length checks exist, but they do not validate internal section structure. VarArray is bounds checked, but FindSection uses direct memcpy.
- **Limitations:** No hang payload was executed. No disclosure channel or runtime reproduction was established.
- **Recommended correction:** Before reading each section header, require at least 16 remaining bytes. Reject section_length &lt; 16, section_length &gt; buffer_length - offset, overflow, truncation, and any next offset not greater than the current offset.
- **Disposition, patch and before/after:** Require a complete 16-byte section, forward progress and section-local reads **Before:** zero-length and cross-section test failed/timed out before fix **After:** 34 section boundary controls pass Recorded integration pair: [app a77ccaaa](https://github.com/Umberto-DEV/melonDS-android/commit/a77ccaaac5ca99f4ac1893b1dfb9b41326f75811); [core 65952aec](https://github.com/Umberto-DEV/melonDS-android-lib/commit/65952aec868bedd2dc33151ed850804e3e13df05).
- **Source and evidence:** [melonDS-android-lib/src/Savestate.cpp:345 @ 907849b3](https://github.com/Umberto-DEV/melonDS-android-lib/blob/907849b3544bb805b29644356d3386c670ad9aae/src/Savestate.cpp#L345); [Canonical CSV](../FINDINGS.csv); [First-campaign report](../FIRST-CAMPAIGN.md).
