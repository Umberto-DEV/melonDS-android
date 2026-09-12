# melonDS Nightly 2.1 — ramo `ventuno` (stato al 12/09/2026, ore 12:40)
Base: PR #1672 (21 commit, = build sulla Thor) + motore lib#16 (3ad4e940). Da allora 21 commit app + 1 motore, tutti su origin/ventuno e fork/ventuno. **La Thor ha ancora la build precedente**: il ramo è divergente finché l'utente non installa una build nuova.

## Commit oltre la PR (dal più recente)
138baa2b feat(community): integrate five open upstream pull requests with corrections
fb611635 fix(bios): run the guided BIOS setup off the main thread and clean up after failures
70a1a8b2 build(engine): update the engine submodule (JIT literal guard, edge shaders, cache log)
ccc39f63 refactor(rtc): sync the console clock when the emulator resumes, not every frame
6aeab3bc fix(roms): size the 7z memory limit on the Java heap and decode one archive at a time
a8553031 fix(core-bridge): check save-state results and clean up the camera handler on stop
863f74b9 fix(audio): reopen the output stream on resume and serialise stream state changes
5b73ea5d fix(save): never truncate the save file before the new data is written, and tell the user when a write fails
37585980 fix(input): release keys on cancelled gestures, layout changes and opposing d-pad presses
2c14e8fa build(engine): point the engine submodule at the ventuno core (431ab4bd -> 3ad4e940)
7a701177 docs(db): explain why migration 4->5 avoids window functions
507c35f7 fix(video): keep the LCD and scanline filters anchored to the native DS grid
d6f6c3a7 build(tools): host test runner, core sanitizer build and StrictMode in debuggable builds
53334f61 build(app): stop the profiling build type being debuggable
Motore (submodule, fork/ventuno): 137c4ba7 fix(jit,gl): guardia literal non tracciato, shader Edge rimossi, log cache corretto.

## Cosa contiene in più rispetto alla build sulla Thor
- Filtri LCD/Scanlines ancorati alla griglia nativa; Scanlines selezionabile.
- Input: ACTION_CANCEL su stilo e pulsanti singoli; rilascio al cambio layout; croce senza direzioni opposte; zero allocazioni; 25 test Robolectric.
- Salvataggi: scrittura non troncante + fallback + toast su errore; thread guardato; test host con kill a metà scrittura.
- Audio: mutex, riapertura al resume, policy unificata, activeInstance protetto.
- RTC: sync al resume (non per frame), soglia 30 s, mai indietro.
- 7z: limite memoria sull'heap, un decoder alla volta, paracadute OOM.
- Fix laterali da #1666 (saveState/backup error/camera handler).
- PR di terzi integrate con correzioni: #1608 hold FF, #1606 combo hotkeys, #1625 mute on FF, #1648 ROM da app esterne, #1589 sleep/wake → lid. Fuori: #1657 (troppo invasiva), #1666 (scartata), #1664 (superata).
- Assistente BIOS: copia su IO con dialogo, pulizia parziali, ambiguità corretta, cartelle case-insensitive, rivalidazione; 5 test.
- Strumenti: tools/host-tests.sh (4 suite C++ con ASan/UBSan), tools/core-host-sanitize.sh, StrictMode nelle build debuggable, -PnativeSanitize=hwaddress.

## Come si verifica (Mac)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
tools/host-tests.sh                                   # test C++ su host
sh ./gradlew :app:testGitHubNightlyDebugUnitTest      # 80+ test JVM/Robolectric
sh ./gradlew :app:assembleGitHubNightlyDebug -Pandroid.injected.build.abi=arm64-v8a   # APK in app/build/intermediates/apk/gitHubNightly/debug/
Prove in AVD: PROVE-AVD.md (checklist e risultati).

## Prove fatte (AVD, 12/09 pomeriggio) — dettaglio in PROVE-AVD.md
- PASS: memoria 7z (8/8 MemoryLimitException, zero OOM, PSS max 153 MB); sleep/wake → lid; audio che riparte al ritorno; lancio da app esterna con content://; 25 test input, 5 test BIOS, 3 test ROM name, 4 suite C++ host (kill a metà scrittura → file intero).
- LIMITE AVD: renderer OpenGL nero anche con l'APK del ramo PR (byte-identico) → il percorso GL si prova sulla Thor; software renderer ok.
- HWASan (wrap.sh, 06f9c6a1): ha trovato un use-after-free in setupAudio (stream precedente distrutto sotto il callback), corretto; riprova con 3 cicli background/foreground/riavvio ROM: zero report su 8.802 righe.
- NON RISOLTO: file:// da app esterne (scoped storage nega l'accesso: solo il nome è risolto).

## Aggiunte del pomeriggio (12/09)
- Test JIT su host (77449003): differenziale interprete/JIT su HeartGold (600 frame identici) + invalidazione con ROM sintetica; mutanti A/B/C provati e ripristinati; x86_64 non coperto su Mac.
- Motore: override DNS opzionale (bc060f4c) con test a pacchetti sintetici (6/6).
- WFC: schermata "Online connections (WFC)" (29213750): tre slot, DNS, checksum, "server consigliati" (1 WiiLink Doni Segreti, 2 Kaeru GTS/lotte, 3 AltWFC). Disegno concordato con la sessione ROM: scelta per posizione, etichetta dal DNS, SSID "melonAP".
- Messaggio BIOS per caso + WRONG_CONSOLE (25c79cc2); lint: servizio lid via ContextCompat (708b9589); precarico config controller (9460cc5f).

## Aperto
- StrictMode all'avvio di EmulatorActivity (6 letture preferenze + client HTTPS RetroAchievements sul thread UI): preesistenti, fix S.
- #1657 idle power draw; caso file:// in BaseRomFileProcessorFactory; cache shader vera; Wi-Fi/WFC > 3 min (solo Thor); build profiling "2.1" da installare sulla Thor (decisione dell'utente).
