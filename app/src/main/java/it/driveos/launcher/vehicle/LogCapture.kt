package it.driveos.launcher.vehicle

/**
 * Cattura MIRATA del registro di sistema per capire cosa fa il firmware (portiere, radar,
 * retromarcia, CarPlay/Android Auto, vivavoce, radio). Non salva tutto: solo righe i cui
 * tag/parole riguardano il veicolo e la proiezione, senza ripetizioni, con un tetto al
 * secondo, e con le sequenze lunghe di cifre (numeri di telefono) oscurate.
 * Logica pura, testabile.
 */
class LogCapture(private val nowMs: () -> Long = System::currentTimeMillis) {

    data class Entry(val id: String, val message: String)

    private val interestingTag = Regex(
        "CanDoor|CanFunc|CanRadar|Backcar|RearView|SpeedPlay|zlink|Mirror|CarPlay|CarLife|AndroidAuto|" +
            "Hfp|Bluetooth|BtMusic|Radio|Tuner|ForFan|MainUI|Can[A-Z]\\w*|Txz|TXZ",
        RegexOption.IGNORE_CASE,
    )
    private val interestingText = Regex(
        "radar|reverse|backcar|door|speedplay|suding|zlink|carplay|android ?auto|projection|" +
            "hfp|connected|disconnect|want2start|navi|ACC_OFF|ACC_ON|start u0.*(com\\.ts|suding|txznet)",
        RegexOption.IGNORE_CASE,
    )
    // rumore del sistema che non serve: negazioni SELinux, ioctl del touch, permessi dei pacchetti
    private val noise = Regex("avc: *denied|Un-granting permission|audit\\(|forfan_touch_ctrl")
    private val burstTrigger = Regex("CanDoorMsgbox|GotoWin = 128|CanRadarActivity|CanCameraActivity")
    private val burstSkipTag = Regex("Finsky|Nearby|wifi|Wifi|wpa_|Bluetooth|Gnss|agps|hal2mnl|NetworkController|Surface|BufferQueue|DuraSpeed|PowerHal|WM-|Choreographer|ViewRootImpl|InputMethod|AudioFlinger|System.err|Mirror|aguw|DomainFilter|RecoverableKey|ProximityAuth|MagicTether|GmsCore", RegexOption.IGNORE_CASE)
    @Volatile private var burstUntil = 0L
    private val line = Regex("^([VDIWEF])/([^(]+)\\(\\s*\\d+\\):\\s?(.*)$")
    private val longDigits = Regex("\\d{7,}")

    private var lastKey = ""
    private var windowStart = 0L
    private var inWindow = 0

    /** @return la voce da registrare, o null se la riga non serve / è un doppione / supera il tetto. */
    fun accept(raw: String): Entry? {
        val m = line.find(raw.trim()) ?: return null
        val tag = m.groupValues[2].trim()
        val msg = m.groupValues[3]
        if (noise.containsMatchIn(msg)) return null
        val now = nowMs()
        // Un evento porta/radar apre una "finestra" di 3 s in cui si registra QUALSIASI riga utile:
        // il dettaglio (quale porta, baule, sensori) potrebbe stare in un tag che non conosciamo.
        if (burstTrigger.containsMatchIn(tag) || burstTrigger.containsMatchIn(msg)) burstUntil = now + 3000
        val inBurst = now < burstUntil && !burstSkipTag.containsMatchIn(tag)
        if (!inBurst && !interestingTag.containsMatchIn(tag) && !interestingText.containsMatchIn(msg)) return null
        val clean = longDigits.replace(msg, "#").take(200)
        val key = "$tag|$clean"
        if (key == lastKey) return null
        lastKey = key
        val t = now
        if (t - windowStart > 1000) { windowStart = t; inWindow = 0 }
        if (++inWindow > (if (inBurst) 40 else 6)) return null
        return Entry("logx:" + tag.take(40), clean)
    }
}
