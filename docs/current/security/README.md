# Security findings and evidence

This is the current source's evidence index, retaining the original scan and
integration identities. Each occurrence has its own dossier with problem,
reachable path, severity, correction, before/after evidence, counterevidence
and limits. The [CSV](FINDINGS.csv) remains the canonical disposition register.
A scan occurrence, an independently corrected cause and a closed upstream
issue are different counts.

| Campaign | Scan ID | Scanned app / core | Findings SHA-256 | Occurrences |
| --- | --- | --- | --- | ---: |
| [First](FIRST-CAMPAIGN.md) | `4c41e000-6ba0-4453-aaa7-3cdbec542687` | `6a9d7a1f19cfd44ba7fccdedec9f9196d3e04108` / `907849b3544bb805b29644356d3386c670ad9aae` | `3d5ff07c92d7e8d93a1d8f19b553b8d07b8e76ab5330a843a8a08c0c66dc7b2d` | 35 |
| [Second](SECOND-CAMPAIGN.md) | `ab26b107-97dd-4557-9c4b-2942e5df879d` | `9929f530df6f60b86d50c324f4aec83747e5745b` / `65952aec868bedd2dc33151ed850804e3e13df05` | `3e009c6f8476a350b13086ccd21f6c0905dad980432b0a69f6a4b67f29e22430` | 66 |

The first campaign records 35 corrected occurrences. The second records 62
repository-code corrections, two repository corrections with external
deployment controls unverified, one Android-non-applicable case and one
informational reconciliation. Its 66 occurrences normalize to 48 groups:
46 correctable groups, one non-applicable prerequisite and one informational
record. These do not recount the first 35. The union contains 29 high,
50 medium, 21 low and one informational occurrence.

Static source analysis does not establish dynamic reproduction. A post-fix
suite pass does not establish an individual pre-fix RED or per-trigger Android
execution. The cards retain those distinctions. Security-interface workflow
state is separate from local technical disposition; no service closure is
asserted. In particular, external production environments/reviewer controls
and tag protection remain unverified for `occ_7fddae3d4a0847da222da76c` and
`occ_2d9284d05331b527dffd90b6`.

The recorded corrected pair for the second campaign is app
`0018cddb04278f2bca5054804de501a459eb1de9` / core
`ac5e6ba0fb795b7aa8a43fdefed62eed8a37eff8`. Later release identities and
execution belong to the [current release](../RELEASE.md) and
[post-release smoke](../POST-RELEASE-SMOKE.md). The [adversarial review](ADVERSARIAL-REVIEW.md)
retains incomplete-fix controls; [scan provenance](SCAN-PROVENANCE.md) distinguishes
canceled historical scans from these two completed campaigns.

| Occurrence | Severity | Layer | Dossier |
| --- | --- | --- | --- |
| <a id="occ_bf9ea27628798833dda7cf7b"></a>`occ_bf9ea27628798833dda7cf7b` | high | core | [Malformed cartridge images cause native out-of-bounds reads and secure-area writes](cases/occ_bf9ea27628798833dda7cf7b.md) |
| <a id="occ_2be586dc4a21fbc0f60e65fd"></a>`occ_2be586dc4a21fbc0f60e65fd` | medium | core | [Malformed savestate section metadata causes infinite loops and native overreads](cases/occ_2be586dc4a21fbc0f60e65fd.md) |
| <a id="occ_e9fbed34ba4f36c73dc9449a"></a>`occ_e9fbed34ba4f36c73dc9449a` | medium | app | [Android RCHV loader discards the real buffer length before native deserialization](cases/occ_e9fbed34ba4f36c73dc9449a.md) |
| <a id="occ_4c67e6080ca4afa7cee67a40"></a>`occ_4c67e6080ca4afa7cee67a40` | high | core | [Savestates restore unchecked cartridge transfer indices used in host memory writes](cases/occ_4c67e6080ca4afa7cee67a40.md) |
| <a id="occ_2993485bc81b37540bda66df"></a>`occ_2993485bc81b37540bda66df` | high | core | [Crafted savestates control native callback, FIFO, audio-buffer, and table indices](cases/occ_2993485bc81b37540bda66df.md) |
| <a id="occ_6b9635717a6a6448125d3010"></a>`occ_6b9635717a6a6448125d3010` | medium | core | [Guest DNS parsing can read beyond the frame and return stale native bytes](cases/occ_6b9635717a6a6448125d3010.md) |
| <a id="occ_9423bc5a581f219396ba5434"></a>`occ_9423bc5a581f219396ba5434` | medium | app | [Launching a compressed ROM can exceed the configured cache limit and exhaust storage](cases/occ_9423bc5a581f219396ba5434.md) |
| <a id="occ_46d778442f1d34d3992b75b7"></a>`occ_46d778442f1d34d3992b75b7` | medium | core | [Running a ROM can proxy traffic to Android localhost and private-network services](cases/occ_46d778442f1d34d3992b75b7.md) |
| <a id="occ_7912862fc50149068674e372"></a>`occ_7912862fc50149068674e372` | medium | app | [DSiWare metadata is imported from cleartext HTTP without authenticity verification](cases/occ_7912862fc50149068674e372.md) |
| <a id="occ_8af6b491512413f1e03d2bb8"></a>`occ_8af6b491512413f1e03d2bb8` | medium | core | [Guest DNS queries leak native resolver allocations](cases/occ_8af6b491512413f1e03d2bb8.md) |
| <a id="occ_9fbf5f7454cdc1a7a98c3959"></a>`occ_9fbf5f7454cdc1a7a98c3959` | medium | cross | [DSi title listing reads fixed-width banner text as an unbounded string](cases/occ_9fbf5f7454cdc1a7a98c3959.md) |
| <a id="occ_a58108db986cc45682121731"></a>`occ_a58108db986cc45682121731` | low | app | [A malformed external firmware-launch intent crashes the Android emulator](cases/occ_a58108db986cc45682121731.md) |
| <a id="occ_2bba18cf5ac157af5fc41bcb"></a>`occ_2bba18cf5ac157af5fc41bcb` | high | core | [Guest-controlled Wi-Fi TX descriptors overwrite native state and trigger an oversized copy](cases/occ_2bba18cf5ac157af5fc41bcb.md) |
| <a id="occ_0ead515f26677112d1f828d6"></a>`occ_0ead515f26677112d1f828d6` | high | app | [Untrusted serialized ROM configurations expose or overwrite files using melonDS authority](cases/occ_0ead515f26677112d1f828d6.md) |
| <a id="occ_d540d679331dce207e3adb60"></a>`occ_d540d679331dce207e3adb60` | low | app | [Null PATH extra crashes the exported singleTask launch path](cases/occ_d540d679331dce207e3adb60.md) |
| <a id="occ_6fc7ac8a4bb1383c4b1bb230"></a>`occ_6fc7ac8a4bb1383c4b1bb230` | medium | core | [A DSiWare save size can trap NAND import in a nonterminating loop](cases/occ_6fc7ac8a4bb1383c4b1bb230.md) |
| <a id="occ_bc26e0f9273443d666f51fd1"></a>`occ_bc26e0f9273443d666f51fd1` | medium | app | [A tiny cheat database can request a near-2 GiB allocation](cases/occ_bc26e0f9273443d666f51fd1.md) |
| <a id="occ_1211210d24906e23b17d51f2"></a>`occ_1211210d24906e23b17d51f2` | low | app | [A malformed settings backup can prevent every later app launch](cases/occ_1211210d24906e23b17d51f2.md) |
| <a id="occ_4e4229a143d29c8d477e6cd6"></a>`occ_4e4229a143d29c8d477e6cd6` | low | app | [Cheat XML import does not bound internal entity expansion or parsed text](cases/occ_4e4229a143d29c8d477e6cd6.md) |
| <a id="occ_c7156b64208b9c244aee3dec"></a>`occ_c7156b64208b9c244aee3dec` | medium | core | [GPU3D savestate restoration trusts array indices and counts before native writes](cases/occ_c7156b64208b9c244aee3dec.md) |
| <a id="occ_dec38863aa79dfd2f944a501"></a>`occ_dec38863aa79dfd2f944a501` | low | app | [Privileged release workflows execute dependencies without immutable integrity pins](cases/occ_dec38863aa79dfd2f944a501.md) |
| <a id="occ_6cb1c55ef7aff5b4986102aa"></a>`occ_6cb1c55ef7aff5b4986102aa` | low | app | [RetroAchievements bearer token is included in Android platform backup](cases/occ_6cb1c55ef7aff5b4986102aa.md) |
| <a id="occ_f39613af9fae3c25d83a0055"></a>`occ_f39613af9fae3c25d83a0055` | medium | app | [Signing in can expose RetroAchievements passwords and tokens through URL logs](cases/occ_f39613af9fae3c25d83a0055.md) |
| <a id="occ_4ab7c23e479f23f205e15b1f"></a>`occ_4ab7c23e479f23f205e15b1f` | medium | app | [Savestate file length drives an uncapped native allocation before validation](cases/occ_4ab7c23e479f23f205e15b1f.md) |
| <a id="occ_a558a5b137d2433c0269cf8e"></a>`occ_a558a5b137d2433c0269cf8e` | high | core | [A crafted custom firmware image can make checksum updates write outside its native buffer](cases/occ_a558a5b137d2433c0269cf8e.md) |
| <a id="occ_3b5415742d7ddbfad77786f5"></a>`occ_3b5415742d7ddbfad77786f5` | high | cross | [A malicious document provider can turn firmware revalidation drift into a controlled heap overwrite](cases/occ_3b5415742d7ddbfad77786f5.md) |
| <a id="occ_d861c760cdbcae81657434c1"></a>`occ_d861c760cdbcae81657434c1` | medium | app | [A direct ROM URI can force a one-gigabyte native allocation](cases/occ_d861c760cdbcae81657434c1.md) |
| <a id="occ_f0001eeaf73d4f180cc706e6"></a>`occ_f0001eeaf73d4f180cc706e6` | medium | cross | [A truncated DSiWare file can create an invisible, non-reimportable NAND title](cases/occ_f0001eeaf73d4f180cc706e6.md) |
| <a id="occ_0f5f2f5c076ab2aa0094b993"></a>`occ_0f5f2f5c076ab2aa0094b993` | high | core | [GBA flash commands read and overwrite host heap beyond save RAM](cases/occ_0f5f2f5c076ab2aa0094b993.md) |
| <a id="occ_199e211e1856d6a8fcbf97fa"></a>`occ_199e211e1856d6a8fcbf97fa` | high | core | [Guest-writable firmware metadata reaches unchecked host buffer copies](cases/occ_199e211e1856d6a8fcbf97fa.md) |
| <a id="occ_9e7a80234cd3366f50ce8d90"></a>`occ_9e7a80234cd3366f50ce8d90` | high | core | [DSi DSP HLE pipe descriptors escape mapped host memory](cases/occ_9e7a80234cd3366f50ce8d90.md) |
| <a id="occ_595441428ec94336e7c4ebf7"></a>`occ_595441428ec94336e7c4ebf7` | high | core | [Zero-width DSi graphics command underflows a native transfer length](cases/occ_595441428ec94336e7c4ebf7.md) |
| <a id="occ_71e386607ebf94f39d022a9f"></a>`occ_71e386607ebf94f39d022a9f` | medium | app | [A 32-bit URI hash lets one compressed ROM substitute another cached ROM](cases/occ_71e386607ebf94f39d022a9f.md) |
| <a id="occ_a134bcef99d03aae124925a6"></a>`occ_a134bcef99d03aae124925a6` | high | core | [Guest-controlled B7 transfers overread ROM memory and zero-length commands underflow into massive copies](cases/occ_a134bcef99d03aae124925a6.md) |
| <a id="occ_c9523ca984164c679e7340bb"></a>`occ_c9523ca984164c679e7340bb` | medium | core | [Savestate SRAM lengths drive unbounded cartridge allocations](cases/occ_c9523ca984164c679e7340bb.md) |
| <a id="occ_09abfa72b6bd218ba5427fa8"></a>`occ_09abfa72b6bd218ba5427fa8` | high | core | [Running a crafted DSi ROM can read beyond the BPTWL register array into host memory](cases/occ_09abfa72b6bd218ba5427fa8.md) |
| <a id="occ_c4615d109e2957a6c04ff773"></a>`occ_c4615d109e2957a6c04ff773` | high | core | [A crafted DSi ROM can overflow a host stack buffer through unaligned NAND block transfers](cases/occ_c4615d109e2957a6c04ff773.md) |
| <a id="occ_57ddbf72d1386b72987ba0b8"></a>`occ_57ddbf72d1386b72987ba0b8` | high | core | [A crafted DSi ROM can read past SD/MMC response objects into host memory](cases/occ_57ddbf72d1386b72987ba0b8.md) |
| <a id="occ_73d6504df53c15f837d73f4e"></a>`occ_73d6504df53c15f837d73f4e` | high | core | [A crafted DSi ROM can obtain uninitialized host stack bytes through failed SD or NAND reads](cases/occ_73d6504df53c15f837d73f4e.md) |
| <a id="occ_54a7deb0ad08a3fafebc1726"></a>`occ_54a7deb0ad08a3fafebc1726` | high | app | [Crafted savestates escape camera, GPU2D, SD, RTC, I2C, GBA, DSP, and DMA invariants](cases/occ_54a7deb0ad08a3fafebc1726.md) |
| <a id="occ_31750274562430030e68a5db"></a>`occ_31750274562430030e68a5db` | high | core | [Retail NAND cartridge savestates retain unchecked staging cursors and addresses](cases/occ_31750274562430030e68a5db.md) |
| <a id="occ_f3a0f11c9bc863db1b13f85f"></a>`occ_f3a0f11c9bc863db1b13f85f` | medium | app | [Android save-sidecar loading can allocate nearly 4 GiB from a provider-controlled or failed length](cases/occ_f3a0f11c9bc863db1b13f85f.md) |
| <a id="occ_e52f1eda044745cca9bad3bb"></a>`occ_e52f1eda044745cca9bad3bb` | medium | core | [Restored DSi DSP timestamps can trap the emulator in a zero-progress catch-up loop](cases/occ_e52f1eda044745cca9bad3bb.md) |
| <a id="occ_95f95be3047a6de20473771d"></a>`occ_95f95be3047a6de20473771d` | medium | app | [Closing the DSiWare screen can free NAND state while title enumeration is still running](cases/occ_95f95be3047a6de20473771d.md) |
| <a id="occ_b5ec7b0906603afed2916ce7"></a>`occ_b5ec7b0906603afed2916ce7` | high | core | [R4 cartridge override bypasses corrected B7 bounds and overreads native ROM memory](cases/occ_b5ec7b0906603afed2916ce7.md) |
| <a id="occ_81cd48e8ae72ac0edbf324ac"></a>`occ_81cd48e8ae72ac0edbf324ac` | medium | app | [ROM metadata offsets can force multi-gigabyte archive decoding during scans or granted-content launch](cases/occ_81cd48e8ae72ac0edbf324ac.md) |
| <a id="occ_5830142954786d70a8044c22"></a>`occ_5830142954786d70a8044c22` | medium | app | [R4 guest commands dereference SD storage that Android intentionally omits](cases/occ_5830142954786d70a8044c22.md) |
| <a id="occ_19c2f7d0a71bb16afcebbb17"></a>`occ_19c2f7d0a71bb16afcebbb17` | medium | app | [Cyclic document-provider trees can repeatedly crash ROM library scanning](cases/occ_19c2f7d0a71bb16afcebbb17.md) |
| <a id="occ_4c6c667123e2f9c495f39cbe"></a>`occ_4c6c667123e2f9c495f39cbe` | medium | core | [Malformed DSi NAND exports can disclose uninitialized stack bytes](cases/occ_4c6c667123e2f9c495f39cbe.md) |
| <a id="occ_ac89863ef8213945c0ee1634"></a>`occ_ac89863ef8213945c0ee1634` | medium | app | [G.711 DSP commands can trap the Android emulation thread in billions of iterations](cases/occ_ac89863ef8213945c0ee1634.md) |
| <a id="occ_b3ec145f5094e12cfd0ad76e"></a>`occ_b3ec145f5094e12cfd0ad76e` | medium | app | [Crafted NAND title directories can exhaust Android heap during DSiWare checks](cases/occ_b3ec145f5094e12cfd0ad76e.md) |
| <a id="occ_b2e00e7deb81cb8414cb9ac7"></a>`occ_b2e00e7deb81cb8414cb9ac7` | medium | app | [Cheat XML and DAT imports still lack an actual-byte and document-wide resource budget](cases/occ_b2e00e7deb81cb8414cb9ac7.md) |
| <a id="occ_1db371734a02d73a8775b1cc"></a>`occ_1db371734a02d73a8775b1cc` | low | app | [Failed save-writability checks leak native provider descriptors](cases/occ_1db371734a02d73a8775b1cc.md) |
| <a id="occ_57050ca0e32fd3583bc589bc"></a>`occ_57050ca0e32fd3583bc589bc` | informational | core | [[Completed candidate disposition] Core current-state reconciliation](cases/occ_57050ca0e32fd3583bc589bc.md) |
| <a id="occ_2ef748460c8bc14f9e87a687"></a>`occ_2ef748460c8bc14f9e87a687` | high | core | [Teakra DMA active-channel MMIO accepts arbitrary array indices](cases/occ_2ef748460c8bc14f9e87a687.md) |
| <a id="occ_f332a69bdae9b2e0d1dac524"></a>`occ_f332a69bdae9b2e0d1dac524` | high | core | [Savestate scheduler fix leaves DisplayFIFO event parameters able to corrupt native memory](cases/occ_f332a69bdae9b2e0d1dac524.md) |
| <a id="occ_ae5b73c4466f8467cbd84442"></a>`occ_ae5b73c4466f8467cbd84442` | high | core | [GPU2D savestates restore unchecked display-FIFO pointers](cases/occ_ae5b73c4466f8467cbd84442.md) |
| <a id="occ_a28be96fd4601332ade34084"></a>`occ_a28be96fd4601332ade34084` | medium | app | [Persistent native threads leak JNI local references across SAF and camera callbacks](cases/occ_a28be96fd4601332ade34084.md) |
| <a id="occ_fd5c865996d47f8a29eaaafc"></a>`occ_fd5c865996d47f8a29eaaafc` | low | app | [GitHub builds contact GitHub after update checks are disabled](cases/occ_fd5c865996d47f8a29eaaafc.md) |
| <a id="occ_f49486b60787daa20317b351"></a>`occ_f49486b60787daa20317b351` | medium | app | [RetroAchievements responses can exhaust the Android heap before unbounded JNI mapping](cases/occ_f49486b60787daa20317b351.md) |
| <a id="occ_3c141d4de0621f3163477e28"></a>`occ_3c141d4de0621f3163477e28` | medium | app | [Malformed DSi NAND reads expose uninitialized host memory during boot](cases/occ_3c141d4de0621f3163477e28.md) |
| <a id="occ_1ba9fffd6413a9fed864cc8a"></a>`occ_1ba9fffd6413a9fed864cc8a` | medium | core | [DSi NWiFi BMI commands run up to 2^32 guest-selected iterations](cases/occ_1ba9fffd6413a9fed864cc8a.md) |
| <a id="occ_1fca5222a4e54a0e50d62054"></a>`occ_1fca5222a4e54a0e50d62054` | low | app | [DAT cheat import has no aggregate byte, object, or database-growth budget](cases/occ_1fca5222a4e54a0e50d62054.md) |
| <a id="occ_b8800026c2c1514ac6cf77e3"></a>`occ_b8800026c2c1514ac6cf77e3` | low | app | [Provider write exceptions cross JNI unchecked and poison the native save worker](cases/occ_b8800026c2c1514ac6cf77e3.md) |
| <a id="occ_374991baf934127ee85e4c58"></a>`occ_374991baf934127ee85e4c58` | medium | app | [The sized rcheevos deserializer still overreads malformed RCHV chunks](cases/occ_374991baf934127ee85e4c58.md) |
| <a id="occ_f26f37e53de818cf6047c118"></a>`occ_f26f37e53de818cf6047c118` | low | app | [Restored firmware text can crash generated-firmware startup](cases/occ_f26f37e53de818cf6047c118.md) |
| <a id="occ_e7ee97f5407adcc076962982"></a>`occ_e7ee97f5407adcc076962982` | medium | app | [Savestate-restored multiplayer reply lengths overflow the native Wi-Fi receive buffer](cases/occ_e7ee97f5407adcc076962982.md) |
| <a id="occ_2cbc1cd3c17db642b326d34c"></a>`occ_2cbc1cd3c17db642b326d34c` | medium | app | [Exhausted guest DHCP leases leak native reply buffers on every subsequent request](cases/occ_2cbc1cd3c17db642b326d34c.md) |
| <a id="occ_198b7bfed24d33a6f5a9703a"></a>`occ_198b7bfed24d33a6f5a9703a` | medium | app | [Provider-controlled ROM names can escape, collide with, and overwrite SRAM and savestate targets](cases/occ_198b7bfed24d33a6f5a9703a.md) |
| <a id="occ_56f99ecbb0c411912ec720d8"></a>`occ_56f99ecbb0c411912ec720d8` | medium | app | [Parenthesis stripping permits quadratic cheat-name processing and persistent UI hangs](cases/occ_56f99ecbb0c411912ec720d8.md) |
| <a id="occ_51f8dfa0478282b4bf2b39ae"></a>`occ_51f8dfa0478282b4bf2b39ae` | high | core | [ROM padding exposes uninitialized native heap bytes to emulated cartridges](cases/occ_51f8dfa0478282b4bf2b39ae.md) |
| <a id="occ_7e9c0b4d94b8437452182fb0"></a>`occ_7e9c0b4d94b8437452182fb0` | medium | core | [Cartridge bounds fix omits word-rounded direct-boot reads](cases/occ_7e9c0b4d94b8437452182fb0.md) |
| <a id="occ_76beba3dc134ea6d6c6e02b8"></a>`occ_76beba3dc134ea6d6c6e02b8` | low | app | [Loading a crafted RetroAchievements definition can make Android read across an emulated-memory region boundary](cases/occ_76beba3dc134ea6d6c6e02b8.md) |
| <a id="occ_95b372be5c7a53614199db50"></a>`occ_95b372be5c7a53614199db50` | medium | app | [Enabling a crafted Action Replay cheat can freeze the Android emulator for billions of operations](cases/occ_95b372be5c7a53614199db50.md) |
| <a id="occ_2142f6825b17450c0807e331"></a>`occ_2142f6825b17450c0807e331` | medium | app | [Importing a wrong-size DSiWare save can replace the installed title’s persistent data](cases/occ_2142f6825b17450c0807e331.md) |
| <a id="occ_6fe4c3eccf94ed4e2f5b5fc2"></a>`occ_6fe4c3eccf94ed4e2f5b5fc2` | medium | app | [A name-only NAND candidate can make guided setup consume storage until provider EOF](cases/occ_6fe4c3eccf94ed4e2f5b5fc2.md) |
| <a id="occ_277d6d22695d3f279a0df1f1"></a>`occ_277d6d22695d3f279a0df1f1` | medium | app | [Failed savestate loads leave partial state because the backup is never switched to load mode](cases/occ_277d6d22695d3f279a0df1f1.md) |
| <a id="occ_1c733703a47262e271e37f76"></a>`occ_1c733703a47262e271e37f76` | medium | core | [Invalid DSi camera dimensions create an endless zero-delay event loop](cases/occ_1c733703a47262e271e37f76.md) |
| <a id="occ_88d732c7d8c916df74e2401a"></a>`occ_88d732c7d8c916df74e2401a` | low | core | [Running a DSi ROM with an odd DSP audio queue can crash the emulator](cases/occ_88d732c7d8c916df74e2401a.md) |
| <a id="occ_836e4218de342a38455bf14c"></a>`occ_836e4218de342a38455bf14c` | low | core | [A DSi DSP DMA length can wrap its progress counter and hang the emulator thread](cases/occ_836e4218de342a38455bf14c.md) |
| <a id="occ_3f076c55d71a70958a33220f"></a>`occ_3f076c55d71a70958a33220f` | medium | app | [An external app can substitute an unrelated cached ROM during an exported launch](cases/occ_3f076c55d71a70958a33220f.md) |
| <a id="occ_a32f5bdeefb1bbbc68fd7eaf"></a>`occ_a32f5bdeefb1bbbc68fd7eaf` | medium | app | [Large achievement sets exhaust JNI references and pin Java strings](cases/occ_a32f5bdeefb1bbbc68fd7eaf.md) |
| <a id="occ_e3d247849cd9edb46f0819ab"></a>`occ_e3d247849cd9edb46f0819ab` | low | app | [RetroAchievements HTTP errors leak response resources across repeated requests and retries](cases/occ_e3d247849cd9edb46f0819ab.md) |
| <a id="occ_6cb0ac532a98fc7abd11b3aa"></a>`occ_6cb0ac532a98fc7abd11b3aa` | medium | app | [Static-camera image decoding lacks a dimension and pixel budget](cases/occ_6cb0ac532a98fc7abd11b3aa.md) |
| <a id="occ_6422719eff0c108a08550a57"></a>`occ_6422719eff0c108a08550a57` | medium | core | [A restored zero SPU interval can trap the emulator thread at one timestamp](cases/occ_6422719eff0c108a08550a57.md) |
| <a id="occ_f10f444f438e1120ee83f810"></a>`occ_f10f444f438e1120ee83f810` | high | core | [R4 data commands expose an uninitialized native heap buffer to guest code](cases/occ_f10f444f438e1120ee83f810.md) |
| <a id="occ_836a150ecb9cff2653ede290"></a>`occ_836a150ecb9cff2653ede290` | medium | core | [Live SPU timer-reload writes can index before interpolation tables](cases/occ_836a150ecb9cff2653ede290.md) |
| <a id="occ_681a4f167a153dbb72619581"></a>`occ_681a4f167a153dbb72619581` | high | core | [Restored SPU scheduler parameters can write beyond blip audio buffers](cases/occ_681a4f167a153dbb72619581.md) |
| <a id="occ_cb3a39caab485844bb03f2f0"></a>`occ_cb3a39caab485844bb03f2f0` | low | core | [A crafted DSi guest can extend the selected host NAND file beyond its declared capacity](cases/occ_cb3a39caab485844bb03f2f0.md) |
| <a id="occ_835616344065fc5650304ffc"></a>`occ_835616344065fc5650304ffc` | high | core | [Restored GPU VRAM mapping masks select outside the native bank tables](cases/occ_835616344065fc5650304ffc.md) |
| <a id="occ_845f508c28d308a3ed6921f4"></a>`occ_845f508c28d308a3ed6921f4` | high | core | [GPU3D transient validation omits the polygon mode and vertex-count relationship](cases/occ_845f508c28d308a3ed6921f4.md) |
| <a id="occ_86c50141e68ec4d1fcff078f"></a>`occ_86c50141e68ec4d1fcff078f` | medium | app | [G.711 output to SCFG_RST can delete the executing DSP HLE object](cases/occ_86c50141e68ec4d1fcff078f.md) |
| <a id="occ_f1b3992fa930f8e5be9671e5"></a>`occ_f1b3992fa930f8e5be9671e5` | low | core | [DSi Wi‑Fi EEPROM window reads beyond the fixed array at tail offsets](cases/occ_f1b3992fa930f8e5be9671e5.md) |
| <a id="occ_f81c873106385dfd79200c78"></a>`occ_f81c873106385dfd79200c78` | medium | core | [Guest-triggered rumble events enter an effectively unbounded Android SharedFlow](cases/occ_f81c873106385dfd79200c78.md) |
| <a id="occ_5aa0cdd63e52e8bf6e5c5f58"></a>`occ_5aa0cdd63e52e8bf6e5c5f58` | medium | core | [Guest-corrupted R4 FAT geometry causes native division by zero](cases/occ_5aa0cdd63e52e8bf6e5c5f58.md) |
| <a id="occ_44c82f1de1a141fb56634019"></a>`occ_44c82f1de1a141fb56634019` | low | app | [Verify every Gradle artifact before privileged release builds](cases/occ_44c82f1de1a141fb56634019.md) |
| <a id="occ_7fddae3d4a0847da222da76c"></a>`occ_7fddae3d4a0847da222da76c` | high | app | [Unrestricted tag pushes can initiate Google Play production deployment](cases/occ_7fddae3d4a0847da222da76c.md) |
| <a id="occ_2d9284d05331b527dffd90b6"></a>`occ_2d9284d05331b527dffd90b6` | high | app | [Unrestricted tag pushes reach production signing and GitHub publication](cases/occ_2d9284d05331b527dffd90b6.md) |
| <a id="occ_fe120097762083f50d897200"></a>`occ_fe120097762083f50d897200` | low | app | [Android JNI entrypoints leak UTF string leases across repeated operations](cases/occ_fe120097762083f50d897200.md) |
| <a id="occ_3b717bb0887528b36797d1bb"></a>`occ_3b717bb0887528b36797d1bb` | low | app | [Loading a crafted DSi savestate can trap AES processing in billions of iterations](cases/occ_3b717bb0887528b36797d1bb.md) |
| <a id="occ_dad7fd6942e9dd4f18a15e3f"></a>`occ_dad7fd6942e9dd4f18a15e3f` | low | app | [Nested-core CI checks execute a mutable GitHub Action outside the integrity checker's scope](cases/occ_dad7fd6942e9dd4f18a15e3f.md) |
