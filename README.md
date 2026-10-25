# Junsun DriveOS — launcher per autoradio Android

Launcher (schermata Home) per l'autoradio **Junsun FF_866X** ("V1 Pro") montata sulla
**VW Tiguan 2016**. È una normale app Android: **non modifica firmware, partizioni,
impostazioni di fabbrica né l'MCU** e non richiede root né permessi speciali.

Versione attuale: **0.5.0**

### Novità 0.5.0

- **Barra in basso riorganizzata:** Home · Naviga · **Radio** · Musica · Telefono · App.
  *Impostazioni* è ora nell'elenco App (in testa).
- **Tema automatico dai fari, senza calibrazione:** su questo firmware ForFan DriveOS legge
  `forfan_light_state` → fari accesi = tema scuro.
- **Navigatore auto-rilevato:** usa quello impostato nel sistema (Google Maps) se non ne hai scelto uno.
- **Striscia stato veicolo sulla Home:** fari e i valori che colleghi (carburante, temperatura…)
  sempre visibili; le **spie compaiono solo quando scattano**. Mostra solo dati reali.
- Avvio intelligente: riconosce anche il dongle **SpeedPlay** oltre ad Android Auto.

### Novità 0.4.0

- **Avvio intelligente (sostituisce AutoLink):** quando il telefono si collega in Bluetooth,
  DriveOS avvia da solo la musica — **Spotify se c'è rete, brani locali se offline** — e,
  se vuoi, la navigazione, **restando sulla Home DriveOS**. In *Impostazioni → Avvio intelligente*.
- **Radio integrata:** la radio ora vive **dentro DriveOS** (frequenza, ricerca, preset) e non
  apre più la schermata Junsun. Mostra la frequenza solo se il firmware la pubblica; i comandi
  sono sperimentali finché non adattiamo i parametri esatti di questa radio (vedi export).
- **Export della scansione veicolo:** pulsante *Esporta* nella schermata Veicolo, salva
  `driveos_veicolo_*.json` in *Download/DriveOS* e sulla chiavetta.

### Novità 0.3.0

- **Export JSON corretto** (non va più in crash): salva in *Download/DriveOS* e sulla
  chiavetta USB, senza il selettore di sistema che mancava sul firmware Junsun.
- **Tema automatico**: scuro di notte, chiaro di giorno. Se colleghi il segnale dei fari
  (schermata *Veicolo*), usa quelli; altrimenti la modalità notte di Android o alba/tramonto.
- **Veicolo (CAN)**: DriveOS legge i segnali che il firmware pubblica (proprietà, impostazioni,
  broadcast delle app Junsun) e permette di mostrarne i valori (carburante, pressione pneumatici,
  spie). Si scoprono con una scansione e muovendo il veicolo. *Nessun valore viene inventato.*
- **Pannello controlli**: volume e luminosità con grafica DriveOS, dalla barra in alto nella Home.
- **Radio**: memorizzazione dei canali FM con nome, indipendente dall'app Junsun.

## Cosa fa

**Home a quattro schede**, che si adattano allo spazio disponibile:

| Scheda | Contenuto | Tocco sulla scheda |
|---|---|---|
| Navigazione | Casa, Lavoro, Cerca, destinazioni recenti; durante la guida mostra la prossima manovra e l'orario di arrivo | Vista a tutto schermo con ricerca, dettatura vocale e recenti |
| Musica | Copertina, titolo, artista, avanzamento, comandi ⏮ ⏯ ⏭ | Vista a tutto schermo con comandi grandi ed elenco sorgenti |
| Ora e data | Orologio grande | Orologio a tutto schermo con il prossimo impegno |
| Agenda | Prossimi impegni di Google Calendar | Agenda di 14 giorni, con "Naviga" verso il luogo dell'evento |

**6 layout** selezionabili in *Impostazioni → Schermata Home*, con anteprima:
Navigazione in primo piano (predefinito), Griglia, Musica in primo piano, Cruscotto, Colonne, Essenziale.

Le dimensioni seguono le linee guida Google per le auto: aree di tocco fino a 76 dp,
testo principale grande, contrasto elevato, tema scuro di notte.

Altre funzioni: elenco app con ricerca e preferiti in testa, scelta delle app di
navigazione/musica/telefono, tema scuro/chiaro/sistema con 5 colori, Info dispositivo
con esportazione JSON.

Radio FM, Bluetooth/vivavoce, CAN, DSP e retrocamera **restano le app Junsun**:
DriveOS le apre e basta. Nessuna funzione mostra dati inventati.

### Cosa serve attivare (una volta)

| Funzione | Permesso | Dove |
|---|---|---|
| Brano in riproduzione e indicazioni del navigatore | Accesso alle notifiche | Scheda Musica → *Attiva*, oppure *Impostazioni → Permessi* |
| Agenda | Lettura calendario | Scheda Agenda → *Collega* |
| Dettatura destinazione | Nessuno (usa l'app Google) | Pulsante microfono nella vista Navigazione |

L'accesso alle notifiche viene usato **solo** per il lettore multimediale attivo e per la
notifica di guida dell'app di navigazione; tutte le altre notifiche vengono ignorate e
nulla viene salvato. Se il firmware nasconde la pagina delle impostazioni, si può
concedere via ADB:

```
adb shell cmd notification allow_listener it.driveos.launcher/it.driveos.launcher.notifications.DriveNotificationListener
```

**Agenda:** funziona se sulla radio c'è un account Google con la sincronizzazione del
Calendario attiva. Se manca, installa Google Calendar dal Play Store.

**Durante la guida:** avvii il percorso da DriveOS, si apre Maps/Waze; con il tasto Home
torni a DriveOS e la scheda Navigazione continua a mostrare la prossima manovra
(letta dalla notifica del navigatore). "Torna alla guida" riapre la mappa.
Una mappa interattiva dentro DriveOS non è possibile: Google Maps e Waze non
permettono di incorporare la loro guida in altre app.

## 0. Scaricare l'APK già compilato (consigliato)

Ogni modifica caricata su GitHub viene compilata e testata automaticamente (GitHub Actions).
L'ultima versione si scarica sempre da:

<https://github.com/tariqasmodi88-sudo/junsun-driveos/releases/latest/download/DriveOS.apk>

Aprilo con Chrome direttamente sull'autoradio e installalo.

## 1. Compilare l'APK in locale (facoltativo: Android Studio)

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
4. Elenco *App*: tieni premute le app da avere in testa ai Preferiti (es. Radio, Car Info, DSP).
5. Attiva l'accesso alle notifiche (scheda Musica → *Attiva*) e collega l'agenda (scheda Agenda → *Collega*).
6. *Impostazioni → Info dispositivo → Esporta JSON* su chiavetta e invia il file:
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

Solo `READ_CALENDAR` (chiesto quando colleghi l'agenda) e l'accesso alle notifiche
(concesso da te nelle impostazioni di Android). Nessun permesso di posizione, microfono
o rete viene richiesto da DriveOS.

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
