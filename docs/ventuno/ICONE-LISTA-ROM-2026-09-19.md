# Icone della lista ROM e rotellina all'avvio — 19/09/2026

## Segnalazione
Nella 2.1.1 sulla Thor, all'avvio, la lista delle ROM compare ma la rotellina di aggiornamento
continua a girare e le icone delle ROM restano vuote per qualche secondo; nella 2.0.1 stabile
le icone sono immediate. Dopo uno swipe di aggiornamento le icone tornano subito.

## Causa (verificata)
Il commit 6aeab3bc (7z: un archivio alla volta) aveva fatto condividere **un solo thread**
(`Dispatchers.IO.limitedParallelism(1)`, qualificatore `@RomFileAccessDispatcher`) fra la
scansione delle cartelle ROM (`FileSystemRomsRepository`) e la lettura delle icone
(`RomIconProvider`). All'avvio la scansione occupa quel thread per tutta la sua durata (legge ogni
ROM fino al banner: con la microSD della Thor a ~17 MB/s e cache disco fredda sono ~3,5 s) e ogni
icona richiesta dalla lista, anche se già in cache su disco, resta in coda dietro di essa.
La rotellina è legata alla scansione ed è identica nella 2.0.1 (SwipeRefreshLayout): la differenza
percepita è solo che nella 2.0.1 le icone arrivano subito su un thread proprio.
Con cache disco calda (secondo avvio) la scansione dura 0,3 s e il difetto non si vede.

## Prove sulla Thor (riavvio = cache fredda; screenshot a ~4 fps; rotellina = scansione)
| Build | Lista visibile | Icone visibili | Fine rotellina |
|---|---|---|---|
| 2.1.1 prima (aed28d3f…) | 0,8 s | **4,3 s** (insieme alla fine della rotellina) | 4,0–4,3 s |
| 2.0.1 stabile | 0,6 s | 0,6 s | 3,7–4,0 s |
| 2.1.1 corretta (26ceba58…) | 0,8 s | **1,1 s** | 4,6–4,9 s |

Screenshot e tabelle in `~/Developer/android-test/thor/aggiornamento-20260919/icone-lista-rom/`.

## Correzione (commit a9b8ded2)
- `RomIconProvider` torna ad avere il proprio dispatcher a thread singolo (come nell'upstream);
  `MelonModule` non gli passa più `@RomFileAccessDispatcher`.
- Il vincolo di memoria che motivava la condivisione (un solo archivio 7z/zip decodificato alla
  volta, per il dizionario LZMA2 sullo heap Java) è ora un `ReentrantLock` di processo in
  `CompressedRomFileProcessor`, tenuto mentre un archivio è aperto da scansione, estrazione icona,
  info ROM ed estrazione nella cache. I file `.nds` e le ROM già estratte non lo prendono mai.
- Effetto collaterale corretto: lo stream del file archivio viene chiuso anche quando l'archivio
  non contiene una ROM.

## Test
- `RomIconProviderTest`: icona in cache servita mentre il dispatcher della scansione è occupato.
  Prima della correzione falliva con «Cached icon took 3063 ms: it waited for the ROM scan».
- `CompressedRomFileProcessorTest`: tre archivi decodificati uno alla volta. Senza il lock fallisce
  con 3 decoder contemporanei.
- Suite completa: 112 test, 0 errori.

## Build e installazione
- APK: `~/Developer/android-test/apk/ventuno-2.1.1/melonDS-2.1.1-arm64-20260919-icone-lista-rom.apk`
  (SHA-256 26ceba58…, in `SHA256SUMS`), variante gitHubNightlyProfiling, versionCode 42, 2.1.1,
  firma = debug keystore del 12/09 (identica all'APK installata il 15/09).
- Installata il 19/09 alle 11:32 con `adb install -r -t --no-incremental` (aggiornamento in place):
  `firstInstallTime` invariato (12/09 18:02), permessi SAF su `ROMs/nds` e BIOS conservati, cache
  icone (14 file) conservata, salvataggi e stati sulla microSD non toccati. Backup preventivo di
  `.sav`, `.ml0`, cheats xml e APK installata in
  `~/Developer/android-test/thor/aggiornamento-20260919/backup-pre-fix/` (SHA256SUMS-nds.txt).
- Per le prove la Thor è stata riavviata tre volte (blocco schermo disattivato, nessuna partita in
  corso); dopo il riavvio Android mostra il dialogo di sistema «Use USB for» sopra l'app.
