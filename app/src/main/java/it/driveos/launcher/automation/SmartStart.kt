package it.driveos.launcher.automation

/**
 * "Sostituisce AutoLink in modo intelligente": quando il telefono si collega in Bluetooth,
 * DriveOS avvia da solo la sorgente giusta restando sulla propria Home (non apre AutoLink
 * né cambia schermata, salvo che l'utente lo chieda).
 *
 * Questa è la logica pura (testabile): decide COSA fare in base allo stato. L'esecuzione
 * vera (riprodurre, avviare la navigazione) è nel controller Android.
 */
enum class SmartAction {
    PLAY_ONLINE,        // telefono online → app musica in streaming (es. Spotify)
    PLAY_OFFLINE,       // telefono offline → musica locale
    START_NAV,          // apre l'app di navigazione (Waze o predefinita)
    LAUNCH_ANDROID_AUTO, // opzionale: avvia Android Auto (cambia schermata)
}

data class SmartStartConfig(
    val enabled: Boolean = false,
    val playMusic: Boolean = true,
    val startNav: Boolean = false,
    val launchAndroidAuto: Boolean = false,
    val hasOnlineMusic: Boolean = false,
    val hasOfflineMusic: Boolean = false,
    val hasNav: Boolean = false,
    /** CarPlay/Android Auto (SpeedPlay) già attivo: gestisce lui musica e navigazione. */
    val projectionActive: Boolean = false,
)

object SmartStart {
    /**
     * Piano d'azione alla connessione Bluetooth.
     * @param online true se il telefono (quindi la radio, via tethering) ha rete.
     * Restituisce le azioni in ordine di esecuzione; vuoto se non c'è nulla da fare.
     */
    fun plan(config: SmartStartConfig, online: Boolean): List<SmartAction> {
        if (!config.enabled) return emptyList()
        // Se la proiezione è già attiva non avviamo nulla sopra (eviteremmo due sorgenti audio).
        if (config.projectionActive) return emptyList()
        val actions = ArrayList<SmartAction>()
        if (config.launchAndroidAuto) actions.add(SmartAction.LAUNCH_ANDROID_AUTO)
        if (config.playMusic) {
            when {
                online && config.hasOnlineMusic -> actions.add(SmartAction.PLAY_ONLINE)
                !online && config.hasOfflineMusic -> actions.add(SmartAction.PLAY_OFFLINE)
                config.hasOfflineMusic -> actions.add(SmartAction.PLAY_OFFLINE) // rete incerta: ripiega sul locale
                config.hasOnlineMusic -> actions.add(SmartAction.PLAY_ONLINE)
            }
        }
        if (config.startNav && config.hasNav) actions.add(SmartAction.START_NAV)
        return actions
    }
}

/**
 * Evita di rieseguire il piano a ogni micro-evento Bluetooth: scatta solo sul passaggio
 * da "non connesso" a "connesso", e non più di una volta per connessione.
 */
class ConnectionTrigger {
    private var lastConnected = false

    /** @return true se questa transizione deve far partire l'avvio intelligente. */
    fun onState(connected: Boolean): Boolean {
        val rising = connected && !lastConnected
        lastConnected = connected
        return rising
    }

    fun reset() { lastConnected = false }
}
