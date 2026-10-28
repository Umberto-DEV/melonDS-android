# Issue e patch JIT rilevanti per melonDS Android

Ricerca del 28 ottobre 2026 nei repository `rafaelvcaetano/melonDS-android`, `rafaelvcaetano/melonDS-android-lib` e nel core condiviso `melonDS-emu/melonDS`, mediante il plugin GitHub. Il difetto di teardown ha un riscontro upstream esatto. Per l'alias DTCM da 4 KiB emerso nel test sintetico corrente non è stata trovata una patch già pronta equivalente. Le segnalazioni Android con JIT instabile non dimostrano, da sole, la stessa causa.

## Categorie e conclusioni

| Categoria | Riscontro | Conseguenza per il lavoro Android |
| --- | --- | --- |
| Bug noto, corrispondenza esatta | Core #2537, lettura di ARM9 dopo la sua distruzione durante il teardown JIT | Citare la segnalazione e l'analisi originale. Nessuna patch collegata trovata; verificare la soluzione locale con sanitizer rigorosi. |
| Difetto riprodotto localmente, nessun equivalente trovato | DTCM da 4 KiB a `0x02001000`, mirror fastmem da 16 KiB | Proseguire con una correzione Android del core condiviso guidata dalla riproduzione; non presentarla come backport di una soluzione esistente. |
| Patch Android esistenti per problemi distinti | Core Android PR #11 e #12, invalidazione e literal Thumb | Conservare attribuzione e distinguere i meccanismi. Non spiegano il mapping DTCM errato. |
| Patch upstream pronta ma per compilazione | Commit `ab9e6240`, macro Android, issue #2706 | Candidata separata di portabilità. La build locale usa già `-DANDROID`, quindi non è un blocco dimostrato della build attuale. |
| Sintomi Android senza causa dimostrata | App #579, #751, #1642 | Utili come scenari futuri di regressione; nessuna equivalenza causale con il test DTCM. |

## Teardown già segnalato upstream

[Core #2537 — JIT: Use-after-free in NDS class destructor (no impact)](https://github.com/melonDS-emu/melonDS/issues/2537), aperta da **maximxlss** il 9 gennaio 2026, risultava ancora aperta alla verifica. La descrizione identifica proprio la sequenza `ARMJIT::~ARMJIT()` → `ResetBlockCache()` → `ARMJIT_Memory::Reset()` → `Mapping::Unmap()`, che legge `nds.ARM9.DTCMBase` e `DTCMMask` dopo la distruzione di ARM9. Propone la riproduzione con JIT abilitato, uscita dall'app e sanitizer senza recupero dagli errori. È la stessa violazione di durata degli oggetti rilevata dalla CI corrente, benché la segnalazione originale non sia una prova su un dispositivo Android.

[RSDuck suggerisce di eliminare o sostituire il reset del block cache nel distruttore](https://github.com/melonDS-emu/melonDS/issues/2537#issuecomment-3730164157). È un'indicazione progettuale, non una patch approvata o un commit pronto. La timeline consultata non contiene PR o commit collegati. La soluzione locale che svuota i mapping mentre ARM9 è ancora vivo deve quindi essere descritta e verificata come implementazione locale della correzione, citando questa precedente segnalazione.

La vecchia [PR #1798 — Make NDS teardown more robust](https://github.com/melonDS-emu/melonDS/pull/1798), merge `db963aa002cdf943c86d19f8abd3f4fd40be38ec`, riguarda la robustezza dell'inizializzazione/deinizializzazione nel 2023. Non risolve automaticamente il successivo problema di ordine dei membri descritto da #2537.

## Mapping DTCM da 4 KiB

La riproduzione riferita dal lavoro corrente usa DTCM da 4 KiB a `0x02001000`, codice a `0x02000000` e risultato a `0x02000300`; Linux x64 e ARM64 restituiscono un valore precedente dopo la modifica DTCM. L'analisi locale attribuisce il risultato al mirror fisso da 16 KiB e al trattamento della regione DTCM stessa come intervallo nel quale lasciare un buco. Questa diagnosi viene dal debugging locale, non dalle issue trovate.

Il sorgente upstream consultato conserva il mirror DTCM fisso: `mirrorStart = addr & ~(DTCMPhysicalSize - 1)` e `mirrorSize = DTCMPhysicalSize`. Lo fanno sia [il core condiviso al commit `1e654443e4c0a54b40709c0208aa0eeba41e6142`](https://github.com/melonDS-emu/melonDS/blob/1e654443e4c0a54b40709c0208aa0eeba41e6142/src/ARMJIT_Memory.cpp#L1040) sia [il core Android upstream al commit `431ab4bd0003c4356e25fce89640ae1004579b9b`](https://github.com/rafaelvcaetano/melonDS-android-lib/blob/431ab4bd0003c4356e25fce89640ae1004579b9b/src/ARMJIT_Memory.cpp#L1040). Questo sostiene la conclusione limitata che aggiornarsi semplicemente a questi snapshot upstream non importa una correzione del mirror.

Le ricerche di issue e PR per `DTCM`, `DTCMBase`, `DTCMMask`, `GetMirrorLocation`, `MapAtAddress`, `DTCM 4KB`, `DTCM 4KiB`, `fastmem`, `teardown`, `destructor` e `Unmap`, più la cronologia di `ARMJIT_Memory.cpp`, non hanno restituito una correzione equivalente. È un esito della ricerca mirata, non una garanzia che nessuna discussione non indicizzata esista.

Tre precedenti del core condiviso aiutano a distinguere il problema:

- [`d697f9e0d245da0ed0061724e24e4a476091ee7e` — make fastmem work again](https://github.com/melonDS-emu/melonDS/commit/d697f9e0d245da0ed0061724e24e4a476091ee7e), **RSDuck**, 13 novembre 2020, introduce la logica che lascia libero l'intervallo DTCM in `MapAtAddress` e `Unmap`. È provenienza del comportamento da esaminare, non una correzione pronta del difetto attuale.
- [`15a66b1be1552615ac3017a225009e0f12cad351` — more accurate DTCM check](https://github.com/melonDS-emu/melonDS/commit/15a66b1be1552615ac3017a225009e0f12cad351) modifica controlli DTCM e accessi CP15/slow path usando la maschera e l'offset fisico. Non modifica il mirror fastmem di `GetMirrorLocation`.
- [`b12e1a1b6e2c983f522ef1369a6bfb8167b1bf4c` — JIT fastmem: fix out of bounds read](https://github.com/melonDS-emu/melonDS/commit/b12e1a1b6e2c983f522ef1369a6bfb8167b1bf4c) anticipa il controllo `offset < mirrorSize` rispetto a `PageContainsCode`. L'ordine corretto è già presente nel core locale, `src/ARMJIT_Memory.cpp:705`. È un altro bug, non il mapping da 4 KiB.

La [PR #2201 — Multiinstance jit](https://github.com/melonDS-emu/melonDS/pull/2201), merge `99ce959913bd678781d066dc78a9e30e1daf5f05`, contiene evoluzioni JIT/ARM64 e gestione delle pagine host. Non è una patch specifica per il difetto corrente. La correzione va valutata anche rispetto alla dimensione della pagina host Android: un successo Linux ARM64 con pagine da 4 KiB, da solo, non dimostra il comportamento su dispositivi Android con altre dimensioni di pagina.

## Patch Android per invalidazione e Thumb

[Core Android PR #11 — JIT: invalidate only blocks overlapping the written code granule](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/11), di **Umberto-DEV**, head `b820d177a6e9b7790f01878503cd1101df1d5781`, risultava chiusa e non unita. Corregge la maschera di invalidazione oscurata da una variabile interna: il blocco deve intersecare la maschera della scrittura. Il comportamento corretto è presente nella copia locale, `src/ARMJIT.cpp:947–961`. Non occorre reimportare la patch per risolvere il mapping DTCM.

[Core Android PR #12 — AArch64 JIT: align Thumb PC-relative loads after literal invalidation](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/12), sempre di **Umberto-DEV**, head `b301606ef49c24eae659b051f0dbfeac3d0963a2`, risultava chiusa e non unita. Il diff aggiunge l'allineamento Thumb del PC nel percorso di ottimizzazione literal. Non è identico alla soluzione locale corrente: questa richiede `CurInstr.LiteralRegistered` (`src/ARMJIT_A64/ARMJIT_LoadStore.cpp:115`) e ha l'allineamento Thumb nel percorso normale (`:132–135`). Non descrivere quindi #12 come applicata testualmente o automaticamente ancora necessaria. Nessuno dei due diff modifica `MapAtAddress`, `Unmap` o `GetMirrorLocation`.

## Correzione pronta della macro Android

[`ab9e624082040fe2abf76175e71a549104fe2979` — JIT: fastmem: fix Android predefined macro](https://github.com/melonDS-emu/melonDS/commit/ab9e624082040fe2abf76175e71a549104fe2979), di **Rayyan Ansari**, 28 luglio 2026, sostituisce `#ifdef ANDROID` con `#if defined(__ANDROID__)` per il membro `Libandroid` di `ARMJIT_Memory`. Il messaggio chiude esplicitamente [#2706 — Compilation Issue: Termux](https://github.com/melonDS-emu/melonDS/issues/2706).

È una patch piccola e pertinente ad Android, ma riguarda un errore di compilazione quando manca la macro non standard `ANDROID`. Il core locale conserva `#ifdef ANDROID` a `src/ARMJIT_Memory.h:193`; i comandi NDK generati nell'attuale `app/.cxx/Debug/532z4415/arm64-v8a/compile_commands.json` contengono già `-DANDROID`. Pertanto non spiega né il fallimento DTCM né un errore di build osservato in questa configurazione. Va eventualmente classificata come allineamento/portabilità Android separato, con attribuzione originale, senza ampliare il fix corrente per necessità presunte.

## Segnalazioni Android con sintomi simili

| Fonte | Evidenza e stato alla consultazione | Limite della corrispondenza |
| --- | --- | --- |
| [App #579 — Super Mario256 JIT Crash](https://github.com/rafaelvcaetano/melonDS-android/issues/579) | Aperta; ROM modificata di Super Mario 64 DS, crash con JIT su Galaxy S7 Snapdragon/Android 8. [Il reporter riferisce che Lemuroid e Switch funzionano](https://github.com/rafaelvcaetano/melonDS-android/issues/579#issuecomment-904046318). | Nessun indirizzo o configurazione DTCM, stack o test equivalente. La differenza fra frontend/core rende scorretto equipararla automaticamente al bug condiviso. |
| [App #751 — Issues with AiRace games with JIT enabled](https://github.com/rafaelvcaetano/melonDS-android/issues/751) | Aperta; AiRace in DSi, problemi grafici/input con JIT, Galaxy A71/Android 11; JIT disabilitato funziona secondo il reporter. | Nessuna diagnosi DTCM o patch dimostrata. |
| [App #1642 — JIT not working](https://github.com/rafaelvcaetano/melonDS-android/issues/1642) | Chiusa; Galaxy A16/Android 16, avvio da menu DSi verso giochi con schermata bianca. [Il maintainer suggerisce di disabilitare Show boot screen](https://github.com/rafaelvcaetano/melonDS-android/issues/1642#issuecomment-4858851086), [il reporter conferma il workaround](https://github.com/rafaelvcaetano/melonDS-android/issues/1642#issuecomment-4859218734). | Il maintainer lo considera un problema del core condiviso, ma non collega un commit risolutivo o una diagnosi DTCM. La chiusura segue il workaround, non una prova di correzione del core. |

Esiste anche il precedente condiviso [#699 — JIT: DSi-mode crash when returning to DSi menu](https://github.com/melonDS-emu/melonDS/issues/699) e il commit [`2a3147db461d8538334245cb1073e8b46ad2a99d` — reset fastmem on DSi soft reset](https://github.com/melonDS-emu/melonDS/commit/2a3147db461d8538334245cb1073e8b46ad2a99d). Il commit ripulisce i mapping prima del reset delle CPU; è un precedente tecnico utile per il ciclo di vita DSi, ma non prova che le successive schermate bianche Android siano la stessa regressione.

## Ambito della verifica

Questa ricerca non applica patch, non esegue build o test e non pubblica PR. Le evidenze runtime Linux e la diagnosi del test sintetico sono state fornite dal lavoro di debugging corrente; non vengono presentate come verifica su dispositivo Android. I sorgenti locali sono stati letti quando il core era a `20cf21d7425ab97db75e96dda41190ab9e93494a`.

Sono state escluse dalle proposte le correzioni specifiche macOS, tra cui la [PR #1728](https://github.com/melonDS-emu/melonDS/pull/1728) e il commit [`7d8dea42e221b13521270c423b61add41a741b97`](https://github.com/melonDS-emu/melonDS/commit/7d8dea42e221b13521270c423b61add41a741b97): non risolvono il target Android richiesto. Analogamente le modifiche OpenBSD/NetBSD alle protezioni della memoria non vengono proposte come soluzione del difetto osservato.
