# Registro test

Ogni voce riporta solo test **realmente eseguiti**. "Da verificare" = non ancora fatto.

## 2026-10-25 — v0.1.0

Ambiente: workspace cloud, nessun dispositivo Android, nessun accesso ai repository Maven/Google.

| Test | Esito | Note |
|---|---|---|
| Compilazione logica pura (`AppKey`, `FavoritesCodec`, `AppSearch`, `NavIntentBuilder`) con Kotlin 2.0.21 | Superato | Compilatore Kotlin standalone, senza Android |
| 18 test unitari della logica pura | **18/18 superati** | Eseguiti con un runner minimale compatibile con le annotazioni JUnit |
| Compilazione completa APK (Compose, DataStore, AndroidX) | **Superato** | GitHub Actions, build-1 (25/10/2026): APK debug 24 MB |
| `gradlew testDebugUnitTest` con JUnit reale | **Superato** | GitHub Actions, build-1 |
| Installazione su Junsun FF_866X | Da verificare | |
| Comparsa tra le app Home / scelta come predefinita | Da verificare | |
| Riavvio radio con DriveOS predefinito | Da verificare | |
| Avvio Google Maps / Waze / iGO / Navi | Da verificare | Maps e Waze vanno installati se assenti |
| "Portami a casa" con Maps e con Waze | Da verificare | |
| Radio, Bluetooth, retrocamera funzionanti con DriveOS attivo | Da verificare | Regressione: obbligatorio |
| Ripristino launcher Junsun | Da verificare | |
| Esportazione JSON su chiavetta | Da verificare | |

## Come compilare questo registro sulla radio

Per ogni riga "Da verificare": esegui il test e annota esito, data e cosa hai visto
(una foto dello schermo è sufficiente in caso di errore).

## 2026-10-25 — v0.2.0

| Test | Esito | Note |
|---|---|---|
| 34 test unitari della logica (layout, agenda, cronologia, avanzamento brano, ricerca, navigazione) | **Superati** | Eseguiti in locale e su GitHub Actions |
| Compilazione APK v0.2 | **Superato** | GitHub Actions, build 8 (le build 3–7 fallite per un file danneggiato da una sostituzione automatica, poi ripristinato) |
| Layout e schede sulla Junsun (6 layout) | Da verificare | |
| Accesso notifiche: brano in riproduzione e comandi | Da verificare | Dipende dalle app che espongono una MediaSession |
| Indicazioni di guida dalla notifica di Maps/Waze | Da verificare | |
| Agenda Google Calendar | Da verificare | Richiede account Google sincronizzato sulla radio |
| Dettatura destinazione | Da verificare | Richiede l'app Google |
