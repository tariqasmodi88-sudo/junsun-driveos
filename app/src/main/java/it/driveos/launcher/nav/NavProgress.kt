package it.driveos.launcher.nav

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Indicazioni svolta-per-svolta calcolate da DriveOS sul percorso OSRM, a partire dalla posizione GPS.
 * Servono perche' Waze nella sua notifica non pubblica le manovre ("In funzione"): cosi' la Home
 * continua a mostrare la prossima svolta anche quando Waze e' in secondo piano.
 * Solo calcoli, niente Android: testabile.
 */
object NavProgress {

    enum class Turn { STRAIGHT, SLIGHT_RIGHT, RIGHT, SHARP_RIGHT, SLIGHT_LEFT, LEFT, SHARP_LEFT, UTURN, ROUNDABOUT, ARRIVE }

    /** Manovra del percorso; [pointIndex] = indice del punto della linea in cui avviene. */
    data class Step(
        val pointIndex: Int,
        val type: String,
        val modifier: String,
        val name: String,
        val exit: Int? = null,
    ) {
        val turn: Turn get() = turnOf(type, modifier)
        val instruction: String get() = instruction(type, modifier, exit)
    }

    data class State(
        val turn: Turn,
        val instruction: String,
        val street: String,
        /** Metri alla prossima manovra. */
        val distanceM: Double,
        /** Manovra successiva se arriva subito dopo (meno di 200 m), altrimenti null. */
        val then: Turn?,
        val remainingM: Double,
        val remainingS: Double,
        /** Indice del punto del percorso piu' vicino all'auto: la parte prima e' gia' percorsa. */
        val traveledIndex: Int,
    )

    /** Le manovre "prosegui dritto" senza svolta non si annunciano (come fanno Maps e Waze). */
    fun isAnnounced(type: String, modifier: String): Boolean = when (type) {
        "depart", "notification" -> false
        "new name", "continue" -> modifier != "straight" && modifier.isNotBlank()
        else -> true
    }

    fun turnOf(type: String, modifier: String): Turn = when (type) {
        "arrive" -> Turn.ARRIVE
        "roundabout", "rotary", "roundabout turn", "exit roundabout", "exit rotary" -> Turn.ROUNDABOUT
        else -> when (modifier) {
            "uturn" -> Turn.UTURN
            "sharp right" -> Turn.SHARP_RIGHT
            "right" -> Turn.RIGHT
            "slight right" -> Turn.SLIGHT_RIGHT
            "sharp left" -> Turn.SHARP_LEFT
            "left" -> Turn.LEFT
            "slight left" -> Turn.SLIGHT_LEFT
            else -> Turn.STRAIGHT
        }
    }

    fun instruction(type: String, modifier: String, exit: Int?): String {
        val side = when {
            modifier.contains("right") -> "destra"
            modifier.contains("left") -> "sinistra"
            else -> null
        }
        return when (type) {
            "arrive" -> "Arrivo a destinazione"
            "roundabout", "rotary", "roundabout turn" ->
                if (exit != null && exit > 0) "Alla rotonda prendi la ${exit}ª uscita" else "Entra nella rotonda"
            "exit roundabout", "exit rotary" -> "Esci dalla rotonda"
            "on ramp" -> "Prendi la rampa" + (side?.let { " a $it" } ?: "")
            "off ramp" -> "Prendi l'uscita" + (side?.let { " a $it" } ?: "")
            "fork" -> side?.let { "Al bivio tieni la $it" } ?: "Al bivio prosegui dritto"
            "merge" -> "Immettiti" + (side?.let { " a $it" } ?: "")
            "end of road" -> side?.let { "In fondo alla strada gira a $it" } ?: "In fondo alla strada prosegui"
            else -> when (modifier) {
                "uturn" -> "Fai inversione"
                "sharp right" -> "Svolta stretta a destra"
                "right" -> "Gira a destra"
                "slight right" -> "Tieni la destra"
                "sharp left" -> "Svolta stretta a sinistra"
                "left" -> "Gira a sinistra"
                "slight left" -> "Tieni la sinistra"
                else -> "Prosegui dritto"
            }
        }
    }

    /** Distanze progressive lungo la linea (metri dal primo punto). */
    fun cumulative(points: List<Pair<Double, Double>>): DoubleArray {
        val out = DoubleArray(points.size)
        for (i in 1 until points.size) out[i] = out[i - 1] + NavRoute.haversine(points[i - 1], points[i])
        return out
    }

    fun nearestIndex(points: List<Pair<Double, Double>>, p: Pair<Double, Double>, from: Int = 0): Int {
        var best = from.coerceIn(0, (points.size - 1).coerceAtLeast(0))
        var bestD = Double.MAX_VALUE
        for (i in best until points.size) {
            val d = NavRoute.haversine(p, points[i])
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    fun compute(route: NavRoute.Route, here: Pair<Double, Double>): State? {
        val pts = route.points
        if (pts.size < 2) return null
        val cum = route.cumulative
        val total = cum.last()
        val i = nearestIndex(pts, here)
        val remaining = (total - cum[i] + NavRoute.haversine(here, pts[i]).coerceAtMost(30.0)).coerceAtLeast(0.0)
        val remainingS = if (total > 0) route.durationS * remaining / total else 0.0
        val idx = route.steps.indexOfFirst { it.pointIndex > i }
        val next = if (idx >= 0) route.steps[idx] else null
        if (next == null) {
            return State(Turn.ARRIVE, "Arrivo a destinazione", route.destination, remaining, null, remaining, remainingS, i)
        }
        val dist = (cum[next.pointIndex] - cum[i]).coerceAtLeast(0.0)
        val after = route.steps.getOrNull(idx + 1)
        val then = after?.takeIf { cum[it.pointIndex] - cum[next.pointIndex] < 200 }?.turn
        val street = if (next.type == "arrive") route.destination else next.name
        return State(next.turn, next.instruction, street, dist, then, remaining, remainingS, i)
    }

    fun formatDistance(m: Double): String = when {
        m < 15 -> "Ora"
        m < 100 -> "${(m / 10).roundToInt() * 10} m"
        m < 1000 -> "${((m / 50).roundToInt() * 50).coerceAtMost(950)} m"
        m < 10_000 -> String.format(Locale.ITALY, "%.1f km", m / 1000)
        else -> "${(m / 1000).roundToInt()} km"
    }

    fun formatDuration(s: Double): String {
        val min = (s / 60).roundToInt().coerceAtLeast(1)
        return if (min < 60) "$min min" else "${min / 60} h ${"%02d".format(min % 60)}"
    }
}
