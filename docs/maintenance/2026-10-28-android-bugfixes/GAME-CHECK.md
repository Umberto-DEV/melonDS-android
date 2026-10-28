# Prova funzionale nell’APK Android con ROM locale

**Esito: avvio → caricamento raw → salvataggio → chiusura del processo → nuovo avvio e caricamento riusciti.** Una sola sessione breve, senza attribuire al risultato la correzione di freeze Latios/Mew o la compatibilità di ogni ROM.

Ambiente: Android15/API35 ARM64, AVD `melonds-buglab-api35-arm64-16k`, pagine16384 byte,1080×2400, rendering host SwiftShader, APK del codice finale `19071123`, core `fd758240`. Impostazioni iniziali dell’app: JIT attivo per ABI64bit, renderer software, boot diretto, BIOS/firmware integrati, RTC sync opzionale disattivata. Nessun savestate importato, nessun cheat attivato. Non è una misura di prestazioni.

Fixture congelate fornite dalla chat **Pianifica debug profondo della ROM**, usate tramite copie private:

| Fixture | Byte | SHA256 |
|---|---:|---|
| Sacred Gold Plus1.2.5 IT |131906120|`bdf50a4f327f8ea0d8a2ef312e1491d78460d34e978d9b15d160eaf140b313d4`|
| Raw avanzato iniziale |524288|`975bc4d18c5991a688c2dcefb97a959fc2a400027ea216d5a77bdaded2bf6f1a`|
| Raw scritto e ricaricato |524288|`9874fe6e6f721760440bb077fd70f15cbeaa74a8d4fd765fc5f13309950cdf75`|

Procedura eseguita:

1. Verifica hash degli originali, copia locale separata e trasferimento delle copie in `Download/MelonDS-Buglab` dell’AVD. Stesso nome base per `.nds` e `.sav`.
2. Nell’app, **Set ROM directory** → picker SAF → cartella dedicata → autorizzazione della cartella. Il tentativo iniziale di lanciare direttamente un URI `file:` non trovava la ROM e il grant di un URI SAF dalla shell era correttamente negato: la prova valida passa dal picker Android normale.
3. Apertura ROM dalla libreria, avanzamento delle schermate di avvio con il pulsante A virtuale. Arrivo al Centro Pokémon di Ebanopoli con il salvataggio avanzato.
4. Tocco **Salva**, **Sì**, conferma di sovrascrittura della sola copia. Attesa del ritorno dal messaggio di scrittura al gioco. Il file mantiene524288 byte e cambia63 byte rispetto al raw iniziale.
5. `am force-stop` dopo il completamento; copia del raw scritto. Nuovo avvio dell’Activity con URI già autorizzato, sequenza di boot completa, caricamento nel nuovo processo. PID5095→5882 conferma che il vecchio processo non è stato semplicemente ripreso.
6. Ritorno alla stessa posizione; riepilogo del gioco conserva8 medaglie e119 voci Pokédex. Il tempo di gioco avanza normalmente. Il raw letto dopo il nuovo caricamento ha lo stesso hash del raw scritto. Il secondo riepilogo viene aperto per leggere i dati, senza confermare un altro salvataggio.

Input inviati mediante ADB come tocchi mantenuti150–500ms, sincronizzati sulle schermate. Coordinate osservate in questo layout: A circa(1012,2218), Salva(520,1260), Sì(980,1342). Non sono coordinate per altri telefoni e le durate non rappresentano una misura di latenza. Nessun warp o intervento sulla RAM del gioco.

Evidenze private: `build/android-bug-audit/runtime/game/`, con screenshot, logcat dell’app, hash, raw prima/dopo, PID e metadati della fixture. Lo screenshot di ritorno al campo dimostra la fine della scrittura; il messaggio conclusivo transitorio non è stato catturato. Nessun fatal/ANR dell’app è emerso nel log raccolto, senza inferire da ciò assenza universale di crash.

La verifica non controlla semanticamente ogni Pokémon/record del raw, non interrompe intenzionalmente una scrittura, non prova NAND/DSi, WFC, RetroAchievements, driver/microfono/Thor o tutti i salvataggi. Il fixture con training attivo e le scene Latios/Mew restano verifiche ROM separate. Gli originali sono nuovamente hashati dopo la prova e restano invariati.
