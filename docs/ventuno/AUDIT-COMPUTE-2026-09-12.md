# Audit del renderer Compute (12/09/2026, sola lettura)
Misura di riferimento sulla Thor (HeartGold, Compute 6x, 60 fps): CPU 32%, GPU 33,5%, 586 MB, 51,7 °C.

## Dove va il lavoro
- CPU: il ciclo per riga scalata di ogni poligono (GPU3D_Compute.cpp:835-890) produce una SetupIndices per riga: O(scala × altezza poligoni). A 6x = 6 volte le iterazioni di 1x.
- Upload: 4 glBufferSubData per frame (:908 fino a 1,5 MB, :911 0,3-1,5 MB, :914 ≤82 KB, :983 UBO) su buffer DYNAMIC_DRAW senza orphaning/ring (ghosting o stallo nel driver Adreno).
- Dispatch: 9 + varianti; barriere: 7 (:1001,:1006,:1011,:1017,:1086,:1091,:1105); glBindBuffer indirect ripetuto nel ciclo varianti (:1081).
- Nessuna attesa CPU↔GPU in regime normale (nessun glFinish/ClientWaitSync; FrameQueue non aspetta mai). Unica sincronia: glMapBufferRange in GetLine (:1168) solo con display capture attiva.
- Frame limiter: clock_nanosleep, non busy-wait (MelonDSAndroidJNI.cpp:673-690). ADPF: target 16,67 ms sul tid emu, riporta la durata reale; chiede il massimo solo in fast-forward illimitato.
- Compositor: 2 glTexSubImage2D da 590 KB per frame (~71 MB/s), per tutti i renderer.
- Thread a vuoto: 3 SaveManager con Sleep 100 ms = 30 risvegli/s (SaveManager.cpp:192).

## Memoria (GPU3D_Compute.cpp:384-425; P = 49152·S²)
TileMemory 192 B/pixel (NON dipende da TileSize: MaxWorkTiles = P/TS²·16, TS² si cancella); totale GPU ≈ 238 MB a 4x, 344 a 5x, 478 a 6x, + FrameQueue 14,2 MB × slot a 6x. TileSize passa a 16 già a 5x (:363-372).
Scritto e mai letto: DepthBlend scrive sempre 6 uint/pixel (shaders.h:1466-1472); il layer .y serve solo ad AntiAliasing (:1594-1628) e Fog (:1587-1590); DepthBlend e FinalPass girano su tutto lo schermo anche senza poligoni. A 6x il giro FinalTileMemory costa ~5,1 GB/s.

## Le tre leve del documento gemello
- MaxWorkTiles ×16: leva reale ma BinCombined scrive WorkDescs senza confronto col limite (shaders.h:989-1005): ridurre senza contatore = overflow silenzioso. Prima il contatore.
- TileSize: non libera memoria di tile; quadruplica BinResultMemory; esito di segno ignoto.
- Barriere: 2 COMMAND_BARRIER superflue su 4; guadagno basso su Adreno.
- Upstream non ha ottimizzato il compute dopo 431ab4bd (5df83c97 e 77774538 già nel nostro albero).

## Interventi ordinati (guadagno × sicurezza)
1. Scala = quella che lo schermo mostra (4x/5x invece di 6x): nessun codice, −50% memoria, −55% pixel. Quasi sicuro. Misura: /proc/pid/stat, batterystats, kgsl/gpubusy, FanBase.
2. Contatore di overflow dei work tile (~20 righe): strumentazione, prerequisito per toccare il ×16. Quasi sicuro.
3. Ciclo varianti: bind indirect fuori dal ciclo, salto di DepthBlend/FinalPass/barriera con numYSpans==0 (~10 righe). Quasi sicuro; controllo a schermo su un menu.
4. Non scrivere il layer .y senza AA/fog (2 varianti shader, ~30 righe): fino a −2,5 GB/s a 6x. Da misurare prima (verificare se HGSS accende l'AA in DISP3DCNT).
5. Ridurre maxYSpanIndices con contatore (fino a −50 MB a 6x). Da misurare.
6. SaveManager su condition variable (~25 righe): −30 risvegli/s. Quasi sicuro, guadagno piccolo.
Esclusi: ristrettura bit barriere, unificazione dispatch, Vulkan, cache shader (vale l'avvio, non il frame).
