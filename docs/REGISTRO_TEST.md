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

## 2026-10-25 — v0.3.0

Nuove funzioni: correzione crash export, tema automatico (fari/alba-tramonto), sonda
veicolo (CAN), pannello controlli (volume/luminosità), radio con memorizzazione canali.

| Test | Esito | Note |
|---|---|---|
| 20 nuovi test unitari (StringScanner, ActionFilter, segnali, candidati, alba/tramonto, radio, controlli) | **20/20 superati** | Compilatore Kotlin standalone 2.0.20 |
| Compilazione APK v0.3 (Compose) | Da verificare | GitHub Actions |
| Export JSON senza crash (salva in Download/DriveOS e su chiavetta) | Da verificare | Sostituito il selettore di sistema che causava il crash |
| Tema automatico da alba/tramonto | Da verificare | Posizione predefinita Italia centrale, impostabile |
| Scansione app veicolo + lettura segnali CAN | **Da verificare sul veicolo** | Dipende da cosa il firmware Junsun pubblica |
| Calibrazione fari (registra acceso/spento → candidato) | **Da verificare sul veicolo** | Serve per il tema automatico dai fari |
| Pannello controlli: volume reale | Da verificare | AudioManager, nessun permesso |
| Pannello controlli: luminosità finestra / sistema | Da verificare | Sistema solo con permesso WRITE_SETTINGS |
| Radio: salvataggio e apertura canali | Da verificare | La sintonizzazione diretta dipende da un comando dell'app radio, da scoprire |
