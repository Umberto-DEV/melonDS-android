# Regola di pulizia disco (dal 12/09/2026)
Principio: ogni artefatto di prova ha un ciclo di vita creato → validato → registrato nel report → cancellato. Restano solo i report (testo) e, se serve, un'immagine di prova piccola. Niente APK, ROM, log grezzi o build residui a fine sessione.
Permanenti (necessari, ~13 GB): SDK ~/Library/Android/sdk (8,7 GB: NDK 2,8, system image 3,8, emulator 1,1), cache Gradle ~/.gradle (2 GB), AVD melonds-test (2,1 GB, da cancellare a fine campagna: `avdmanager delete avd -n melonds-test`, si ricrea in secondi).
Temporanei, da cancellare dopo ogni test validato: app/build/outputs (APK), copie ROM/BIOS dentro l'AVD (/sdcard/Download), screenshot e logcat nello scratchpad di sessione, log gradle.
A fine sessione di lavoro: `sh ./gradlew clean` (app/build ~1,9 GB) e `rm -rf app/.cxx` (~1 GB) se non si ricompila a breve; emulatore spento (`adb emu kill`, verificare con pgrep).
Stato al 12/09 sera: liberi 34 GB (erano 59 prima della toolchain); scratchpad ridotto a 176 KB; APK cancellate; ROM/BIOS rimosse dall'AVD; emulatore fermato.
