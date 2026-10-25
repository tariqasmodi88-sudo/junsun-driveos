package it.driveos.launcher.dashboard

/** Le quattro schede della Home. */
enum class CardType { CLOCK, NAV, MUSIC, CALENDAR }

/** Posizione di una scheda su una griglia di [GRID_COLUMNS] x [GRID_ROWS] celle. */
data class Slot(val card: CardType, val x: Int, val y: Int, val w: Int, val h: Int)

const val GRID_COLUMNS = 12
const val GRID_ROWS = 6

/**
 * Disposizioni selezionabili. Ogni layout e' solo un elenco di slot: la stessa
 * definizione disegna sia la Home sia l'anteprima nelle impostazioni.
 * Il lato sinistro e' quello del guidatore (guida a sinistra).
 */
enum class DashboardLayout(val id: String, val title: String, val description: String, val slots: List<Slot>) {
    NAV_FOCUS(
        "nav_focus", "Navigazione in primo piano", "Navigazione grande vicino al guidatore",
        listOf(
            Slot(CardType.NAV, 0, 0, 6, 6),
            Slot(CardType.MUSIC, 6, 0, 6, 3),
            Slot(CardType.CLOCK, 6, 3, 3, 3),
            Slot(CardType.CALENDAR, 9, 3, 3, 3),
        )
    ),
    GRID(
        "grid", "Griglia", "Quattro schede della stessa dimensione",
        listOf(
            Slot(CardType.NAV, 0, 0, 6, 3),
            Slot(CardType.CLOCK, 6, 0, 6, 3),
            Slot(CardType.MUSIC, 0, 3, 6, 3),
            Slot(CardType.CALENDAR, 6, 3, 6, 3),
        )
    ),
    MEDIA_FOCUS(
        "media_focus", "Musica in primo piano", "Copertina e comandi grandi",
        listOf(
            Slot(CardType.MUSIC, 0, 0, 6, 6),
            Slot(CardType.NAV, 6, 0, 6, 3),
            Slot(CardType.CLOCK, 6, 3, 3, 3),
            Slot(CardType.CALENDAR, 9, 3, 3, 3),
        )
    ),
    COCKPIT(
        "cockpit", "Cruscotto", "Ora e agenda in colonna, navigazione e musica affiancate",
        listOf(
            Slot(CardType.CLOCK, 0, 0, 3, 2),
            Slot(CardType.CALENDAR, 0, 2, 3, 4),
            Slot(CardType.NAV, 3, 0, 5, 6),
            Slot(CardType.MUSIC, 8, 0, 4, 6),
        )
    ),
    COLUMNS(
        "columns", "Colonne", "Quattro colonne verticali",
        listOf(
            Slot(CardType.NAV, 0, 0, 3, 6),
            Slot(CardType.MUSIC, 3, 0, 3, 6),
            Slot(CardType.CLOCK, 6, 0, 3, 6),
            Slot(CardType.CALENDAR, 9, 0, 3, 6),
        )
    ),
    MINIMAL(
        "minimal", "Essenziale", "Orologio grande e una riga di schede compatte",
        listOf(
            Slot(CardType.CLOCK, 0, 0, 12, 4),
            Slot(CardType.NAV, 0, 4, 4, 2),
            Slot(CardType.MUSIC, 4, 4, 4, 2),
            Slot(CardType.CALENDAR, 8, 4, 4, 2),
        )
    );

    companion object {
        val DEFAULT = NAV_FOCUS
        fun fromId(id: String?): DashboardLayout = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
