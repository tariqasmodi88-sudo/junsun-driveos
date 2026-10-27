# Passaggio di consegne — stato del progetto (v0.6.4, 27/10/2026)

Questo file serve a chiunque riprenda il lavoro (una persona o un altro assistente AI):
contiene lo stato reale, cosa è verificato sulla radio e cosa resta da fare.

## Come si lavora
- Codice: Kotlin + Jetpack Compose, una sola Activity (`MainActivity.kt`), minSdk 29.
- **Non si compila in locale**: ogni push su `main` viene compilato e testato da GitHub Actions
  (`.github/workflows/build.yml`), che pubblica l'APK come release `build-N`.
  Link fisso all'ultima: `releases/latest/download/DriveOS.apk`.
- Logica pura in file separati con test JUnit (`app/src/test`). Le parti Android/Compose
  si verificano solo con la build CI e poi sulla radio.
- Regola del progetto: **mai dati inventati**. Se un valore non arriva dal firmware, non si mostra.

## La radio (dati reali dagli export)
- Junsun FF_866X, Android 10, firmware **ForFan**, scheda **Hazens**, suite media **TS**, voce **TXZ**,
  CarPlay/Android Auto wireless con **SpeedPlay** (`com.suding.speedplay`).
- Auto: VW Tiguan 2016.

## Segnali CONFERMATI dal registro di guida (25/10, 1 ora)
| Cosa | Segnale | Valori |
|---|---|---|
| Fari | `prop:forfan_light_state` | on / off |
| Retromarcia | broadcast `com.forfan.operator_reversing`, extra `operate` | 1 / 0 |
| Retrocamera | broadcast `com.ts.rearview_state`, extra `show_state` | 1 / 0 |
| Telefono vivavoce | broadcast `com.globalconstant.BROADCAST_RESPONSE_HFPSTATUS`, extra `hfpStatus` (+ `name`, `number` in chiamata) | 0 = scollegato |
| CarPlay/AA attivo | `prop:sys.speedplay.connection.state` | 1 / 0 |
| Quadro spento | broadcast `com.cusc.action.ACC_OFF` | evento |
| App in primo piano | `prop:forfan.want2start` | nome pacchetto |
| Navigatore di sistema | `prop:forfan.navi.name` | es. Google Maps |

## NON disponibili dal firmware
- **Carburante, temperature, olio, pressione pneumatici, porte**: `com.ts.can.BROADCAST_CAN_INFO`
  esiste nel codice ma **non è mai stato inviato** in un'ora di guida. Il firmware non espone
  la telemetria alle altre app. Via realistica: **adattatore OBD-II ELM327 Bluetooth**
  (lo stack Bluetooth Android della radio funziona: si vedono ACL_CONNECTED/DISCONNECTED).
- **Radio FM**: frequenza e comandi del sintonizzatore non ancora individuati. Nel registro la radio
  Junsun non risulta aperta: serve un registro con la radio Junsun aperta e 3–4 cambi di stazione.

## Fatto nella v0.6.4
- Avvio intelligente scatta anche dal vivavoce TS (`hfpStatus`), non solo dal Bluetooth Android.
- Non avvia nulla se CarPlay/AA (SpeedPlay) è già attivo, per non sovrapporre due sorgenti audio.
- Striscia stato sulla Home: Telefono collegato / CarPlay-AA attivo, oltre a fari e retromarcia.
- Registro pulito: esclusi i canali rumorosi (battito TXZ, modem cellulare: 660 righe su 2050).
- Intestazione CSV sempre presente anche dopo "Azzera registro".

## Prossimi passi, in ordine
1. Registro con la **radio Junsun aperta** e cambi di stazione → trovare come pilotare il tuner
   (`radio/RadioController.kt`, profili in `controlProfiles`).
2. **OBD-II (ELM327 Bluetooth SPP)**: carburante, temperatura liquido, giri, velocità, tensione,
   codici errore. Nuovo pacchetto `obd/`, valori nella striscia stato (`VehicleStatus.knownStatus`).
3. Verifica sulla radio delle funzioni segnate "Da verificare" in `docs/REGISTRO_TEST.md`.

## File principali
- `vehicle/VehicleStatus.kt` — segnali noti di questa radio (`KnownVehicleSignals`) e stato Home.
- `vehicle/VehicleRepository.kt` — lettura proprietà/broadcast, `knownVehicleActions`, `noisyActions`.
- `vehicle/VehicleLogService.kt`, `VehicleLog.kt`, `LogFormat.kt` — registrazione continua.
- `automation/` — avvio intelligente. `radio/` — radio integrata. `ui/` — schermate.
