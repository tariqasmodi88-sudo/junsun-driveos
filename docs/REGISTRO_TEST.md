# Registro test

Ogni voce riporta solo test **realmente eseguiti**. "Da verificare" = non ancora fatto.

## 2026-10-25 — v0.1.0

Ambiente: workspace cloud, nessun dispositivo Android, nessun accesso ai repository Maven/Google.

| Test | Esito | Note |
|---|---|---|
| Compilazione logica pura (`AppKey`, `FavoritesCodec`, `AppSearch`, `NavIntentBuilder`) con Kotlin 2.0.21 | Superato | Compilatore Kotlin standalone, senza Android |
| 18 test unitari della logica pura | **18/18 superati** | Eseguiti con un runner minimale compatibile con le annotazioni JUnit |
| Compilazione completa APK (Compose, DataStore, AndroidX) | **Da verificare** | Richiede Android Studio sul PC (SDK non scaricabile da qui) |
| `gradlew test` con JUnit reale | Da verificare | |
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
