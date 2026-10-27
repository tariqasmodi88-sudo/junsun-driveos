# Rapporto di compatibilità (stato al 2026-10-25)

## Dispositivo: dati confermati (da foto delle schermate)

- Modello: **FF_866X** (venduto come Junsun V1/V1 Pro)
- Build: `alps-mp-q0.mp5-V2.122.1_pvetec.q0mp5.spm8666p1.64_P2`
- Numero build: `sys_mssi_spm_64_cn-userdebug 10 QP1A.190711.020 p5spm8666p164P2 test-keys`
  → **Android 10 (API 29)**: "Q"/"10" nel numero build; l'etichetta "11" è cosmetica.
- RAM: 8,2 GB (Opzioni sviluppatore → Memoria)
- Debug USB: presente. ADB di rete: non disponibile nel menu (Android 10).
- Veicolo: VW Tiguan (5N) 2016 → OBD-II su CAN ISO 15765-4.

## App presenti (da foto)

Junsun: Navi, Radio, Video, Music, Bluetooth, BT Music, AV_IN, DSP, Car Info,
ApkInstaller, AutoLink. Google: Play Store, Google, Gemini, Chrome, YouTube.
Altre: iGO PAL, GpsTest, Contatti, Files, FileManager, Gallery.

## Dati confermati dall'export DriveOS (2026-10-25)

- Vendor firmware: **ForFan** (custom "tengshi"). Proprietà `forfan.*` presenti.
- **Fari/illuminazione: `prop:forfan_light_state` = on/off** → usato per il tema automatico (nessuna calibrazione).
- Navigatore di sistema: `prop:forfan.navi.name = com.google.android.apps.maps` → DriveOS lo auto-seleziona.
- Proiezione telefono: dongle **SpeedPlay** `com.suding.speedplay` (wireless CarPlay/AndroidAuto) → usato dall'avvio intelligente.
- Servizio CAN: **`com.ts.car.client`** (azioni note: sleep/wakeup, AVM 360). 
- **Telemetria (carburante, olio, temperatura, pneumatici): NON esposta** come proprietà né come broadcast
  nell'export. Le `*.radio.*` presenti sono il modem cellulare (RIL), non la radio FM.
  → Per questi valori servono: broadcast del servizio `com.ts.car.client` catturati **in marcia**,
  oppure un adattatore **OBD-II (ELM327)** collegato alla Tiguan. La radio FM non espone frequenza nell'export statico.

## Canali trovati (scansione estesa 2026-10-25, board Hazens / suite TS-TXZ)

Azioni broadcast reali da ascoltare (ora in `knownVehicleActions`):
- **`com.ts.can.BROADCAST_CAN_INFO`** — dati CAN (carburante, temperature, porte…). Da decodificare
  con il registro in marcia: gli extra possono essere array di byte/interi (ora resi leggibili).
- `com.autoai.iot.tpms` — pressione pneumatici (TPMS).
- `comm.power.ENTER_REVERSE` / `comm.power.QUIT_REVERSE` — retromarcia.
- `forfan.adas.turnlight_state` — frecce. `com.cusc.action.ACC_OFF` — quadro.
- `mediatek.intent.action.BATTERY_WARNING` / `THERMAL_WARNING` — avvisi.
- `vendor.hazens.vehicle.info.vin` — VIN (board **Hazens**).
- Radio: suite media **TS** (`com.ts.main.radio`, `com.ts.dvdplayer.*`), voce **TXZ** (`txz.fm.*`).
- Proiezione: **SpeedPlay** (`com.suding.speedplay`). Comandi UI: `forfan.intent.action.VOLUME/MUTE/EQ/MODE…`.

Prossimo passo: registro continuo in marcia → decodificare gli extra di `BROADCAST_CAN_INFO`
(mappare quali indici dell'array sono carburante/temperatura/pneumatici) e individuare l'azione
di sintonizzazione radio.

## Ipotesi da confermare (con l'esportazione JSON di DriveOS)

- SoC: fonti discordanti (MediaTek MT8667 o Unisoc UIS8666); "alps/mssi" suggerisce MediaTek.
- Risoluzione: probabilmente 1280×720.
- Riconoscimento vocale italiano tramite l'app Google.
- Gestione della scelta Home da parte del firmware.

## Integrazione per area

| Area | Approccio | Fattibilità |
|---|---|---|
| Launcher Home | intent-filter HOME standard | Alta (da verificare sul firmware) |
| Navigazione (avvio, destinazione) | URI ufficiali Maps/Waze, `geo:` | Alta |
| Stato navigazione nel launcher | lettura notifiche, con consenso | Parziale, fase successiva |
| Musica: brano e controlli | MediaSession + accesso notifiche | Probabile per Spotify/YouTube; da verificare per app Junsun |
| Voce | SpeechRecognizer (app Google) + microfono | Probabile |
| Telefono/vivavoce | app "Bluetooth" Junsun | Solo avvio esterno |
| Radio FM/RDS, DSP, Car Info, CAN | app Junsun | Solo avvio esterno |
| Retrocamera | gestita da MCU/sistema | Nessun intervento |
| OBD-II | ELM327 Bluetooth Classic o USB | Da verificare lo stack Bluetooth Android |

## Logcat via ADB Wi-Fi (27/10/2026, Bugjaeger dal telefono, radio 10.39.13.66:5555)

- ADB di rete funziona: `service.adb.tcp.port=5555`, basta attivare "Debug USB" sulla radio.
- **Portiere, sensori di parcheggio e CAN sono decodificati DENTRO `com.ts.MainUI`** (app di sistema,
  uid 1000, processo unico): tag `CanFunc` (GotoWin, mbRadarUIUpdate), `CanDoorMsgbox`
  (`SetShowFlg = true/false` quando una portiera si apre/chiude), `CanRadarActivity`.
  I dati non escono dal processo come broadcast: per questo DriveOS non li vede.
- MainUI legge l'MCU tramite `/proc/forfan_*` (ioctl), accesso riservato alle app di sistema.
- Retro: `com.ts.MainUI/com.ts.backcar.BackcarMainActivity`. Luci: MainUI riceve `BROADCAST_ADAS_LIGHT_STATE`.
- La radio è anch'essa dentro `com.ts.MainUI` (azione `com.ts.main.radio`); nel log del 27/10 non era aperta.
- Prossimo: `dumpsys package com.ts.MainUI` (componenti esportati) e logcat con la radio aperta.
