# Issue note: melonDS Android e libreria

Inventario verificato il 28 ottobre 2026: **246 issue aperte, in 13 categorie funzionali** (242 app, 4 libreria). Ogni issue compare una sola volta nella categoria primaria. Le pull request sono escluse da questi conteggi e trattate separatamente nelle bozze e nella ricerca JIT.

Fonti complete: [app](all-open-app-issues.json), [libreria](all-open-core-issues.json). Il [registro JSON](ISSUE-CATEGORIES.json) conserva per ogni voce motivazione, evidenze sorgente, hash del corpo, note storiche e limiti. I 242 corpi app coincidono con la lettura completa del 24 ottobre; erano stati approfonditi 55 thread selezionati e 234 commenti, non tutti i commenti di tutte le issue.

**“Coperto nel codice” non significa verificato su tutti i telefoni o giochi, né autorizza la chiusura upstream.** “Parziale” indica una correzione pertinente o un workaround con aspetti ancora aperti. “Candidato concreto” richiede implementazione e prova; “da indagare” richiede isolamento/riproduzione; l’ultima colonna indica funzioni o lavori ampi da valutare separatamente, non problemi privi di valore.

## Conteggi

| Categoria | Totale | Coperto | Parziale | Candidato | Da indagare | Intervento più ampio |
|---|---:|---:|---:|---:|---:|---:|
| Audio e microfono | 12 | 0 | 3 | 1 | 7 | 1 |
| Grafica e display | 31 | 0 | 6 | 3 | 17 | 5 |
| Touch e controller | 20 | 6 | 4 | 0 | 5 | 5 |
| Salvataggi, stati e rewind | 13 | 1 | 5 | 0 | 2 | 5 |
| File ROM, SAF e archivi | 14 | 1 | 4 | 0 | 4 | 5 |
| Avvio, BIOS, firmware e DSi | 43 | 1 | 11 | 1 | 26 | 4 |
| JIT, prestazioni e compatibilità | 37 | 1 | 5 | 0 | 25 | 6 |
| Rete e multiplayer | 29 | 0 | 2 | 0 | 14 | 13 |
| Cheat | 4 | 1 | 2 | 0 | 0 | 1 |
| RetroAchievements | 6 | 0 | 0 | 0 | 5 | 1 |
| Ciclo di vita e integrazione Android | 20 | 2 | 11 | 0 | 3 | 4 |
| Interfaccia e lingue | 13 | 2 | 1 | 0 | 1 | 9 |
| Distribuzione e altre funzioni | 4 | 0 | 1 | 0 | 0 | 3 |
| **Totale** | **246** | **15** | **55** | **5** | **109** | **62** |

Le 15 voci coperte comprendono richieste già disponibili e correzioni precedenti: non sono 15 nuovi bug risolti in questa sessione. L’unica promozione app rispetto al vecchio registro è #1253, limitatamente al cinese semplificato `zh-Hans`. Core #2 è coperta solo per aggiornamento della versione del core; la compatibilità C-Gear resta da provare.

## Priorità per il debug Android

1. **Guadagno e saturazione microfono** — [#1679](https://github.com/rafaelvcaetano/melonDS-android/issues/1679), [#1285](https://github.com/rafaelvcaetano/melonDS-android/issues/1285). Acquisire campioni di input Android con tono/livello noto; misurare clipping e assenza di segnale separatamente. Verificare MIC_GAIN 10 e conversione s16 prima di definire gain UI o saturazione. Prova richiesta: Test numerici di saturazione più registrazioni confrontabili su dispositivi interessati.

2. **Quota logica e rollback DSiWare** — [#1651](https://github.com/rafaelvcaetano/melonDS-android/issues/1651), [#1099](https://github.com/rafaelvcaetano/melonDS-android/issues/1099). Con una copia NAND fornita dall’utente, misurare blocchi titoli/salvataggi/metadati e riprodurre l’import che impedisce il boot HOME. Definire preflight e rollback senza confondere spazio FAT con quota DSi. Prova richiesta: NAND riproducibile prima/dopo, invarianti di import fallito e boot del menu verificato.

3. **Durabilità firmware e SRAM** — [#1649](https://github.com/rafaelvcaetano/melonDS-android/issues/1649), [#1594](https://github.com/rafaelvcaetano/melonDS-android/issues/1594), [#685](https://github.com/rafaelvcaetano/melonDS-android/issues/685), [#698](https://github.com/rafaelvcaetano/melonDS-android/issues/698). Confrontare byte/CRC di copie firmware e tracciare flush/errori SAF; separare scritture normali WFC da corruzione e permessi cross-profile da troncamento. Non modificare file originali dell’utente per diagnosticare. Prova richiesta: Riproduzione controllata dei percorsi di scrittura su Android e conservazione byte fuori dagli intervalli autorizzati.

4. **Casting e controllo del display esterno** — [#1663](https://github.com/rafaelvcaetano/melonDS-android/issues/1663), [#1641](https://github.com/rafaelvcaetano/melonDS-android/issues/1641), [#1610](https://github.com/rafaelvcaetano/melonDS-android/issues/1610), [#1619](https://github.com/rafaelvcaetano/melonDS-android/issues/1619). Registrare identità e ciclo vita di display fisici/virtuali con casting, HDMI e Thor; definire il comportamento mirroring/disabilitazione e accessibilità dell’editor su TV. Il toggle non esiste ancora. Prova richiesta: Matrice connect/disconnect/rotation con entrambi gli schermi e input utilizzabili.

5. **Sblocchi RetroAchievements** — [#1611](https://github.com/rafaelvcaetano/melonDS-android/issues/1611), [#1620](https://github.com/rafaelvcaetano/melonDS-android/issues/1620), [#1177](https://github.com/rafaelvcaetano/melonDS-android/issues/1177), [#1296](https://github.com/rafaelvcaetano/melonDS-android/issues/1296). Usare hash ROM supportato, salvataggio riproducibile e achievement preciso; tracciare campionamento memoria, trigger e invio rete separando offline/autenticazione da riconoscimento locale. Prova richiesta: Stesso trigger prima/dopo riproducibile, con account e definizione achievement controllati.

6. **Coordinate e temporizzazione touch** — [#1424](https://github.com/rafaelvcaetano/melonDS-android/issues/1424), [#1211](https://github.com/rafaelvcaetano/melonDS-android/issues/1211), [#1141](https://github.com/rafaelvcaetano/melonDS-android/issues/1141), [lib #8](https://github.com/rafaelvcaetano/melonDS-android-lib/issues/8). Prima separare offset geometrico Thor con keep DS aspect ratio da impulsi touch sub-frame e latenza. Misurare coordinate trasformate e cronologia ACTION_DOWN/MOVE/UP con scene Rhythm Heaven. Prova richiesta: Test geometrici e temporali più confronto touch sul dispositivo con layout/risoluzione espliciti.

7. **Distorsione audio e codec Bluetooth** — [#1583](https://github.com/rafaelvcaetano/melonDS-android/issues/1583), [#1645](https://github.com/rafaelvcaetano/melonDS-android/issues/1645), [#1246](https://github.com/rafaelvcaetano/melonDS-android/issues/1246), [#1644](https://github.com/rafaelvcaetano/melonDS-android/issues/1644). Registrare stream/timing su speaker e headset, bitrate e codec; distinguere crackle persistente/bassi/interpolazione dal solo stream perso dopo resume. Nessun AudioStretch nuovo va assunto. Prova richiesta: Campioni e tracce di underrun/ampiezza riproducibili, con matrice codec e pause/resume.

8. **Compatibilità JIT su Android ARM64** — [#751](https://github.com/rafaelvcaetano/melonDS-android/issues/751), [#579](https://github.com/rafaelvcaetano/melonDS-android/issues/579), [#1283](https://github.com/rafaelvcaetano/melonDS-android/issues/1283), [#1392](https://github.com/rafaelvcaetano/melonDS-android/issues/1392). Confrontare interprete/JIT e renderer su identica build Android, ROM hash e SRAM/stato per AiRace, Super Mario 256 e hack Pokémon. Identificare anche formato stato e header hack. Prova richiesta: Riproduzione Android nativa e regressione specifica; benchmark o differenziale macOS non sufficiente.

9. **Librerie ROM grandi e costo dei provider** — [#1519](https://github.com/rafaelvcaetano/melonDS-android/issues/1519), [#1378](https://github.com/rafaelvcaetano/melonDS-android/issues/1378), [#540](https://github.com/rafaelvcaetano/melonDS-android/issues/540), [#1481](https://github.com/rafaelvcaetano/melonDS-android/issues/1481), [#1281](https://github.com/rafaelvcaetano/melonDS-android/issues/1281). Separare librerie NDS pure da ZIP/7z; acquisire heap, crash Java/native, decoder e tempi del provider SD al primo avvio. Isolare il riavvio Pokémon Black 2 dalla scansione. Prova richiesta: Libreria riproducibile senza crash e misure memoria/latency su dispositivi Android pertinenti.

10. **Stabilità WFC e caso C-Gear** — [#1301](https://github.com/rafaelvcaetano/melonDS-android/issues/1301), [#1467](https://github.com/rafaelvcaetano/melonDS-android/issues/1467), [#1507](https://github.com/rafaelvcaetano/melonDS-android/issues/1507), [#1634](https://github.com/rafaelvcaetano/melonDS-android/issues/1634), [#1622](https://github.com/rafaelvcaetano/melonDS-android/issues/1622), [lib #2](https://github.com/rafaelvcaetano/melonDS-android-lib/issues/2). Raccogliere log protocolli/server e riproduzioni con peer noto; distinguere AP search, WFC remoto, Game Sync e wireless locale. Il core aggiornato e i DNS configurabili non sono prova di compatibilità completa. Prova richiesta: Sessioni Android ripetibili con esito atteso, senza inferire risoluzione dal solo test di connessione.

## JIT e metodo di verifica

La [ricerca mirata di issue e PR JIT](KNOWN-JIT-ISSUES.md) distingue la corrispondenza esatta del teardown (#2537 del core condiviso) dai sintomi generici dei giochi Android. Il difetto DTCM è stato riprodotto separatamente su Android 15 ARM64 con programma sintetico, isolando fast memory e ottimizzazione dei literal. Non viene presentato come soluzione dimostrata per AiRace, Super Mario 256 o ogni romhack.

Il metodo è: leggere segnalazione e discussione pertinente; fissare ambiente, ABI, versione, hash e passi; confrontare interprete/JIT o il percorso di controllo appropriato; isolare una causa; osservare la regressione fallire; applicare una correzione minima; ripetere la stessa prova su Android e i controlli CI; mantenere espliciti i casi non riprodotti. La [distinzione ufficiale fra test locali e su Android](https://developer.android.com/training/testing/fundamentals) guida la separazione dei risultati. La compilazione nativa usa la [toolchain CMake NDK](https://developer.android.com/ndk/guides/cmake); le pagine da [4/16 KiB](https://developer.android.com/guide/practices/page-sizes) sono una variabile di prova, non una supposizione.

Le prove sintetiche del core non coprono driver grafici, audio Bluetooth, input fisico, SAF o geometria dual-display della Thor. I precedenti differenziali specifici macOS restano fuori dalla certificazione Android. Nessuna nuova PR viene pubblicata.

## Elenco completo per categoria

### Audio e microfono

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1679](https://github.com/rafaelvcaetano/melonDS-android/issues/1679) Mic too loud, Need Microphone volume slider. | Candidato concreto | baseline:E02 |
| [#1645](https://github.com/rafaelvcaetano/melonDS-android/issues/1645) Audio distortion/crackling on low frequencies (bass) on Nothing Phone 4a Pro (Nothing OS 4.1 / Android 16) | Da indagare | baseline:E11 |
| [#1644](https://github.com/rafaelvcaetano/melonDS-android/issues/1644) No audio when switching to a different app and coming back (headphones) | Parziale | baseline:E11 |
| [#1635](https://github.com/rafaelvcaetano/melonDS-android/issues/1635) MelonDS Nightly Audio issue | Parziale | baseline:E11 |
| [#1602](https://github.com/rafaelvcaetano/melonDS-android/issues/1602) New core destroys fast forward audio | Parziale | baseline:E11 |
| [#1583](https://github.com/rafaelvcaetano/melonDS-android/issues/1583) Latest Nightly build removes Audio Latency and now Audio cracking cannot be fixed | Da indagare | baseline:E11 |
| [#1468](https://github.com/rafaelvcaetano/melonDS-android/issues/1468) Sometimes gameplay becomes choppy, and micro stutters in audio | Da indagare | baseline:E11, baseline:E14 |
| [#1285](https://github.com/rafaelvcaetano/melonDS-android/issues/1285) Microphone Not Working | Da indagare | baseline:E02 |
| [#1246](https://github.com/rafaelvcaetano/melonDS-android/issues/1246) [Bug] Changing audio settings causes lag | Da indagare | baseline:E11 |
| [#1014](https://github.com/rafaelvcaetano/melonDS-android/issues/1014) Microphone is linked to the touch screen by default | Da indagare | baseline:E02, baseline:E06 |
| [#524](https://github.com/rafaelvcaetano/melonDS-android/issues/524) The audio of the intro of Pokemon Black is out of sync with the scene. | Da indagare | baseline:E11, baseline:E22 |
| [#271](https://github.com/rafaelvcaetano/melonDS-android/issues/271) Add a sound button to turn on or off sound in the menu | Funzionalità/intervento più ampio | baseline:E11 |

### Grafica e display

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1663](https://github.com/rafaelvcaetano/melonDS-android/issues/1663) Issue with casting. | Candidato concreto | baseline:E20 |
| [#1641](https://github.com/rafaelvcaetano/melonDS-android/issues/1641) [Feature Request] Toggle External Display | Candidato concreto | baseline:E20 |
| [#1619](https://github.com/rafaelvcaetano/melonDS-android/issues/1619) [Feature Request] Flexible HDMI options: Mirroring toggle and external layout customization | Parziale | baseline:E20 |
| [#1612](https://github.com/rafaelvcaetano/melonDS-android/issues/1612) Top and Botton Screen wrong | Da indagare | baseline:E20 |
| [#1610](https://github.com/rafaelvcaetano/melonDS-android/issues/1610) Issue with only one screen showing up | Candidato concreto | baseline:E20 |
| [#1556](https://github.com/rafaelvcaetano/melonDS-android/issues/1556) Texture seams worse than other emulator | Da indagare | baseline:E15 |
| [#1550](https://github.com/rafaelvcaetano/melonDS-android/issues/1550) Feature Request, add option to "rotate screen 90°" | Funzionalità/intervento più ampio | baseline:E20 |
| [#1529](https://github.com/rafaelvcaetano/melonDS-android/issues/1529) GTA: Chinatown Wars - Cars pop in randomly | Da indagare | baseline:E22 |
| [#1512](https://github.com/rafaelvcaetano/melonDS-android/issues/1512) Kirby squeak squad bug | Da indagare | baseline:E15, baseline:E22 |
| [#1508](https://github.com/rafaelvcaetano/melonDS-android/issues/1508) Graphics bug in pokemon hearthgold | Da indagare | baseline:E15 |
| [#1495](https://github.com/rafaelvcaetano/melonDS-android/issues/1495) Add more filters like zfast_lcd | Funzionalità/intervento più ampio | baseline:E15 |
| [#1494](https://github.com/rafaelvcaetano/melonDS-android/issues/1494) Possible to expose "Improved polygon splitting" setting? This is needed to prevent openGL rendering errors in Pokemon Diamond, Pearl, Platinum, Heartgold and Soul Silver | Parziale | baseline:E15 |
| [#1490](https://github.com/rafaelvcaetano/melonDS-android/issues/1490) OpenGL Issue | Da indagare | baseline:E15 |
| [#1485](https://github.com/rafaelvcaetano/melonDS-android/issues/1485) LCD filter is too dark | Parziale | baseline:E15 |
| [#1476](https://github.com/rafaelvcaetano/melonDS-android/issues/1476) Top of the screen is inaccurate for layouts when unfolded | Da indagare | baseline:E20 |
| [#1454](https://github.com/rafaelvcaetano/melonDS-android/issues/1454) [Nightly] Screen rotation bug | Parziale | baseline:E06, baseline:E20 |
| [#1449](https://github.com/rafaelvcaetano/melonDS-android/issues/1449) Opengl textures broken | Da indagare | baseline:E15 |
| [#1431](https://github.com/rafaelvcaetano/melonDS-android/issues/1431) Glitches appearing while playing any game | Da indagare | baseline:E15 |
| [#1404](https://github.com/rafaelvcaetano/melonDS-android/issues/1404) GTA Chinatown Wars. Vehicles spawn in front of the player (Pop In) | Da indagare | baseline:E22 |
| [#1396](https://github.com/rafaelvcaetano/melonDS-android/issues/1396) Tiling issue in Zelda Spirit Tracks with OpenGL renderer. | Da indagare | baseline:E15 |
| [#1390](https://github.com/rafaelvcaetano/melonDS-android/issues/1390) New super Mario bros graphic glitch Xiaomi mi 10 t pro | Da indagare | baseline:E15 |
| [#1389](https://github.com/rafaelvcaetano/melonDS-android/issues/1389) Feature Requests | Parziale | baseline:E15, baseline:E20 |
| [#1324](https://github.com/rafaelvcaetano/melonDS-android/issues/1324) And also LCD  filter is too dark, I have a lighter filter if you want | Parziale | baseline:E15 |
| [#1303](https://github.com/rafaelvcaetano/melonDS-android/issues/1303) [Suggest] Hybrid screen | Funzionalità/intervento più ampio | baseline:E20 |
| [#1282](https://github.com/rafaelvcaetano/melonDS-android/issues/1282) OpenGL renderer bug | Da indagare | baseline:E15 |
| [#1280](https://github.com/rafaelvcaetano/melonDS-android/issues/1280) MelonDS Vulkan Support? | Funzionalità/intervento più ampio | baseline:E15 |
| [#1265](https://github.com/rafaelvcaetano/melonDS-android/issues/1265) A glitch on GTA Chinatown Wars that doesn't supposed to happen. | Da indagare | baseline:E22 |
| [#1179](https://github.com/rafaelvcaetano/melonDS-android/issues/1179) [Bug] Changing the screen's resolution warps the emulator's layout. | Da indagare | baseline:E20 |
| [#1084](https://github.com/rafaelvcaetano/melonDS-android/issues/1084) zelda spirit tracks floor rendering bug nightly dc218bf | Da indagare | baseline:E15 |
| [#788](https://github.com/rafaelvcaetano/melonDS-android/issues/788) [Bug] Default layout is messed up on Galaxy Fold in portrait orientation | Da indagare | baseline:E20 |
| [#261](https://github.com/rafaelvcaetano/melonDS-android/issues/261) Stretched images screens(layouts) | Funzionalità/intervento più ampio | baseline:E20 |

### Touch e controller

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1579](https://github.com/rafaelvcaetano/melonDS-android/issues/1579) Add a turbo input! | Funzionalità/intervento più ampio | baseline:E18 |
| [#1547](https://github.com/rafaelvcaetano/melonDS-android/issues/1547) Bug action button | Coperto nel codice | baseline:E06 |
| [#1543](https://github.com/rafaelvcaetano/melonDS-android/issues/1543) Request adding combo button or additional button | Parziale | baseline:E06, baseline:E20 |
| [#1446](https://github.com/rafaelvcaetano/melonDS-android/issues/1446) The touch buttons (A B X Y) do not work if you try to press more than one button at the same time. | Coperto nel codice | baseline:E06 |
| [#1437](https://github.com/rafaelvcaetano/melonDS-android/issues/1437) Joycon mapping not working | Funzionalità/intervento più ampio | baseline:E18 |
| [#1424](https://github.com/rafaelvcaetano/melonDS-android/issues/1424) Touch input high latency | Da indagare | baseline:E06, baseline:E14 |
| [#1365](https://github.com/rafaelvcaetano/melonDS-android/issues/1365) Make the touchscreen's A, B, X and Y buttons separate buttons. | Parziale | baseline:E06, baseline:E20 |
| [#1364](https://github.com/rafaelvcaetano/melonDS-android/issues/1364) Button hitboxes are too big? | Parziale | baseline:E06 |
| [#1348](https://github.com/rafaelvcaetano/melonDS-android/issues/1348) USB controller support for Android 14 | Da indagare | baseline:E18 |
| [#1323](https://github.com/rafaelvcaetano/melonDS-android/issues/1323) Add Disable Diagonal buttons in keypad and vibrate is different on latest phones | Funzionalità/intervento più ampio | baseline:E06 |
| [#1247](https://github.com/rafaelvcaetano/melonDS-android/issues/1247) Multi touch issue | Coperto nel codice | baseline:E06 |
| [#1223](https://github.com/rafaelvcaetano/melonDS-android/issues/1223) Can't press virtual A and B at the same time | Coperto nel codice | baseline:E06 |
| [#1211](https://github.com/rafaelvcaetano/melonDS-android/issues/1211) Quick taps don't register | Da indagare | baseline:E06, baseline:E14 |
| [#1171](https://github.com/rafaelvcaetano/melonDS-android/issues/1171) Feature Request: Switch Layout Hotkey Binding | Funzionalità/intervento più ampio | baseline:E18 |
| [#1141](https://github.com/rafaelvcaetano/melonDS-android/issues/1141) Slide Inputs are Buggy when They are Done Too Quickly. | Da indagare | baseline:E06, baseline:E14 |
| [#973](https://github.com/rafaelvcaetano/melonDS-android/issues/973) Custom ABXY buttons | Parziale | baseline:E06, baseline:E20 |
| [#578](https://github.com/rafaelvcaetano/melonDS-android/issues/578) ABXY Buttons not working properly | Coperto nel codice | baseline:E06 |
| [#548](https://github.com/rafaelvcaetano/melonDS-android/issues/548) Não consigo apertar dois botões ao mesmo tempo,como o Y e o B | Coperto nel codice | baseline:E06 |
| [#188](https://github.com/rafaelvcaetano/melonDS-android/issues/188) [Future Request] Added PPSSPP Type Buttons | Funzionalità/intervento più ampio | baseline:E06, baseline:E20 |
| [lib #8](https://github.com/rafaelvcaetano/melonDS-android-lib/issues/8) Touch Screen Inaccuracy | Da indagare | baseline:E06, baseline:E20 |

### Salvataggi, stati e rewind

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1655](https://github.com/rafaelvcaetano/melonDS-android/issues/1655) Rewind frame selector top of screen instead of bottom | Coperto nel codice | baseline:E04 |
| [#1654](https://github.com/rafaelvcaetano/melonDS-android/issues/1654) [BUG] Jagged Alliance DS has broken sprites when loading a mid-mission Quick Save. | Da indagare | baseline:E16, baseline:E17 |
| [#1605](https://github.com/rafaelvcaetano/melonDS-android/issues/1605) Save States on Mobile | Funzionalità/intervento più ampio | baseline:E17 |
| [#1594](https://github.com/rafaelvcaetano/melonDS-android/issues/1594) Picross DS save's progress mysteriously wiped out | Parziale | baseline:E16 |
| [#1573](https://github.com/rafaelvcaetano/melonDS-android/issues/1573) Feature Request: Multiple saves / Save Number Selection | Funzionalità/intervento più ampio | baseline:E16, baseline:E17 |
| [#1325](https://github.com/rafaelvcaetano/melonDS-android/issues/1325) save extension select .sav or .srm | Funzionalità/intervento più ampio | baseline:E16 |
| [#793](https://github.com/rafaelvcaetano/melonDS-android/issues/793) Improvements options at Game Menu | Parziale | baseline:E17 |
| [#710](https://github.com/rafaelvcaetano/melonDS-android/issues/710) Overwriting Save States does not update timestamp | Da indagare | baseline:E17 |
| [#698](https://github.com/rafaelvcaetano/melonDS-android/issues/698) The max of fps is 17 in gameplay, i can put it in 30 or 60 fps in game? | Parziale | baseline:E16, baseline:E24 |
| [#497](https://github.com/rafaelvcaetano/melonDS-android/issues/497) Loadstate not working (dsi)(asphalt 4) | Parziale | baseline:E17 |
| [#282](https://github.com/rafaelvcaetano/melonDS-android/issues/282) (Quick save and load)Make a save button input and 'continue last save' feature | Parziale | baseline:E18 |
| [#277](https://github.com/rafaelvcaetano/melonDS-android/issues/277) Google Drive Backup | Funzionalità/intervento più ampio | baseline:E10, baseline:E16 |
| [#276](https://github.com/rafaelvcaetano/melonDS-android/issues/276) Auto Save feature | Funzionalità/intervento più ampio | baseline:E17, baseline:E18 |

### File ROM, SAF e archivi

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1660](https://github.com/rafaelvcaetano/melonDS-android/issues/1660) can't browse ROM files in Android 37 | Da indagare | baseline:E08, baseline:E13 |
| [#1623](https://github.com/rafaelvcaetano/melonDS-android/issues/1623) Firestick | Funzionalità/intervento più ampio | baseline:E08 |
| [#1575](https://github.com/rafaelvcaetano/melonDS-android/issues/1575) Multiple directories | Coperto nel codice | baseline:E08 |
| [#1565](https://github.com/rafaelvcaetano/melonDS-android/issues/1565) I don't have a file browser builtin (AndroidTV) | Funzionalità/intervento più ampio | baseline:E08 |
| [#1541](https://github.com/rafaelvcaetano/melonDS-android/issues/1541) Requesting support for .rar files | Funzionalità/intervento più ampio | baseline:E24 |
| [#1519](https://github.com/rafaelvcaetano/melonDS-android/issues/1519) App crashing when trying to load large libraries on Ayn Thor | Parziale | baseline:E13 |
| [#1481](https://github.com/rafaelvcaetano/melonDS-android/issues/1481) Loading roms inside external storage is slow at first time | Da indagare | baseline:E13, baseline:E24 |
| [#1388](https://github.com/rafaelvcaetano/melonDS-android/issues/1388) Some games not showing up | Da indagare | baseline:E08, baseline:E09, baseline:E13 |
| [#1378](https://github.com/rafaelvcaetano/melonDS-android/issues/1378) Melonds crashes when setting up directory | Parziale | baseline:E13 |
| [#1281](https://github.com/rafaelvcaetano/melonDS-android/issues/1281) Crashes when scrolling down roms.  | Parziale | baseline:E13 |
| [#1217](https://github.com/rafaelvcaetano/melonDS-android/issues/1217) Zip Compression methods | Funzionalità/intervento più ampio | baseline:E24 |
| [#685](https://github.com/rafaelvcaetano/melonDS-android/issues/685) No puedo abrir un rom en una app dual | Da indagare | baseline:E16, baseline:E24 |
| [#540](https://github.com/rafaelvcaetano/melonDS-android/issues/540) App crashes when scanning for roms and restarts itself after launching Pokémon Black 2 | Parziale | baseline:E13, baseline:E22 |
| [#273](https://github.com/rafaelvcaetano/melonDS-android/issues/273) Add zip support for gba roms | Funzionalità/intervento più ampio | baseline:E24 |

### Avvio, BIOS, firmware e DSi

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1678](https://github.com/rafaelvcaetano/melonDS-android/issues/1678) Local reference leak in MelonDSiNand.listTitles | Coperto nel codice | baseline:E01 |
| [#1651](https://github.com/rafaelvcaetano/melonDS-android/issues/1651) DSi Manager allows installing DSiWare beyond the DSi logical storage limit | Candidato concreto | baseline:E01 |
| [#1649](https://github.com/rafaelvcaetano/melonDS-android/issues/1649) [BUG]: melonDS will corrupt the DS BIOS. | Da indagare | baseline:E16, baseline:E25 |
| [#1581](https://github.com/rafaelvcaetano/melonDS-android/issues/1581) Bug. DSi White screening games. MelonDS nightly | Da indagare | baseline:E22 |
| [#1475](https://github.com/rafaelvcaetano/melonDS-android/issues/1475) Powder isn't working | Da indagare | baseline:E22 |
| [#1461](https://github.com/rafaelvcaetano/melonDS-android/issues/1461) can't enable sd card | Funzionalità/intervento più ampio | baseline:E22 |
| [#1426](https://github.com/rafaelvcaetano/melonDS-android/issues/1426) When I open a ROM or BIOS on the Nintendo DS it crashes after a few seconds | Da indagare | baseline:E09, baseline:E22 |
| [#1415](https://github.com/rafaelvcaetano/melonDS-android/issues/1415) Castle of Magic Dsiware take a photo mode problem | Da indagare | baseline:E27 |
| [#1392](https://github.com/rafaelvcaetano/melonDS-android/issues/1392) Pokemon Black Blaze won't run | Da indagare | baseline:E22 |
| [#1379](https://github.com/rafaelvcaetano/melonDS-android/issues/1379) new update problem | Da indagare | baseline:E09, baseline:E22 |
| [#1376](https://github.com/rafaelvcaetano/melonDS-android/issues/1376) Camera app keeps crashing | Parziale | baseline:E27 |
| [#1350](https://github.com/rafaelvcaetano/melonDS-android/issues/1350) Dldi support | Funzionalità/intervento più ampio | baseline:E22 |
| [#1329](https://github.com/rafaelvcaetano/melonDS-android/issues/1329) I cannot use the GBA slot | Da indagare | baseline:E22, baseline:E24 |
| [#1321](https://github.com/rafaelvcaetano/melonDS-android/issues/1321) app says it requires bios | Parziale | baseline:E09 |
| [#1314](https://github.com/rafaelvcaetano/melonDS-android/issues/1314) Melonds Emulator dropping at 4 fps when I start Ugoku Memo Chou. | Da indagare | baseline:E27 |
| [#1299](https://github.com/rafaelvcaetano/melonDS-android/issues/1299) Firmware  | Parziale | baseline:E09 |
| [#1294](https://github.com/rafaelvcaetano/melonDS-android/issues/1294) pokemon moon black 2 not working well on dsi mode | Da indagare | baseline:E22 |
| [#1288](https://github.com/rafaelvcaetano/melonDS-android/issues/1288) DSi Camera crash on Redmi 9T with AOSPA 13 | Parziale | baseline:E27 |
| [#1283](https://github.com/rafaelvcaetano/melonDS-android/issues/1283) Pokemon Blaze Black / Volt White boots a white screen | Da indagare | baseline:E22 |
| [#1270](https://github.com/rafaelvcaetano/melonDS-android/issues/1270) Game crashes when i load a rom  | Da indagare | baseline:E09, baseline:E22 |
| [#1233](https://github.com/rafaelvcaetano/melonDS-android/issues/1233) Camera Not Working | Parziale | baseline:E27 |
| [#1215](https://github.com/rafaelvcaetano/melonDS-android/issues/1215) Harvest Moon Ds Cute GBA-connect issues  | Da indagare | baseline:E22, baseline:E24 |
| [#1188](https://github.com/rafaelvcaetano/melonDS-android/issues/1188) Nintendo support sound and freeze while white. | Da indagare | baseline:E09, baseline:E22 |
| [#1160](https://github.com/rafaelvcaetano/melonDS-android/issues/1160) Pokemon | Parziale | baseline:E09 |
| [#1159](https://github.com/rafaelvcaetano/melonDS-android/issues/1159) Pokemon ds | Parziale | baseline:E09 |
| [#1099](https://github.com/rafaelvcaetano/melonDS-android/issues/1099) DSi Ware Manager | Da indagare | baseline:E01 |
| [#1047](https://github.com/rafaelvcaetano/melonDS-android/issues/1047) Trying to boot up a DSiware game on melonds for android on chromebook | Parziale | baseline:E01, baseline:E09 |
| [#1000](https://github.com/rafaelvcaetano/melonDS-android/issues/1000) Roms won't open android 0.9.5 | Da indagare | baseline:E09, baseline:E22 |
| [#996](https://github.com/rafaelvcaetano/melonDS-android/issues/996) Checking BIOS Directory not working correctly | Parziale | baseline:E09 |
| [#923](https://github.com/rafaelvcaetano/melonDS-android/issues/923) it doesn't load nand for dsi | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#896](https://github.com/rafaelvcaetano/melonDS-android/issues/896) dsi bootfrimware show correpted game icon and cant run it | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#849](https://github.com/rafaelvcaetano/melonDS-android/issues/849) Ferrari GT Evolution bug | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#796](https://github.com/rafaelvcaetano/melonDS-android/issues/796) Pokemon White 2 Error Load | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#757](https://github.com/rafaelvcaetano/melonDS-android/issues/757) Nitendo dsiware instrument tuner not working  | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#642](https://github.com/rafaelvcaetano/melonDS-android/issues/642) DSiware (Black Screen problem) with some games | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#630](https://github.com/rafaelvcaetano/melonDS-android/issues/630) How can i run homebrews on the emulator | Funzionalità/intervento più ampio | baseline:E22 |
| [#627](https://github.com/rafaelvcaetano/melonDS-android/issues/627) When I select a GBA rom it crashes. | Da indagare | baseline:E22, baseline:E24 |
| [#571](https://github.com/rafaelvcaetano/melonDS-android/issues/571) Fps problem in dsi | Da indagare | baseline:E27 |
| [#532](https://github.com/rafaelvcaetano/melonDS-android/issues/532) App crash on custom firmware screen | Da indagare | baseline:E01, baseline:E09, baseline:E22 |
| [#287](https://github.com/rafaelvcaetano/melonDS-android/issues/287) DSi Mode Issues | Parziale | baseline:E27, baseline:E22 |
| [#174](https://github.com/rafaelvcaetano/melonDS-android/issues/174) Not able to open non official Roms | Da indagare | baseline:E22 |
| [#44](https://github.com/rafaelvcaetano/melonDS-android/issues/44) I play dsi games but show error | Parziale | baseline:E01, baseline:E09 |
| [lib #9](https://github.com/rafaelvcaetano/melonDS-android-lib/issues/9) No sd card support for dsi | Funzionalità/intervento più ampio | baseline:E22 |

### JIT, prestazioni e compatibilità

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1630](https://github.com/rafaelvcaetano/melonDS-android/issues/1630) Crash on Tomodachi Collection (English Patch) | Da indagare | baseline:E17, baseline:E22 |
| [#1562](https://github.com/rafaelvcaetano/melonDS-android/issues/1562) cutscenes 20 FPS AG when put on Opengl with x5 as resolution | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1548](https://github.com/rafaelvcaetano/melonDS-android/issues/1548) Stutter/frame pacing issue in software and OpenGL. | Parziale | baseline:E14 |
| [#1537](https://github.com/rafaelvcaetano/melonDS-android/issues/1537) Intermittent stutter in OpenGL every couple of seconds that lasts a few seconds | Parziale | baseline:E14 |
| [#1522](https://github.com/rafaelvcaetano/melonDS-android/issues/1522) Poor performance in Pokemon Black when choosing starter Pokemon at 3x upscale | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1513](https://github.com/rafaelvcaetano/melonDS-android/issues/1513) Feature request:low accuracy/high-speed mode for low-end 32-bit phones | Funzionalità/intervento più ampio | baseline:E22 |
| [#1509](https://github.com/rafaelvcaetano/melonDS-android/issues/1509) Poor OpenGL Performance on Exynos 2400 | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1507](https://github.com/rafaelvcaetano/melonDS-android/issues/1507) Emulator Nightly build Crashing. | Da indagare | baseline:E11, baseline:E22, baseline:E25 |
| [#1492](https://github.com/rafaelvcaetano/melonDS-android/issues/1492) Rune Factory 3 heavy slowdown.  | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1444](https://github.com/rafaelvcaetano/melonDS-android/issues/1444) Slowness of games and shaky screen | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1429](https://github.com/rafaelvcaetano/melonDS-android/issues/1429) Crash and fluctuations in Pokemon B/W | Da indagare | baseline:E22 |
| [#1421](https://github.com/rafaelvcaetano/melonDS-android/issues/1421) Fast forward isn't fast | Da indagare | baseline:E18, baseline:E22 |
| [#1418](https://github.com/rafaelvcaetano/melonDS-android/issues/1418) The World Ends With You, both undub and (U) | Da indagare | baseline:E22 |
| [#1412](https://github.com/rafaelvcaetano/melonDS-android/issues/1412) Otimização Software/OpenGL/Vulkan | Funzionalità/intervento più ampio | baseline:E15 |
| [#1406](https://github.com/rafaelvcaetano/melonDS-android/issues/1406) Performance got worse after last update | Da indagare | baseline:E22 |
| [#1395](https://github.com/rafaelvcaetano/melonDS-android/issues/1395) Some games run a little slow | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1393](https://github.com/rafaelvcaetano/melonDS-android/issues/1393) Golden Sun Dark Dawn | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1366](https://github.com/rafaelvcaetano/melonDS-android/issues/1366) OpenGL upscale issue  | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1275](https://github.com/rafaelvcaetano/melonDS-android/issues/1275) OpenGL Performance issue and Filters  | Parziale | baseline:E15 |
| [#1252](https://github.com/rafaelvcaetano/melonDS-android/issues/1252) Poor performance on Exynos 850(Mali) gpu phones  | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1241](https://github.com/rafaelvcaetano/melonDS-android/issues/1241) fps drop in pokemon games. | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1151](https://github.com/rafaelvcaetano/melonDS-android/issues/1151) Bad performance on 3D games | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1088](https://github.com/rafaelvcaetano/melonDS-android/issues/1088) Lag on S22 Ultra | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#1085](https://github.com/rafaelvcaetano/melonDS-android/issues/1085) zelda spirit tracks random RAM curruption nightly dc218bf | Da indagare | baseline:E06, baseline:E22 |
| [#921](https://github.com/rafaelvcaetano/melonDS-android/issues/921) Choppy audio and bad framerate in games | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#813](https://github.com/rafaelvcaetano/melonDS-android/issues/813) This video game ROM Nintendo DS it is slowing and crashing the emulator for phone because of what?... cell phone weak? | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#751](https://github.com/rafaelvcaetano/melonDS-android/issues/751) Issues with AiRace games with JIT enabled | Parziale | baseline:E22 |
| [#712](https://github.com/rafaelvcaetano/melonDS-android/issues/712) Issue with Rhythm Heaven | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#689](https://github.com/rafaelvcaetano/melonDS-android/issues/689) Fast forward isnt working | Da indagare | baseline:E18, baseline:E22 |
| [#677](https://github.com/rafaelvcaetano/melonDS-android/issues/677) Fast forward not working | Da indagare | baseline:E18, baseline:E22 |
| [#672](https://github.com/rafaelvcaetano/melonDS-android/issues/672) to change the compiler is possible? (sorry my english) | Funzionalità/intervento più ampio | baseline:E22 |
| [#579](https://github.com/rafaelvcaetano/melonDS-android/issues/579) Super Mario 256 JIT Crash | Parziale | baseline:E22 |
| [#491](https://github.com/rafaelvcaetano/melonDS-android/issues/491) Low fps in ds(not dsi) | Da indagare | baseline:E14, baseline:E15, baseline:E22 |
| [#397](https://github.com/rafaelvcaetano/melonDS-android/issues/397) Jit for 32 bit | Funzionalità/intervento più ampio | baseline:E22 |
| [#311](https://github.com/rafaelvcaetano/melonDS-android/issues/311) JIT Isn't available on my smartphone | Funzionalità/intervento più ampio | baseline:E22 |
| [#173](https://github.com/rafaelvcaetano/melonDS-android/issues/173) Frame skip | Funzionalità/intervento più ampio | baseline:E22 |
| [lib #2](https://github.com/rafaelvcaetano/melonDS-android-lib/issues/2) Update melonDS version | Coperto nel codice | current:core_version, baseline:E22 |

### Rete e multiplayer

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1662](https://github.com/rafaelvcaetano/melonDS-android/issues/1662) [Suggestion] Wifi-direct implementation | Funzionalità/intervento più ampio | baseline:E22 |
| [#1661](https://github.com/rafaelvcaetano/melonDS-android/issues/1661) [Question] : Is it possible to play local multiplayer by connecting 2 androids via USB-C or ethernet? | Funzionalità/intervento più ampio | baseline:E22 |
| [#1656](https://github.com/rafaelvcaetano/melonDS-android/issues/1656) [Bug]: melonDS has networking issues (LAN multiplayer attempt) | Funzionalità/intervento più ampio | baseline:E22 |
| [#1653](https://github.com/rafaelvcaetano/melonDS-android/issues/1653) Disconnecting | Da indagare | baseline:E25 |
| [#1634](https://github.com/rafaelvcaetano/melonDS-android/issues/1634) Working Nintendo WFC settings area and trying to connect to WiFi servers after. | Da indagare | baseline:E25 |
| [#1622](https://github.com/rafaelvcaetano/melonDS-android/issues/1622) Game Sync issue on Pokemon Black while online | Da indagare | baseline:E25 |
| [#1621](https://github.com/rafaelvcaetano/melonDS-android/issues/1621) Sudomemo - DSi Wifi/Internet settings being reset after restarting | Da indagare | baseline:E25 |
| [#1597](https://github.com/rafaelvcaetano/melonDS-android/issues/1597) Trying to work Nintendo wfc WiFi area | Da indagare | baseline:E25 |
| [#1493](https://github.com/rafaelvcaetano/melonDS-android/issues/1493) LAN multiplayer support on Android port | Funzionalità/intervento più ampio | baseline:E22 |
| [#1467](https://github.com/rafaelvcaetano/melonDS-android/issues/1467) Not able to go to other towns in animal crossing wild world | Da indagare | baseline:E25 |
| [#1460](https://github.com/rafaelvcaetano/melonDS-android/issues/1460) YO hhomie bring DS download play please | Funzionalità/intervento più ampio | baseline:E22 |
| [#1459](https://github.com/rafaelvcaetano/melonDS-android/issues/1459) Add DS Downl;oad play so i can play with Homies | Funzionalità/intervento più ampio | baseline:E22 |
| [#1430](https://github.com/rafaelvcaetano/melonDS-android/issues/1430) wfcsettings.bin file location | Parziale | baseline:E25 |
| [#1317](https://github.com/rafaelvcaetano/melonDS-android/issues/1317) I hope to rejoin the local multiplayer game. | Funzionalità/intervento più ampio | baseline:E22 |
| [#1306](https://github.com/rafaelvcaetano/melonDS-android/issues/1306) Multiplayer issue in River City soccer hooligans  | Da indagare | baseline:E25 |
| [#1301](https://github.com/rafaelvcaetano/melonDS-android/issues/1301) WFC connection unreliable - drops after few minutes | Da indagare | baseline:E25 |
| [#1297](https://github.com/rafaelvcaetano/melonDS-android/issues/1297) Pokemon Platinum error | Da indagare | baseline:E25 |
| [#1231](https://github.com/rafaelvcaetano/melonDS-android/issues/1231) Online Multiplayer not working | Da indagare | baseline:E25 |
| [#1230](https://github.com/rafaelvcaetano/melonDS-android/issues/1230) Local Wireless doesn't work | Funzionalità/intervento più ampio | baseline:E22 |
| [#1043](https://github.com/rafaelvcaetano/melonDS-android/issues/1043) Can't go any further after I completed the catch minigame | Funzionalità/intervento più ampio | baseline:E22 |
| [#1030](https://github.com/rafaelvcaetano/melonDS-android/issues/1030) Melonds needs to have the DS Download play feature  | Funzionalità/intervento più ampio | baseline:E22 |
| [#951](https://github.com/rafaelvcaetano/melonDS-android/issues/951) Multiplayer for Android MelonDS | Funzionalità/intervento più ampio | baseline:E22 |
| [#912](https://github.com/rafaelvcaetano/melonDS-android/issues/912) Manually setting a custom MAC address | Parziale | baseline:E21 |
| [#745](https://github.com/rafaelvcaetano/melonDS-android/issues/745) Weird Wifi Behavior:Nintendo dsi wifi user agreement doesnt load | Da indagare | baseline:E25 |
| [#586](https://github.com/rafaelvcaetano/melonDS-android/issues/586) Bios issue? | Da indagare | baseline:E25 |
| [#433](https://github.com/rafaelvcaetano/melonDS-android/issues/433) Need IR(Infrared) Connection Support | Funzionalità/intervento più ampio | baseline:E22 |
| [#413](https://github.com/rafaelvcaetano/melonDS-android/issues/413) Game Crashed the Patcher | Funzionalità/intervento più ampio | baseline:E25 |
| [#412](https://github.com/rafaelvcaetano/melonDS-android/issues/412) Call of Duty - Black Ops (Wfc Patched) Communication Issue | Da indagare | baseline:E25 |
| [#331](https://github.com/rafaelvcaetano/melonDS-android/issues/331) Problema Grave Wi-fi | Da indagare | baseline:E25 |

### Cheat

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1631](https://github.com/rafaelvcaetano/melonDS-android/issues/1631) support usrcheat.dat cheat code library | Coperto nel codice | baseline:E05 |
| [#1584](https://github.com/rafaelvcaetano/melonDS-android/issues/1584) Load *.mch cheat file if exist | Funzionalità/intervento più ampio | baseline:E05 |
| [#1410](https://github.com/rafaelvcaetano/melonDS-android/issues/1410) Bugs that I found so far | Parziale | baseline:E05, baseline:E20 |
| [#269](https://github.com/rafaelvcaetano/melonDS-android/issues/269) Add custom cheats and add .dat file database support | Parziale | baseline:E05 |

### RetroAchievements

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1620](https://github.com/rafaelvcaetano/melonDS-android/issues/1620) RetroAchievements Not Triggering on 2.0 or 2.1 | Da indagare | baseline:E23 |
| [#1611](https://github.com/rafaelvcaetano/melonDS-android/issues/1611) Achievements seemingly can no longer be earned in normal play | Da indagare | baseline:E23 |
| [#1586](https://github.com/rafaelvcaetano/melonDS-android/issues/1586) Achievement unlock sound | Funzionalità/intervento più ampio | baseline:E23 |
| [#1405](https://github.com/rafaelvcaetano/melonDS-android/issues/1405) Can't Connect with RetroarchAchievements | Da indagare | baseline:E23 |
| [#1296](https://github.com/rafaelvcaetano/melonDS-android/issues/1296) Fire Emblem Shadow Dragon Retroachievements Not Working | Da indagare | baseline:E23 |
| [#1177](https://github.com/rafaelvcaetano/melonDS-android/issues/1177) A specific RetroAchievement unlocks via RetroArch with melonDS core, but not via melonDS standalone emulator. | Da indagare | baseline:E23 |

### Ciclo di vita e integrazione Android

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1665](https://github.com/rafaelvcaetano/melonDS-android/issues/1665) Resuming game from power off | Da indagare | baseline:E14, baseline:E20, current:lid, current:session_scope |
| [#1659](https://github.com/rafaelvcaetano/melonDS-android/issues/1659) [Bug]: melonDS prompts to reload instead of resuming the same running ROM | Coperto nel codice | baseline:E03 |
| [#1652](https://github.com/rafaelvcaetano/melonDS-android/issues/1652) [feat] Sync RTC to current time on lid open | Parziale | baseline:E12, current:lid |
| [#1618](https://github.com/rafaelvcaetano/melonDS-android/issues/1618) Problem with RTC | Parziale | baseline:E12 |
| [#1604](https://github.com/rafaelvcaetano/melonDS-android/issues/1604) DS clock doesn't sync with device clock | Parziale | baseline:E12 |
| [#1561](https://github.com/rafaelvcaetano/melonDS-android/issues/1561) Feature Request: Add simulated clock option | Funzionalità/intervento più ampio | baseline:E12 |
| [#1544](https://github.com/rafaelvcaetano/melonDS-android/issues/1544) Title: Frontend launch support — ROM not found when opened via frontend | Parziale | baseline:E03 |
| [#1456](https://github.com/rafaelvcaetano/melonDS-android/issues/1456) The issue was with the SAK path!! Once that’s fixed, Frontend works well up to Android 15. | Parziale | baseline:E03 |
| [#1359](https://github.com/rafaelvcaetano/melonDS-android/issues/1359) Melonds-android not in the "games" category? | Coperto nel codice | baseline:E07 |
| [#1358](https://github.com/rafaelvcaetano/melonDS-android/issues/1358) MelonDS - "Could not find ROM" ??? | Parziale | baseline:E03 |
| [#1330](https://github.com/rafaelvcaetano/melonDS-android/issues/1330) Add set date and time | Funzionalità/intervento più ampio | baseline:E12 |
| [#1185](https://github.com/rafaelvcaetano/melonDS-android/issues/1185) Reqest: sync real pysical battery to emulated ds/dsi battery | Funzionalità/intervento più ampio | baseline:E22 |
| [#1169](https://github.com/rafaelvcaetano/melonDS-android/issues/1169) MelonDS doesn’t work with the Retroid Launcher frontend | Parziale | baseline:E03 |
| [#928](https://github.com/rafaelvcaetano/melonDS-android/issues/928) Minimizing the emulator makes it reset the game on ChromeOS | Da indagare | baseline:E17, baseline:E20 |
| [#723](https://github.com/rafaelvcaetano/melonDS-android/issues/723) Berries not growing in Pokemon platinum | Parziale | baseline:E12, baseline:E21 |
| [#362](https://github.com/rafaelvcaetano/melonDS-android/issues/362) unable to send bug report | Da indagare | baseline:E24 |
| [#281](https://github.com/rafaelvcaetano/melonDS-android/issues/281) Custom Clock (date and time) feature | Funzionalità/intervento più ampio | baseline:E12 |
| [#267](https://github.com/rafaelvcaetano/melonDS-android/issues/267) Create rom shortcut and pin the rom on the launcher | Parziale | baseline:E19 |
| [#176](https://github.com/rafaelvcaetano/melonDS-android/issues/176) Passing of time in games | Parziale | baseline:E12, baseline:E21 |
| [lib #4](https://github.com/rafaelvcaetano/melonDS-android-lib/issues/4) Close Lid when locking android screen | Parziale | current:lid |

### Interfaccia e lingue

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1658](https://github.com/rafaelvcaetano/melonDS-android/issues/1658) Nice feature from MelonDualds. | Funzionalità/intervento più ampio | baseline:E08 |
| [#1558](https://github.com/rafaelvcaetano/melonDS-android/issues/1558) Display a small icon when fast forward is active | Funzionalità/intervento più ampio | baseline:E18 |
| [#1515](https://github.com/rafaelvcaetano/melonDS-android/issues/1515) The app gets laggy after first installed and restarted | Da indagare | baseline:E13, baseline:E20 |
| [#1503](https://github.com/rafaelvcaetano/melonDS-android/issues/1503) [Improvement] Add Grid View + Custom Cover Art | Funzionalità/intervento più ampio | baseline:E08 |
| [#1447](https://github.com/rafaelvcaetano/melonDS-android/issues/1447) Could you add a feature to directly input parameters for modifying screen layout and screen size? | Funzionalità/intervento più ampio | baseline:E10, baseline:E20 |
| [#1428](https://github.com/rafaelvcaetano/melonDS-android/issues/1428) [Features] Layout - direct size input and grid | Funzionalità/intervento più ampio | baseline:E10, baseline:E20 |
| [#1290](https://github.com/rafaelvcaetano/melonDS-android/issues/1290) [Suggest] Black theme | Funzionalità/intervento più ampio | baseline:E20 |
| [#1260](https://github.com/rafaelvcaetano/melonDS-android/issues/1260) [Feature Request] New Enhancment Ideas | Parziale | baseline:E15, baseline:E20 |
| [#1253](https://github.com/rafaelvcaetano/melonDS-android/issues/1253) Suggest adding Chinese | Coperto nel codice | baseline:E26, current:languages |
| [#1244](https://github.com/rafaelvcaetano/melonDS-android/issues/1244) UI & OnScreen GamePad can be better | Funzionalità/intervento più ampio | baseline:E10, baseline:E20 |
| [#272](https://github.com/rafaelvcaetano/melonDS-android/issues/272) Its very hard to edit the layouts when in split screen | Funzionalità/intervento più ampio | baseline:E20 |
| [#265](https://github.com/rafaelvcaetano/melonDS-android/issues/265) Add the layouts setting in menu bar | Funzionalità/intervento più ampio | baseline:E20 |
| [#264](https://github.com/rafaelvcaetano/melonDS-android/issues/264) Make a configuration file for the emulator | Coperto nel codice | baseline:E10 |

### Distribuzione e altre funzioni

| Issue | Stato nel nostro codice | Evidenze |
|---|---|---|
| [#1319](https://github.com/rafaelvcaetano/melonDS-android/issues/1319) GBA Add-On Android | Funzionalità/intervento più ampio | baseline:E22 |
| [#894](https://github.com/rafaelvcaetano/melonDS-android/issues/894) Suggestion: 8MB expansion support | Funzionalità/intervento più ampio | baseline:E22 |
| [#841](https://github.com/rafaelvcaetano/melonDS-android/issues/841) [Features] ADD These Features Please! | Parziale | baseline:E05, baseline:E15 |
| [#33](https://github.com/rafaelvcaetano/melonDS-android/issues/33) F-Droid | Funzionalità/intervento più ampio | baseline:E07 |

Le sigle delle evidenze e le motivazioni riga per riga sono risolte nel [catalogo JSON](ISSUE-CATEGORIES.json). Non sono stati cambiati gli stati delle issue su GitHub.
