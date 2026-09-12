# Audit trasversale app + motore (12/09/2026, sera) — difetti nati dall'incastro fra componenti
Stato: in correzione sul ramo ventuno (vedi DECISIONI.md e la sezione "Esito" in fondo).

## BUG
- B1 Lid (#1589): il runnable dei 3 s sopravvive all'Activity (cancellato solo in onResume, EmulatorActivity.kt:1067-1070); dopo una ricreazione mette in pausa la sessione nuova; `lidClosedByScreenOff` non è salvato → coperchio chiuso per sempre.
- B2 `MelonDS::resume()` (MelonDS.cpp:226-232) riapre e avvia l'audio anche dopo `cleanup()` (startAudio non guardato da `instance`; currentAudioSettings non azzerato).
- B3 `instance == nullptr` (MelonDS.cpp:67-73, TODO) non gestito in loadRom/updateEmulatorConfiguration/start/stop → SIGSEGV con BIOS/NAND non caricabili (es. nand.bin troncata dichiarata PRESENT dal verificatore).
- B4 Impostazioni a caldo: `onSettingsChanged` sospende (isUserAuthenticated) e `resumeEmulator` parte prima; `updateConfiguration` riassegna lo shared_ptr letto dal thread emu (MelonInstance.cpp:529-541, :311, :653, :729) e muta SPU a frame in corso.
- B5 saveState/loadState (MelonDSAndroidJNI.cpp:365-377) senza pausa/attesa (a differenza di reset e rewind): RequestFlush senza lock contro CheckFlush → SRAM lacerata / UAF.
- B6 Toast "salvataggio fallito": SharedFlow replay 0 (utils/SharedFlow.kt:16) con collector solo STARTED → l'evento nativo (SaveManager.cpp:231) si perde proprio in background/uscita.
- B7 EmulatorMessageQueueJNI.cpp:52-63: due write() separate, e ora due produttori (thread emu + worker SaveManager) → header di un evento dentro il payload di un altro.
- B8 Fast-forward (toggle, hold #1608, bottone) non bloccato in RetroAchievements hardcore (EmulatorActivity.kt:272-274); stringa hardcore_mode_summary promette una slow-motion inesistente.
- B9 loadState/rewind chiamano setDateTime incondizionato (MelonInstance.cpp:577): orologio indietro anche con l'opzione RTC, e ogni rewind ristampa l'RTC.
- B10 Schermata WFC aperta dal menu di pausa: il core tiene un SaveManager sullo stesso file; il primo flush del guest sovrascrive le modifiche, che comunque non hanno effetto fino al prossimo avvio.
- B11 WfcSettingsJNI initDefaultWfcSlots crea lo slot 0 vuoto (il core usa WifiAccessPoint(type): melonAP, Normal): la prima visita distrugge lo slot 1 preconfigurato.
- B12 DSi + firmware interno: il SaveManager su wfcsettings.bin è scelto solo da userInternalFirmwareAndBios (MelonInstance.cpp:59-63) mentre EmulatorArgsBuilder in DSi usa il firmware dell'utente → 128 KB di firmware scritti in wfcsettings.bin.
- B13 EmulatorArgsBuilder.cpp:200-226: CloseFile solo nel ramo di errore → un fd perso a ogni avvio ROM.
## RISCHIO
- Stop sincrono sul thread UI (EmulatorViewModel.kt:405/410) con join + stageSaves + 3 distruttori SaveManager.
- sessionCoroutineScope non cancellato allo stop (finestra per B2/B3).
- syncRTC in parallelo al thread emu (resume() dopo lo sblocco, MelonDSAndroidJNI.cpp:330-341).
- loadState non resetta la finestra di rewind.
- FOREGROUND_SERVICE non dichiarato esplicitamente (arriva da work-runtime).
- useLegacyPackaging scritto nel blocco debug ma risolve al packaging esterno (build.gradle.kts:78).
- Restore delle preferenze non invalida il by lazy della configurazione controller.
- InputProcessor.pressedKeys mai svuotato a perdita di focus (combo spurie al ritorno).
- SCFG direct boot DSi: bit 18 a zero filtra SDMMC per l'ARM7 (comportamento upstream, mai provato).
## FRIZIONE
- Schermata WFC visibile anche con firmware importato o DSi, dove non ha effetto; wfcsettings.bin assente dall'export manuale; readSlots null mostrato come 0.0.0.0; I/O su main thread nella schermata; override DNS del motore senza chiamanti; nessun componente a schermo per FAST_FORWARD_HOLD.
## Percorsi puliti
Verificatore firmware DSi; compatibilità JSON InputConfigDto; R8 su tutti i simboli JNI (verificato con seeds.txt); StrictMode solo debuggable; wrap.sh non in release; mutex SetHostOverrides; releaseAll copre tutti gli handler; path/layout wfcsettings.bin coerenti.
