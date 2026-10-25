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
