# Connessioni WFC generali, allineamento desktop, interoperabilità con hardware reale (12/09/2026)

## 1. Fatti verificati per il gestore generale delle connessioni
- Il gioco rilegge gli slot AP via SPI a ogni connessione (SPI.cpp:202 serve i byte dal buffer firmware; SetupDirectBoot non copia gli AP). Modificare il buffer a emulatore in pausa ha effetto immediato, senza reset. Il firmware NON è nel savestate (SPI.cpp:113-127).
- Offset AP derivati da UserSettingsOffset (SPI_Firmware.h:461-500): validi per 128/256/512 KB; il firmware importato viene già riscritto dal DS stesso (WriteFirmware → SaveManager, con modo non troncante); SAF in scrittura ok.
- Difetto attuale: la schermata scrive sempre filesDir/wfcsettings.bin, usato dal core solo con firmware interno in DS (MelonInstance.cpp:59-65) → con firmware importato o DSi non ha effetto.
- AP emulato: probe response e beacon con nome fisso "melonAP" (WifiAP.cpp:36,162-185,313-347); in DSi il confronto è esatto (DSi_NWifi.cpp:1044). Un nome libero nello SSID richiede la modifica dell'AP (echo del nome richiesto) e prova su hardware, DS e DSi.
- Menu di pausa: voci in RomPauseMenuOption.kt, il menu mette già in pausa; esistono pauseEmuThreadForSyncOperation/resume (MelonDSAndroidJNI.cpp:349-381). Nessun conflitto con RA hardcore (il DNS non è un vettore di cheat).

## 2. Progetto in tre tappe
- T1: API unica read/write sul firmware che la console userà (wfcsettings.bin oppure il file firmware importato, DS e DSi), tramite la classe Firmware del core; campo nome; backup .bak prima della prima scrittura di un firmware importato; ~200 righe.
- T2: scrittura live nel buffer in RAM a thread fermo + voce «Internet connections» nel menu di pausa (3 righe: nome, on/off, DNS); ~160 righe. Effetto immediato per la connessione successiva.
- T3: SSID libero nell'AP emulato (echo del nome nella probe response, beacon con lo slot attivo, APName per istanza; anche DSi_NWifi); ~40 righe, rischio medio: solo dopo collaudo WFC reale sulla Thor, DS e DSi. Finché T3 non è collaudata, il nome resta un'etichetta dell'app e lo SSID resta "melonAP"; la ROM mostra il nome ricavato dal DNS.

## 3. Desktop: vedi ALLINEAMENTO-DESKTOP-2026-09-12.md. La ROM non dipende da nulla di solo-Android.

## 4. Interoperabilità con hardware reale via server privati
- Kaeru (178.62.43.212): HGSS supportato, nessuna patch, WEP/aperta su DS; melonDS citato nella guida ufficiale; stesso backend Wiimmfi → cross-play implicito.
- Wiimmfi: cross-play DS reale ↔ melonDS documentato (video); GTS ok, lotte P2P via natneg; ban automatici per tool di manipolazione; serve BIOS dump reale per non essere visti come la stessa console.
- WiiLink (5.161.56.11, verificato da noi con dig; la guida melonDS cita 167.235.229.36 che oggi punta a Kaeru senza dls1): Doni Segreti via Wi-Fi.
- Limiti melonDS: errore 86420 = natneg dietro NAT (serve port forward/DMZ per le lotte P2P); 52100/52000 DNS; IR non emulato; "Search for Access Point" può crashare su Android (#1634): usare setup manuale.
- Prove in ordine: Test connessione (Thor e console reale, stesso DNS) → GTS deposito/ritiro incrociato (asincrono, non P2P) → Wi-Fi Club → lotta per codice amico (attesa fallire dietro NAT; ripetere con DMZ) → Piazza → cambio server per isolare → verifica game code della hack (IPKI/IPGI) → controllo ban.
