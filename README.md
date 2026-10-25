# Junsun DriveOS — launcher per autoradio Android

Launcher (schermata Home) per l'autoradio **Junsun FF_866X** ("V1 Pro") montata sulla
**VW Tiguan 2016**. È una normale app Android: **non modifica firmware, partizioni,
impostazioni di fabbrica né l'MCU** e non richiede root né permessi speciali.

Versione attuale: **0.1.0**: prototipo reale (Fase 3 + avvio della navigazione, Fase 4/A).

## Cosa fa la v0.1

| Funzione | Stato |
|---|---|
| Schermata Home con orologio, scheda navigazione e preferiti | Implementata |
| Elenco app con ricerca (ignora accenti e maiuscole) | Implementata |
| Preferiti: tieni premuta un'app nell'elenco | Implementata |
| Barra inferiore: Home, Naviga, Musica, Telefono, App, Impostazioni | Implementata |
| Scelta dell'app di navigazione, musica e telefono (salvata) | Implementata |
| "Portami a casa" e "Cerca destinazione" (Google Maps, Waze, altre app via `geo:`) | Implementata |
| Tema scuro/chiaro/sistema + 5 colori d'accento | Implementata |
| Info dispositivo + esportazione JSON (sostituisce l'ADB) | Implementata |
| Aggiornamento automatico se installi o rimuovi app | Implementata |
| Radio FM, Bluetooth, CAN, DSP, retrocamera | **Restano le app Junsun**: DriveOS le apre e basta |
| Brano in riproduzione, comandi vocali, OBD-II, stato GPS/BT | Fasi successive (non presenti, non simulate) |

Nessuna funzione mostra dati inventati: se qualcosa non è configurato, lo dice.

## 1. Compilare l'APK (una volta sola: installare Android Studio)

Visual Studio Code da solo non basta per le app Android: serve **Android Studio** (gratuito),
che include l'SDK Android e il JDK.

1. Scarica e installa Android Studio da <https://developer.android.com/studio>
   (opzioni predefinite; al primo avvio lascia scaricare l'SDK proposto).
2. *File → Open* → seleziona la cartella `junsun-driveos`.
3. Attendi la sincronizzazione ("Gradle sync"): la prima volta scarica Gradle 8.9,
   l'SDK 34 e le librerie, e può richiedere 10–20 minuti.
4. *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
5. L'APK è in `app\build\outputs\apk\debug\app-debug.apk`
   (Android Studio mostra un link "locate").

Da riga di comando (dopo che Android Studio ha installato l'SDK), dentro la cartella:

```
gradlew.bat assembleDebug     (crea l'APK)
gradlew.bat test              (esegue i test automatici)
```

## 2. Installare sull'autoradio

1. Copia `app-debug.apk` su una chiavetta USB e inseriscila nella radio.
2. Apri **ApkInstaller** (oppure FileManager), seleziona il file e installa.
   Se compare l'avviso sulle "origini sconosciute", consenti per quell'app.
3. **Prima prova senza renderla predefinita**: apri "DriveOS" dall'elenco app.
   Il launcher Junsun resta quello di sistema.

## 3. Usarla come schermata Home

- In DriveOS: *Impostazioni → Launcher predefinito* (apre l'impostazione di Android),
  scegli **DriveOS**. In alternativa premi il tasto Home e, se Android lo chiede, scegli DriveOS → "Sempre".
- Se il firmware Junsun non mostra la scelta della Home, il tasto Home potrebbe
  essere legato al launcher originale: annotalo nel registro test (vedi `docs/`).

## 4. Tornare al launcher originale Junsun

Il ripristino non richiede nulla di particolare:

1. In DriveOS *Impostazioni → Launcher predefinito* → scegli il launcher Junsun; **oppure**
2. *Impostazioni Android → App → App predefinite → App Home* → launcher Junsun; **oppure**
3. disinstalla DriveOS (*Impostazioni Android → App → DriveOS → Disinstalla*):
   Android torna automaticamente al launcher Junsun.

DriveOS non cancella né disattiva nulla del sistema originale.

## 5. Primo utilizzo consigliato

1. *Impostazioni → Navigazione*: scegli Google Maps, Waze, iGO o "Navi".
2. *Impostazioni → Indirizzo di casa*.
3. *Impostazioni → Musica* (es. YouTube, Spotify o "Music" Junsun) e *Telefono*
   (l'app "Bluetooth" Junsun, che gestisce il vivavoce).
4. Elenco *App*: tieni premute le app da avere in Home (es. Radio, Car Info, DSP).
5. *Impostazioni → Info dispositivo → Esporta JSON* su chiavetta e invia il file:
   serve per progettare le fasi successive con dati reali.

## Struttura

```
app/src/main/java/it/driveos/launcher/
  MainActivity.kt        activity Home, stato schermate, dialoghi
  DriveOSApp.kt          dipendenze condivise
  apps/                  elenco app, chiavi, preferiti, ricerca
  nav/                   costruzione URI di navigazione (Maps, Waze, geo:)
  settings/              preferenze persistenti (DataStore)
  system/Launcher.kt     avvio sicuro di altre app (gli errori non chiudono il launcher)
  device/                raccolta info dispositivo (sostituisce ADB)
  ui/                    schermate Compose e tema
app/src/test/            test unitari (JUnit)
docs/                    compatibilità, registro test
tools/                   script ADB di acquisizione (facoltativo)
```

Tecnologie: Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00), Material 3,
DataStore 1.1.1, AGP 8.5.2, Gradle 8.9, minSdk 29 (Android 10), targetSdk 34.

## Permessi

**Nessuno.** Le funzioni future che ne richiederanno (microfono per i comandi vocali,
accesso alle notifiche per il brano in riproduzione, Bluetooth per l'OBD) li chiederanno
solo quando le attivi, con una spiegazione.

## Firma release

Il repository non contiene chiavi. Per una build firmata: *Build → Generate Signed
App Bundle / APK*, crea un keystore **fuori** dalla cartella del progetto e conservalo.
Per l'uso personale l'APK debug è sufficiente.

## Risoluzione problemi

| Problema | Soluzione |
|---|---|
| Gradle sync fallisce | Verifica la connessione, poi *File → Sync Project with Gradle Files*. In *Settings → Build Tools → Gradle* usa il JDK incluso ("jbr-17" o superiore). |
| ApkInstaller rifiuta l'APK | Se esiste già una versione firmata diversamente, disinstalla prima quella vecchia. |
| DriveOS non compare tra le Home | Riavvia la radio dopo l'installazione; se ancora assente, il firmware limita la scelta: segnalalo. |
| Un'app non si apre ("potrebbe essere stata disinstallata") | Riscegli l'app in Impostazioni o nei preferiti. |
| "Cerca destinazione" apre la mappa ma non avvia la guida | Con app diverse da Maps/Waze si usa `geo:`, che mostra il luogo; la navigazione va avviata nell'app. |
