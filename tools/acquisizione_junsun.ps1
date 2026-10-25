# ============================================================
#  Junsun DriveOS - FASE 1: acquisizione informazioni dispositivo
#  SOLA LETTURA: non installa nulla, non modifica impostazioni,
#  non tocca partizioni. Salva tutto in una cartella + ZIP.
#
#  Uso (PowerShell, nella cartella dello script):
#    .\acquisizione_junsun.ps1                  -> censimento completo
#    .\acquisizione_junsun.ps1 -Osserva radio   -> registra logcat durante
#                                                  un'operazione (Fase B)
#    .\acquisizione_junsun.ps1 -Serial 192.168.1.50:5555
#
#  Se Windows blocca lo script:
#    powershell -ExecutionPolicy Bypass -File .\acquisizione_junsun.ps1
# ============================================================
param(
    [string]$Serial = "",
    [string]$Osserva = ""
)

$ErrorActionPreference = "Continue"
$adb = "adb"
$adbArgs = @()
if ($Serial) { $adbArgs = @("-s", $Serial) }

if (-not (Get-Command $adb -ErrorAction SilentlyContinue)) {
    Write-Host "adb non trovato. Installa Android Platform Tools e aggiungilo al PATH." -ForegroundColor Red
    exit 1
}

$state = (& $adb @adbArgs get-state 2>&1) | Out-String
if ($state.Trim() -ne "device") {
    Write-Host "Dispositivo non pronto: $state" -ForegroundColor Red
    Write-Host "Controlla con: adb devices -l  (se 'unauthorized', accetta l'avviso sull'autoradio)"
    exit 1
}

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$base  = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }

# ---------- MODALITA' OSSERVAZIONE (Fase B) ----------
if ($Osserva) {
    $outDir = Join-Path $base "junsun_osserva_$($Osserva)_$stamp"
    New-Item -ItemType Directory -Path $outDir | Out-Null
    $logFile = Join-Path $outDir "logcat_$Osserva.txt"

    & $adb @adbArgs logcat -c 2>&1 | Out-Null
    Write-Host "Registrazione logcat avviata per: $Osserva" -ForegroundColor Cyan
    Write-Host "Esegui ORA l'operazione sull'autoradio, poi premi INVIO qui."

    $argList = $adbArgs + @("logcat", "-v", "threadtime", "-b", "main,system,events,crash")
    $p = Start-Process -FilePath $adb -ArgumentList $argList -RedirectStandardOutput $logFile `
         -NoNewWindow -PassThru
    [void](Read-Host)
    Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue

    # Stato a fine operazione
    & $adb @adbArgs shell "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'" 2>&1 |
        Out-File -Encoding utf8 (Join-Path $outDir "activity_in_primo_piano.txt")
    & $adb @adbArgs shell "dumpsys media_session" 2>&1 |
        Out-File -Encoding utf8 (Join-Path $outDir "media_session.txt")

    Write-Host "Salvato in: $outDir" -ForegroundColor Green
    exit 0
}

# ---------- CENSIMENTO COMPLETO (Fase A) ----------
$outDir = Join-Path $base "junsun_report_$stamp"
New-Item -ItemType Directory -Path $outDir | Out-Null

function Save([string]$file, [string]$shellCmd) {
    Write-Host "-> $file"
    & $adb @adbArgs shell $shellCmd 2>&1 | Out-File -Encoding utf8 (Join-Path $outDir $file)
}

& $adb @adbArgs devices -l 2>&1 | Out-File -Encoding utf8 (Join-Path $outDir "00_adb_devices.txt")

# Sistema e build
Save "01_getprop_completo.txt"   "getprop"
Save "02_build_sintesi.txt"      "echo release=`$(getprop ro.build.version.release); echo sdk=`$(getprop ro.build.version.sdk); echo fingerprint=`$(getprop ro.build.fingerprint); echo display_id=`$(getprop ro.build.display.id); echo security_patch=`$(getprop ro.build.version.security_patch); echo abi=`$(getprop ro.product.cpu.abilist); echo platform=`$(getprop ro.board.platform); echo hardware=`$(getprop ro.hardware); echo model=`$(getprop ro.product.model); echo brand=`$(getprop ro.product.brand); echo manufacturer=`$(getprop ro.product.manufacturer)"
Save "03_cpuinfo.txt"            "cat /proc/cpuinfo"
Save "04_meminfo.txt"            "cat /proc/meminfo"
Save "05_storage.txt"            "df -h"
Save "06_kernel.txt"             "uname -a"

# Display
Save "10_display_wm.txt"         "wm size; wm density"
Save "11_display_dumpsys.txt"    "dumpsys display"

# Package e componenti
Save "20_pkg_tutti.txt"          "pm list packages -f -i -U"
Save "21_pkg_sistema.txt"        "pm list packages -s"
Save "22_pkg_utente.txt"         "pm list packages -3"
Save "23_features.txt"           "pm list features"
Save "24_home_attuale.txt"       "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
Save "25_home_candidati.txt"     "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
Save "26_launcher_app.txt"       "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"
Save "27_dumpsys_package.txt"    "dumpsys package"
Save "28_servizi_binder.txt"     "service list"

# App specifiche (navigazione, musica, voce, Google)
Save "30_app_chiave.txt"         "pm list packages | grep -iE 'maps|waze|spotify|music|youtube|google|gms|vending|assistant|bard|gemini|syu|fyt|junsun|radio|canbus|bt|bluetooth|obd|carplay|android.auto|zlink|tlink'"
Save "31_voce_assistente.txt"    "settings get secure voice_interaction_service; settings get secure assistant; settings get secure voice_recognition_service"
Save "32_speech_recognizer.txt"  "cmd package query-services --brief -a android.speech.RecognitionService"

# Connettivita' e sensori
Save "40_bluetooth.txt"          "dumpsys bluetooth_manager"
Save "41_location.txt"           "dumpsys location"
Save "42_connectivity.txt"       "dumpsys connectivity"
Save "43_wifi.txt"               "dumpsys wifi | head -n 80"
Save "44_telephony.txt"          "dumpsys telephony.registry | head -n 80"
Save "45_usb.txt"                "dumpsys usb"
Save "46_sensori.txt"            "dumpsys sensorservice"

# Multimedia e audio
Save "50_media_session.txt"      "dumpsys media_session"
Save "51_audio.txt"              "dumpsys audio"

# Input (tasti al volante: dispositivi di input esposti)
Save "60_input.txt"              "dumpsys input"

# Stato attuale e log
Save "70_activity_attuale.txt"   "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'"
Save "71_logcat_snapshot.txt"    "logcat -d -v threadtime -b main,system,crash"

# Screenshot della home originale
Write-Host "-> screenshot_home.png"
& $adb @adbArgs shell screencap -p /sdcard/driveos_home.png 2>&1 | Out-Null
& $adb @adbArgs pull /sdcard/driveos_home.png (Join-Path $outDir "screenshot_home.png") 2>&1 | Out-Null
& $adb @adbArgs shell rm /sdcard/driveos_home.png 2>&1 | Out-Null

$zip = "$outDir.zip"
Compress-Archive -Path "$outDir\*" -DestinationPath $zip -Force
Write-Host ""
Write-Host "Fatto. Cartella: $outDir" -ForegroundColor Green
Write-Host "Archivio da inviare: $zip" -ForegroundColor Green
Write-Host "Nota: il report contiene l'elenco delle app installate e nomi Bluetooth/Wi-Fi."
Write-Host "Controllalo prima di condividerlo se vuoi rimuovere dati personali."
