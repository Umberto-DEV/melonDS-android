# Sacred Gold Plus: selettore selvatici — 2.1.1

Questa versione aggiunge il selettore per Sacred Gold Plus EN/IT con intestazione
`IPKE 19D1EEBB` / `IPKE 1C1F741C`. Il core melonDS resta al commit già usato da
`ventuno`; la gestione del selettore vive nell'app Android.

## Uso

1. Importa il catalogo aggiornato di Sacred Gold Plus 1.2.1.
2. In **40 — Selvatici · scegli Pokémon e livello**, tocca **Scegli Pokémon e livello**.
3. Cerca per nome o numero Pokédex, scegli uno dei 493 Pokémon e un livello 1–100.
4. Premi **Attiva** e torna al gioco. Il codice resta abilitato nell'elenco, ma il
   selettore in gioco parte spento.
5. Premi **L+R**: il messaggio conferma che il selettore è attivo. Rilascia i tasti
   e premi di nuovo **L+R** per spegnerlo e tornare agli incontri normali.

Vale per gli incontri nell'erba. Spegni **Livelli selvatici** nelle opzioni del
gioco per ottenere il livello scelto. Unown richiede un puzzle completato nelle
Rovine d'Alfa. Riavvio e caricamento di stato spengono il selettore.

Anche le tre vecchie voci che sceglievano la specie contando Master Ball aprono
il selettore: la nuova configurazione sostituisce il loro codice. Se erano già
state usate nella sessione, riavvia il gioco una volta per eliminare le vecchie
modifiche. I savestate creati mentre girava un vecchio codice possono contenerle.

La scelta viene salvata insieme all'abilitazione. I modificatori conosciuti di
specie/livello incompatibili vengono spenti. I nuovi codici dei gruppi **44–46**
(natura, maschio+natura, femmina+natura) si escludono a vicenda; possono convivere
con il selettore di specie e livello. Per questi gruppi segui le istruzioni della
singola voce; L+R riguarda il selettore di specie/livello.

## Implementazione e verifiche

- Ricerca sui 493 nomi, con normalizzazione di accenti, punteggiatura e maiuscole;
  ricerca numerica esatta anche con `#025`.
- Transazione Room unica per codice e stato. Un errore di scrittura mantiene aperta
  la schermata; cancellazione, annullamento e modifiche pendenti sono coperti.
- Il motore riconosce l'intero codice generato e l'intestazione del gioco.
  I 92 byte della tabella incontri vengono modificati solo durante il frame e
  ripristinati prima di salvataggi rapidi, riavvolgimento e letture dell'interfaccia.
- Verificate sul core reale entrambe le lingue: attivazione, pressione mantenuta,
  disattivazione in lotta, fuga e incontro normale; cambio mappa e ritorno;
  caricamento di stato e rimozione del codice. I confronti RAM non mostrano
  residui della selezione nelle tabelle ripristinate.
- 110 test Android superati, inclusa interfaccia italiana orizzontale 640×360,
  persistenza e selezione esclusiva dei 75 codici natura/sesso generati dal repo ROM.
- Test nativi con ASan/UBSan: toggle, RTC, audio, modalità file, scrittura salvataggi
  e invalidazione JIT superati. Il differenziale JIT richiede una ROM esterna e
  non fa parte di questa verifica dell'app.
- APK ARM64 ottimizzato, non debuggable e non test-only. Firma identica alla
  precedente build locale `.nightly.perf`; versionCode 42, versionName 2.1.1.
- La console fisica non era collegata: questa build non è stata installata né
  collaudata sul dispositivo in questa sessione.

## Build

Con JDK 21 e SDK/NDK dichiarati dal progetto:

```sh
sh ./gradlew :app:testGitHubNightlyDebugUnitTest
bash tools/host-tests.sh
sh ./gradlew :app:assembleGitHubNightlyProfiling \
  -Pandroid.injected.build.abi=arm64-v8a -Pandroid.injected.testOnly=false
```

L'APK usa il package `me.magnum.melonds.nightly.perf`. Non confonderlo con la
versione Play Store/Obtainium `me.magnum.melonds` o con la variante `.dev`.
