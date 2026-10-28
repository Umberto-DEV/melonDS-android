# PR considerate e attribuzioni

Snapshot e corpi conservati in `../2026-10-28-main/open-app-prs.json` e `open-core-prs.json`. Le vecchie nostre PR presenti nello snapshot sono state successivamente chiuse. Questa campagna non copia nuove patch di terzi.

| Fonte | Titolo | Decisione |
|---|---|---|
| [app#1682](https://github.com/rafaelvcaetano/melonDS-android/pull/1682) | Add Wi-Fi settings and cheat search, improve stability and fix Android CI | Archivio originale conservato; chiusa durante consolidamento, sostituzione bloccata. |
| [app#1677](https://github.com/rafaelvcaetano/melonDS-android/pull/1677) | Add search box for cheats | Ricerca cheat adattata precedentemente con credito takanuva15; test di ricerca/navigation ancora inclusi. |
| [app#1589](https://github.com/rafaelvcaetano/melonDS-android/pull/1589) | Map phone sleep/wake to DS lid close/open | Lid/sleep adattato precedentemente con credito DLCSharp; nessuna nuova importazione. |
| [app#1676](https://github.com/rafaelvcaetano/melonDS-android/pull/1676) | Fast-forward audio: fix crackling with dynamic rate matching, plus optional pitch preservation | Non integrata: rate matching/pitch richiedono campioni audio e misure Android; non sostituire i bug PCM dimostrati con una riscrittura. |
| [app#1666](https://github.com/rafaelvcaetano/melonDS-android/pull/1666) | Restore sessions after Android process death | Parti mirate già adattate con credito corigne; recovery completo dopo process death non dichiarato verificato. |
| [app#1664](https://github.com/rafaelvcaetano/melonDS-android/pull/1664) | Sync RTC on lid open | Considerata per RTC; policy corrente opt-in soltanto in avanti, scenario manual lid/game-time ancora parziale. |
| [app#1524](https://github.com/rafaelvcaetano/melonDS-android/pull/1524) | Reduces fatal OutOfMemoryErrors on Ayn Thor and other recent devices when reading 7z files | Stack LZMA utile per audit precedente; limite memoria/serializzazione già presenti, nessuna promessa su ogni OOM. |
| [app#1657](https://github.com/rafaelvcaetano/melonDS-android/pull/1657) | Fix idle power draw while emulator UI is inactive | Correzione mirata pause/lid/render già presente; riscrittura più ampia non incorporata. |
| [app#1648](https://github.com/rafaelvcaetano/melonDS-android/pull/1648) | Fix ROM not found error when launching from external apps | Intent esterni adattati precedentemente con credito N3MI-DG. |
| [app#1608](https://github.com/rafaelvcaetano/melonDS-android/pull/1608) | Added hold to fast forward functionality | Hold fast-forward adattato precedentemente con credito WailAbou. |
| [app#1625](https://github.com/rafaelvcaetano/melonDS-android/pull/1625) | Mute on fast forward setting | Mute fast-forward adattato precedentemente con credito dysprosium; non corregge ogni distorsione. |
| [app#1606](https://github.com/rafaelvcaetano/melonDS-android/pull/1606) | Added combo hotkeys | Combo hotkeys adattati precedentemente con credito WailAbou. |
| [app#1035](https://github.com/rafaelvcaetano/melonDS-android/pull/1035) | Updated README with BIOS/Firmware requirements | Documentazione BIOS considerata; nessuna nuova dipendenza da BIOS proprietario nelle regressioni sintetiche. |
| [lib#19](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/19) | Fix DS/DSi compatibility, Wi-Fi power saving and JIT invalidation | Archivio originale conservato; chiusa, sostituzione bloccata. |
| [lib#17](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/17) | Enlarge SPU output ring buffer 2048 -> 8192 | Non integrata: più buffer non dimostra minore crackle e può aumentare latenza; serve misura Android. |
| [lib#1](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/1) | Add cheat enable tag, right now the enable tag from Cheat data class … | Non integrata in questa campagna: verificare contratto enable/disable con codici/ROM specifici; regressioni modifier/search esistenti conservate. |
| [lib#3](https://github.com/rafaelvcaetano/melonDS-android-lib/pull/3) | Only set firmware paths when internal firmware is not used | Non integrata in questa campagna: cambiamento dei percorsi firmware distinto dalle verifiche di lunghezza/errore NAND. |

La ricerca aggiuntiva `repo:rafaelvcaetano/melonDS-android cheat import` non ha individuato una patch equivalente per atomicità/cancellazione DAT. Il precedente [#1367](https://github.com/rafaelvcaetano/melonDS-android/issues/1367) è chiuso e collegato a#1305; i commenti segnalano già una nightly funzionante nel2024. Non attribuiamo a queste nuove correzioni la chiusura storica.
