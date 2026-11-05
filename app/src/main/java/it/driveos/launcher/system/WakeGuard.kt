package it.driveos.launcher.system

/**
 * Ritorno alla Home dopo l'accensione. Quando giri la chiave la radio spesso non si riavvia ma esce
 * dalla sospensione: non arriva l'avvio di sistema e il firmware rimette davanti la sua app
 * (com.ts.MainUI), a volte con qualche secondo di ritardo. Per un breve periodo dopo l'accensione
 * DriveOS si riporta davanti se compare un'app Junsun.
 *
 * Si ferma da solo: dopo [WINDOW_MS], dopo [MAX_BOUNCES] ritorni, oppure appena tocchi DriveOS
 * (da quel momento un'app Junsun aperta è una tua scelta). Logica pura, testabile.
 */
object WakeGuard {
    const val WINDOW_MS = 30_000L
    const val MAX_BOUNCES = 4

    @Volatile private var armedAt = 0L
    @Volatile private var bounces = 0
    @Volatile private var userActed = false

    /** La Home DriveOS è visibile (aggiornato da MainActivity). */
    @Volatile var homeInFront: Boolean = false

    /** Impostazione "Tieni la Home davanti" (aggiornata dall'app). */
    @Volatile var keepHome: Boolean = true

    /** Accensione, risveglio o riavvio del processo: si riparte da zero. */
    fun arm(now: Long) { armedAt = now; bounces = 0; userActed = false }

    /** Hai toccato DriveOS: le app aperte da ora in poi sono volute. */
    fun userActed() { userActed = true }

    fun active(now: Long): Boolean =
        keepHome && armedAt > 0 && !userActed && bounces < MAX_BOUNCES && now - armedAt in 0..WINDOW_MS

    /**
     * true = la finestra appena comparsa va coperta con la Home.
     * Solo app Junsun "normali": mai retromarcia/radar (li gestisce già DriveOS), mai le pagine
     * di impostazioni, mai la proiezione del telefono.
     */
    fun shouldBounce(pkg: String?, cls: String?, now: Long, homeInFront: Boolean, reverse: Boolean): Boolean {
        if (!active(now) || homeInFront || reverse) return false
        val p = pkg.orEmpty()
        if (!p.startsWith("com.ts.")) return false
        val kind = it.driveos.launcher.vehicle.WindowRules.classify(p, cls)
        if (kind != it.driveos.launcher.vehicle.WindowKind.OTHER) return false
        if (it.driveos.launcher.vehicle.WindowRules.isSettingsPage(cls)) return false
        return true
    }

    fun bounced() { bounces++ }
}
