package it.driveos.launcher.system

/**
 * "Cancello" di accensione: quando lo schermo e' spento (quadro spento, radio verso lo standby) e nei
 * primi secondi dopo l'avvio o il risveglio, DriveOS NON deve toccare la radio Junsun: niente servizi
 * Junsun collegati, niente seriale, niente log. La radio in quei momenti si spegne/riaccende e un
 * collegamento esterno (es. bindService con creazione automatica) la tiene sveglia o la blocca.
 */
object PowerGate {
    @Volatile var screenOn: Boolean = true
    @Volatile var notBefore: Long = System.currentTimeMillis() + 20_000

    /** true = si puo' parlare con la radio. */
    fun ready(): Boolean = screenOn && System.currentTimeMillis() >= notBefore
}
