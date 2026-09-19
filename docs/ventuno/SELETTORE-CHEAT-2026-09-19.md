# Selettore per i cheat "modificatore" — specifica (19/09/2026)

Stato: **specifica approvata in discussione, non ancora implementata.** Sostituisce il selettore
SGP-only (`WildEncounterCheat`, `NatureEncounterCheat`, `WildEncounterDialog`) con un meccanismo
unico che vale per ogni gioco il cui database cheat lo permette. Ramo di lavoro:
`feat/modifier-families` da `ventuno`; merge in `ventuno` solo dopo la prova sulla Thor.

## 1. Scopo e principi

- **Obiettivo**: il selettore a lista per scegliere il Pokémon selvatico (e il livello) funziona su
  HeartGold, SoulSilver, Diamond, Pearl, Platinum, Black, White, Black 2, White 2 e Sacred Gold Plus,
  senza codice specifico per gioco.
- **Discriminante**: il selettore compare per un gioco *se e solo se* fra i cheat importati ce n'è
  uno riconosciuto come "modificatore". Decide il database, non una tabella di giochi supportati.
- **Nessun dato nuovo**: niente migrazione Room, niente colonne, niente file di catalogo. Una
  "famiglia" è una vista calcolata sui cheat del gioco selezionato.
- **Nessun clone**: i file SGP esistenti vengono trasformati o eliminati, mai duplicati. Tutto vive
  nel sorgente dell'app (`app/src/main`, `app/src/test`). Nessuno script, nessun file in `tools/`.
- **SGP resta funzionante** per costruzione: il suo catalogo passa dalle stesse regole di tutti,
  verificato byte per byte da un test (§8).
- **Regole validate sui dati**: ogni soglia qui sotto è uscita da uno spike sul database
  DeadSkullzJr del 12/08/2025 (4265 giochi) e sul catalogo SGP 1.2.1 EN/IT. Numeri in §3.4.

## 2. Modello

Tutto in `app/src/main/java/me/magnum/melonds/common/cheats/`, Kotlin puro, nessuna dipendenza Android.

```kotlin
// ArCode.kt — l'unico posto che sa cos'è un opcode Action Replay
data class ArInstruction(val a: UInt, val b: UInt)           // una coppia di word
data class ArBlock(val instructions: List<ArInstruction>)     // fino a D2000000 00000000 incluso
object ArCode {
    fun parse(code: String): List<ArBlock>?                   // null se non è hex a gruppi di 8
    fun render(blocks: List<ArBlock>): String                 // formato dell'app: "XXXXXXXX YYYYYYYY ..."
}

// ModifierFamily.kt — riconoscimento e applicazione
enum class Kind { SPECIES, LEVEL, NATURE, GENERIC }
enum class Source { ENUMERATED, ITEM_LOAD }

data class Option(val value: Int, val label: String, val cheat: Cheat?)   // cheat = null per ITEM_LOAD

data class Parameter(                       // solo ITEM_LOAD
    val blockIndex: Int,                    // quale blocco del codice
    val instructionIndex: Int,              // quale istruzione nel blocco (la load o la D5)
    val width: Int,                         // 16 (DA/D7) o 8 (DB/D8)
    val kind: Kind,                         // SPECIES per il primo blocco, LEVEL per il secondo
    val current: Int?,                      // valore se già in forma fissa
)

data class ModifierFamily(
    val title: String,                      // nome cartella (A) o nome cheat (B)
    val kind: Kind,
    val source: Source,
    val folder: CheatFolder,
    val members: List<Cheat>,               // A: le voci; B: il solo cheat
    val options: List<Option>,              // A: dalle voci; B: da PokemonSpecies o 1..100
    val parameters: List<Parameter>,        // solo B
    val instructions: String?,              // la <note> del database, mostrata nell'help
) {
    val current: Option?                    // A: la voce abilitata; B: dal valore in forma fissa
}

object ModifierFamilies {
    fun recognize(folder: CheatFolder, cheats: List<Cheat>): List<ModifierFamily>
    fun select(family: ModifierFamily, values: Map<Kind, Int>, allGameCheats: List<Cheat>): List<Cheat>  // cheat da mettere in staging
    fun disable(family: ModifierFamily, allGameCheats: List<Cheat>): List<Cheat>
}
```

`PokemonSpecies.kt` resta la lista 1–493 e acquista `normalize()` che ignora parentesi e `#NNN`
(`"Meganium (Male)"`, `"#001 Bulbasaur"` → `meganium`, `bulbasaur`).

## 3. Riconoscimento

Tutto per **cartella**: `recognize()` riceve una cartella e i suoi cheat, restituisce 0..n famiglie.
Un cheat appartiene al massimo a una famiglia. La gamba A ha priorità sulla B: un cheat che è
membro di una famiglia enumerata non viene mai esaminato come ITEM_LOAD.

### 3.1 Parole chiave (nomi di cartelle e cheat)

Case-insensitive, sottostringa. I cataloghi possono essere localizzati (SGP IT usa «Selvatici»).

| Ruolo | Regex |
|---|---|
| `WILD` | `wild\|encounter\|selvatic\|incontr` |
| `STARTER` | `starter\|iniziale` |
| `LEVEL` | `level\|livell` |
| `NATURE` | `natur` |

### 3.2 Gamba A — famiglia enumerata (`ENUMERATED`)

1. Raggruppa i cheat della cartella **per lunghezza del codice** (numero di word). Cartelle miste
   sono la norma: SGP 50–59 hanno un `LEGGIMI` di 2 word, HG/SS `Level Modifier Codes` ha 1
   codice generico + 12 `Level N`.
2. Per ogni gruppo con **≥ 5** cheat: calcola le posizioni di word che variano fra i membri.
   Il gruppo è famiglia se le posizioni variabili sono **1 o 2** e le tuple dei valori sono
   tutte distinte. (Due posizioni servono alle nature SGP/HG: `0x2400+n` e `0x2700+n`.)
3. `kind`:
   - `SPECIES` se le posizioni variabili sono **esattamente 1** e (≥ 80 % dei nomi normalizzati è in
     `PokemonSpecies` **oppure** tutti i valori sono in 1..649 e il nome cartella matcha `WILD|STARTER`);
   - altrimenti `NATURE` se il nome cartella matcha `NATURE`; `LEVEL` se matcha `LEVEL`; altrimenti `GENERIC`.
4. `options`: una per membro, `value` = la prima word variabile (intero), `label` = nome del cheat.
   Per `SPECIES` la ricerca per numero usa `value` (è il numero Pokédex: verificato Gen 1–5).
5. `title` = nome cartella. `instructions` = la `<note>` del primo membro, se c'è.

### 3.3 Gamba B — lettura da oggetto o calcolatrice (`ITEM_LOAD`)

Per ogni cheat **non** membro di una famiglia A, il cui nome o la cui cartella matcha `WILD`:

1. `ArCode.parse()`; per ogni blocco cerca la prima istruzione con `a ∈ {DA000000, DB000000}`
   (load 16/8 bit) **oppure** `a = D5000000` (forma fissa, §4.2) e, dopo di essa nello stesso blocco,
   saltando solo `C0000000`, `D4000000`, `D3000000`, `DC000000`, un'istruzione `a ∈ {D7000000, D8000000}` (store).
2. **L'indirizzo `b` della load deve essere relativo: `b < 0x02000000`.** È la regola che separa i
   modificatori (leggono un contatore di oggetti tramite puntatore: `DA000000 0000DCF6`) dai codici
   che leggono il generatore casuale (`DA000000 02250010`) — che hanno la stessa forma e sarebbero
   rotti dalla riscrittura. Per la forma fissa il vincolo si applica all'indirizzo dello store.
3. Ogni blocco che soddisfa 1–2 è un `Parameter`. Il **primo** è `SPECIES` (width dalla load),
   il **secondo** è `LEVEL`. Blocchi successivi (Platinum ne ha un terzo) restano intatti e non
   sono parametri.
4. `options`: `SPECIES` → `PokemonSpecies.all` (493 basta: HG/SS/D/P/Pt sono Gen 4; B/W hanno la
   gamba A); `LEVEL` → 1..100.
5. `title` = nome del cheat. `instructions` = la sua `<note>`.

### 3.4 Validazione (spike del 19/09, script usa e getta, non nel repo)

| Catalogo | Famiglie A | di cui SPECIES | SPECIES in giochi non-Pokémon | B presi (retail Pokémon) | B scartati |
|---|---|---|---|---|---|
| DeadSkullzJr 20250812, 4265 giochi | 9678 (1887 in cartelle miste) | 2113 | **0** | HG/SS: `Wild Pokemon [and Level] Modifier`, `v1`, `v2`, `Level Modifier Code`; D/P/Pt: `(Calculator)`, `Mining Museum`; B2/W2: `Wild Pokemon Modifier` | `Encounter Random Wild Pokemon`, `Random Pokemon Levels`, `Dream World`, `Shiny`, `Max IVs`, `No Encounters`, `Recollect Starters` |
| SGP 1.2.1 EN + IT | 56 | 30 (50–59: 50/50 nomi) | 0 | cartella 40 (forma fissa, 2 parametri); 41 `metodo classico` | — |

Fatti usati dalle regole: il codice `Wild Pokemon Modifier v1` di HeartGold USA nel DB è
identico byte per byte a uno dei `legacyCodes` SGP; SoulSilver USA è identico a HeartGold;
D/P/Pt usano la stessa struttura con la calcolatrice del Pokétch (`DA000000 0011ECF0` / `0011FF00`).

## 4. Applicare la scelta

### 4.1 `ENUMERATED`
`select()` restituisce la voce scelta con `enabled = true` e ogni altro membro abilitato con
`enabled = false`. Nessun codice generato. `disable()` spegne il membro attivo.

### 4.2 `ITEM_LOAD` — riscrittura in place
Per ogni parametro con un valore scelto, l'istruzione load `DA000000 addr` / `DB000000 addr`
diventa `D5000000 value` (**forma fissa**: `datareg = value`, `AREngine.cpp:323`). Il resto del
codice — trigger tasti, altri blocchi, il primo blocco che regala gli oggetti — **non viene toccato**.
Il cheat mantiene `id`, `name` e `description`; cambia solo `code` e `enabled = true`.
Un codice già in forma fissa si riconfigura sostituendo `b` della `D5`.

Conseguenza dichiarata: su HeartGold vanilla il Pokémon scelto appare *tenendo L*, come dice la
nota del database. Il toggle L+R nativo resta SGP (§7).

### 4.3 Profilo canonico SGP
Se `WildEncounterCheat.supports(gameCode, checksum)` è vero e la famiglia è `ITEM_LOAD` con
parametri `SPECIES` + `LEVEL`, il codice prodotto è il **canonico** a 28 word (firma overlay
`52246C94 28038800` al posto del trigger, `D5` specie, `D5` livello) — quello che
`WildEncounterToggle::parse()` riconosce. È l'unico punto in cui SGP è trattato diversamente, e
serve a non perdere il toggle nativo.

### 4.4 Mutua esclusione
Quando si abilita un membro/parametro di una famiglia di `kind ≠ GENERIC`, vengono disabilitati:
i membri attivi della stessa famiglia **e i membri attivi di ogni altra famiglia dello stesso
`kind` nello stesso gioco**. Un solo livello selvatico, una sola specie, una sola natura attivi.
Copre: SGP 44/45/46 (tre cartelle NATURE), SGP 41/49 contro il livello della cartella 40,
HG `Level N` contro `Level Modifier Code`. Le famiglie `GENERIC` si escludono solo al proprio interno.

## 5. Interfaccia

- **`CheatListScreen`**: la lista della cartella diventa `List<CheatListItem>` dove
  `CheatListItem = Single(cheat) | Family(family, expanded)`. Una famiglia è **una riga**:
  titolo, sottotitolo `current` («Bulbasaur · Lv 5» o «non attivo»), chevron. Sotto la riga,
  `Mostra tutte (151)` / `Nascondi` espande i membri come righe normali (toggle, modifica,
  cancella come oggi). Stato `expanded` in `rememberSaveable`, non persistito.
- **Tocco sulla riga** → `ModifierFamilyDialog(family)`; **tocco sul membro espanso** → come oggi.
- **`ModifierFamilyDialog`** (ex `WildEncounterDialog`, stesso aspetto e stessa disposizione):
  ricerca per nome o numero sulle `options`; campo livello **solo** se la famiglia ha un parametro
  `LEVEL` (B) — per A il livello è un'altra famiglia; pulsante **Attiva**; pulsante **Disattiva**
  visibile se `current != null`; help = `instructions` della famiglia, con il testo SGP attuale
  come ripiego quando la nota manca.
- Nessun altro cambiamento di schermata. Le stringhe nuove (`Mostra tutte`, `Nascondi`,
  `Disattiva`, `non attivo`) vanno in `values/wild_encounters.xml` e `values-it/`.

## 6. Percorso della modifica — file per file

Ramo `feat/modifier-families` da `ventuno`. Nessun worktree, nessun file fuori da questi.

| File | Azione |
|---|---|
| `common/cheats/ArCode.kt` | **nuovo** — parser/renderer |
| `common/cheats/ModifierFamily.kt` | **nuovo** — §2, §3, §4 |
| `common/cheats/WildEncounterCheat.kt` | **ridotto** a `supports()` + `canonicalCode(species, level)`; via regex, `selection()`, `conflicts()`, `legacyCodes` |
| `common/cheats/NatureEncounterCheat.kt` | **eliminato** |
| `common/cheats/PokemonSpecies.kt` | `normalize()` pubblica, ignora parentesi e `#NNN` |
| `ui/cheats/CheatsViewModel.kt` | `wildEncounterSupported`, `disabledConflicts`, il ramo speciale di `toggleCheat`, `configureWildEncounter` → `families: Flow<List<CheatListItem>>`, `selectFamilyOption()`, `disableFamily()`; lo staging (`stageCheats`/`modifyCheats`/`commit`) resta identico |
| `ui/cheats/ui/CheatListScreen.kt` | riga famiglia + espansione; dispatch su `CheatListItem` al posto dell'`if` su `WildEncounterCheat.isConfigurable` |
| `ui/cheats/ui/WildEncounterDialog.kt` | **rinominato** `ModifierFamilyDialog.kt`, parametro `ModifierFamily` |
| `res/values/wild_encounters.xml`, `res/values-it/wild_encounters.xml` | stringhe nuove; le esistenti restano |
| `cpp/WildEncounterToggle.h`, `cpp/MelonInstance.cpp` | **invariati** |
| `test/.../common/cheats/ArCodeTest.kt`, `ModifierFamilyTest.kt` | **nuovi** |
| `test/.../common/cheats/WildEncounterCheatTest.kt` | ridotto al canonico |
| `test/.../common/cheats/NatureEncounterCheatTest.kt` | **eliminato** |
| `test/.../ui/cheats/WildEncounterViewModelTest.kt`, `WildEncounterDialogTest.kt`, `test/.../impl/WildEncounterPersistenceTest.kt` | aggiornati al modello generico, stessi scenari |
| `test/resources/cheats/modifier-families.xml` | **nuovo** — fixture (§8) |
| `test/resources/sgp-nature-codes.tsv` | resta, usato dal test NATURE |

## 7. Nativo — fase 1 e fase 2

Fase 1: nessuna modifica C++. `MelonInstance::loadCheats` continua a intercettare solo il codice
canonico quando `wildEncounterSupported` (checksum SGP). Ogni altro codice va all'`AREngine`.

Fase 2 (separata, dopo prova sulla Thor con una ROM HeartGold/SoulSilver USA): aggiungere
`4DFFBF91` e `2D5118CA` a `WildEncounterToggle::supports()` e al profilo canonico §4.3, così il
toggle L+R vale anche sui giochi originali. Gli indirizzi sono identici (§3.4), ma non si scrive
prima di averlo visto.

## 8. Test

Fixture `test/resources/cheats/modifier-families.xml`: nel formato `<codelist>` che l'app importa,
estratta dai cataloghi reali, ~6 KB: HeartGold USA (`Wild Pokemon Modifier Codes`, `Level
Modifier Codes`, `Nature Modifier`), Platinum USA (`Encounter Codes`), Black USA (`Wild Pokemon
Modifier - Generation 1` ridotta a 10 voci + `Encounter Codes` per i contro-esempi), SGP EN
(cartelle 40, 41, 44, 45, 49, 50 ridotta a 10 voci + `LEGGIMI`).

Casi obbligatori:
- **Riconoscimento**: ogni riga della tabella §3.4 per i giochi in fixture, presa e scartata.
- **Cartelle miste**: HG `Level Modifier Codes` → famiglia LEVEL di 12, `Level Modifier Code` fuori
  e riconosciuto come ITEM_LOAD; SGP 50 → famiglia SPECIES di 10, `LEGGIMI` fuori.
- **Riscrittura**: `Wild Pokemon Modifier v1` HG + Pikachu → identico all'originale tranne
  `DA000000 0000DCF6` → `D5000000 00000019`; riconfigurazione a Bulbasaur; forma fissa riconosciuta.
- **Non-regressione SGP, byte per byte**: cartella 40 + (Pikachu, 25) → esattamente il codice che
  `WildEncounterCheat.code(25, 25)` produce oggi (il test lo fissa come costante letterale, non lo
  ricalcola); i tre `legacyCodes` aprono il selettore; le 75 righe di `sgp-nature-codes.tsv`
  formano tre famiglie NATURE mutuamente esclusive.
- **Mutua esclusione** per `kind` (§4.4) — gli scenari di `WildEncounterViewModelTest` restano tutti,
  riscritti sul modello generico: `genderAndNatureSelectionsAreExclusive…`,
  `enablingStandaloneLevelDisablesSelector…`, `configurationBlocksCommit…`, `undo…`, `failed…`.
- **UI** (Robolectric, come oggi): ricerca/selezione/conferma; annulla non salva; disattiva;
  campo livello assente per una famiglia A; espansione mostra i membri; paesaggio 640×360.
- **Persistenza**: `WildEncounterPersistenceTest` — transazione unica, rollback su errore.

Comando: `sh ./gradlew :app:testGitHubNightlyDebugUnitTest`.

## 9. Fuori scope e rischi

- Codici B a più di due parametri: il terzo blocco di Platinum resta intatto, non configurabile.
- Regioni EUR/JPN: la gamba B funziona (codici propri nel DB), il toggle nativo no.
- Database con convenzioni diverse da DeadSkullzJr: le regole sono strutturali (opcode e
  indirizzi), i nomi servono solo per `kind` e per il filtro `WILD`; un catalogo con nomi in
  un'altra lingua richiede una parola chiave in più in §3.1, non una tabella.
- La riscrittura in place perde il codice originale del DB: accettato, perché (a) la regola
  dell'indirizzo relativo è stata validata su 4265 giochi senza falsi positivi, (b) il codice
  riscritto è equivalente e ri-configurabile, (c) il DB si reimporta.
- I commit non devono mai toccare `pr-prep/app`: verrà rigenerato da `ventuno` a lavoro finito.

## 10. Verifica sul dispositivo (prima del merge in `ventuno`)

Build `assembleGitHubNightlyProfiling`, `install -r` sulla Thor (firma invariata, dati conservati):
1. SGP EN: cartella 40 → selettore → Pikachu 25 → L+R in erba alta → Pikachu; savestate e rewind
   senza residui (come nel test del 12/09).
2. SGP EN: cartella 44 → Hardy; 45 → Lonely: 44 si spegne. 49 → Livello 7: il livello di 40 si spegne.
3. Black o White (se disponibile): `Wild Pokemon Modifier - Generation 1` collassata → Bulbasaur →
   Select in erba → Bulbasaur; `Mostra tutte` → 151 righe.
4. HeartGold USA (se disponibile): `Wild Pokemon Modifier v1` → Pikachu → tieni L in erba → Pikachu.
5. Lista cheat di un gioco senza famiglie (es. 007): invariata.
