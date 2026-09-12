# Schema delle richieste iniziali — stato al 12/09/2026 (ramo ventuno)
Legenda: FATTA = fa ciò che la richiesta chiedeva, verificato; A METÀ = corretta ma con lacune o difetti propri; REGRESSIONE = peggiora rispetto alla nightly; NON PROVATA; DA RITIRARE.

## PR app #1672
| n | Richiesta | Stato | Come | Cosa non va | Direzione |
|---|---|---|---|---|---|
| 1 | Multi-touch ABXY/croce (598b0c32) | A METÀ | actionMasked + tutti i pointer + CANCEL | stilo e 14 pulsanti singoli senza CANCEL; croce su+giù; allocazioni per evento; test tautologici | CANCEL ovunque, releaseAll al cambio layout, zero alloc, androidTest con MotionEvent veri |
| 2 | Crash touch <Android 11 (af4e3141, a30580e3) | FATTA | overload MotionEvent (API 21) | unbuffered dispatch per finestra; lint NewApi non in CI | tenere; lint locale |
| 3 | Salvataggio fallito segnalato (94710907, c1f24700) | A METÀ | ritentativo invece di marcare scritto; log | nessun avviso utente (accessori senza chiamanti); stageSaves duplicata; scrittura troncante; Thread non inizializzato | dire "ritentativo"; evento+toast; temp+rename; guardia |
| 4 | Audio fallisce all'avvio (294d2db2) | FATTA | check openStream prima dell'uso | race Oboe/JNI preesistente; nessun retry | tenere; mutex |
| 5 | Mute in ritardo (5ca9741c) | FATTA | confronto sul volume nuovo | setupAudio usa solo soundEnabled | stessa regola all'avvio |
| 6 | Filtri video fuori posto (b6a40642) | REGRESSIONE | dimensione texture reale per XBR/HQ/Quilez (giusto) | LCD e Scanlines scalano con la risoluzione → moiré ≥2x; 2 file morti; testo PR errato | ancorare LCD/Scanlines a 256×192; uniform |
| 7 | RTC allineato all'host (4 commit) | DA RITIRARE | polling per frame, solo avanti, soglia 1 s | maintainer vuole sync nel resume nativo (#1664); salta IRQ/allarmi; fallback indietro; loadState indietro; mktime per frame | ritirare; contribuire a #1664 |
| 8 | Messaggio BIOS console sbagliata (b968177a, ce2ba962, eff2bdd0) | FATTA | byte 0x1D del firmware, valori = core | firmware 128 KB verde ma non bootable; messaggio unico per 3 casi | distinguere i casi |
| 9 | Assistente BIOS guidato (7e946a90, 6d6952df) | DIFETTOSA | classifica per dimensione/byte, crea DS/DSi, copia | copia sul thread UI (ANR con nand 240 MB); parziali dichiarati validi; ambiguità morta; preferenza non rivalidata; zero test | coroutine+progresso+cancel+pulizia, oppure tenere solo il punto 8 |
| 10 | Build profiling (8acc2d35, 53334f61) | FATTA | RelWithDebInfo, non debuggable, firma debug | non in CI; niente run-as | tenere |

## PR motore lib#16
| n | Richiesta | Stato | Come | Cosa non va | Direzione |
|---|---|---|---|---|---|
| 11 | Schermo nero DSi con cart DS (c552b389) | FATTA | funzioni SCFG byte-identiche upstream, 12/12 call site | SD/NAND ARM7 spenta in DSi vera (bit 18); timing VRAM in DS mode; avvertenza invertita | testo PR; provare DSiWare che salva |
| 12 | WiFi si spegne dopo ~67 s (53aff8bc) | NON PROVATA | verbatim upstream 10a173b5 | mai riprodotta (max 23 s) | WFC > 3 min fino a GTS/Dono Segreto |
| 13 | Ricompilazione shader Compute (b876d3b3) | FATTA | guardia su scale+hires (unici input) | app marca dirty per tutto; #2751 senza init | tenere; fix #2751 |
| 14 | Edge index list (769d61de) | FATTA | non costruita; unico lettore commentato | 2 shader Edge ancora compilati | toglierli |
| 15 | JIT literal folding (3ad4e940) | NON DICHIARATA | guardia LiteralRegistered (chiude LDRSB/LDRSH da PC) | seconda modifica nascosta a InvalidateByAddr; manca caso translatedAddr==0; nessun test | scorporare, guardia, test |

## Difetti misurati e domande
| n | Tema | Stato | Cosa sappiamo | Direzione |
|---|---|---|---|---|
| 16 | Cache shader 33/33 | SPIEGATO | cache inesistente (commentata da upstream), log incondizionato; 517 ms = 33 compilazioni in un frame | fix log (1 riga); spalmare compilazioni; cache vera solo se serve |
| 17 | Scanlines irraggiungibile | PREESISTENTE | manca da arrays.xml | dopo il fix del punto 6 |
| 18 | rtc_sync acceso sulla Thor | NON SPIEGATO | nessun percorso nel codice; backup/tocco | spegnere prima dei collaudi |
| 19 | Multiplayer locale | ANALIZZATO | budget 8-10 ms RTT; BT 12-18 ms, BLE ≥22 ms fuori; Wi-Fi Direct al limite; maintainer al lavoro | tester su #1662; prototipo misura; PR 3 righe timeout LAN |
| 20 | Nessun test automatico | SCOPERTO | test C++ non compilati; lint/test tolti da CI | target host, androidTest, sanitizer |

## Direzione
1. Rete di sicurezza (test C++ su host, lint+test locali, androidTest, sanitizer in AVD). 2. Regressioni (6, 1). 3. Difetti propri (9, 3, 5, 15). 4. Prove reali per ogni modifica: build, test, ROM in AVD, poi Thor solo con l'utente. WiFi: WFC > 3 min. RTC: ritirata.
