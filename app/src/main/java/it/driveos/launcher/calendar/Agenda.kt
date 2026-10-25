package it.driveos.launcher.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Un'occorrenza di un evento, come letta da CalendarContract.Instances. */
data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String,
    val color: Int?,
)

/**
 * Regole di presentazione dell'agenda. Gli eventi "tutto il giorno" sono salvati
 * da Android in UTC: per non sbagliare giorno vanno letti in UTC.
 */
object AgendaFormatter {
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    fun dayOf(e: CalendarEvent, zone: ZoneId): LocalDate =
        if (e.allDay) Instant.ofEpochMilli(e.begin).atZone(ZoneOffset.UTC).toLocalDate()
        else Instant.ofEpochMilli(e.begin).atZone(zone).toLocalDate()

    /** Esclude gli eventi gia' terminati. */
    fun isRelevant(e: CalendarEvent, now: Instant, zone: ZoneId): Boolean =
        if (e.allDay) Instant.ofEpochMilli(e.end).atZone(ZoneOffset.UTC).toLocalDate() > now.atZone(zone).toLocalDate()
        else e.end > now.toEpochMilli()

    fun timeLabel(e: CalendarEvent, zone: ZoneId): String =
        if (e.allDay) "Tutto il giorno"
        else "${Instant.ofEpochMilli(e.begin).atZone(zone).format(TIME)}–${Instant.ofEpochMilli(e.end).atZone(zone).format(TIME)}"

    fun startLabel(e: CalendarEvent, zone: ZoneId): String =
        if (e.allDay) "Tutto il giorno" else Instant.ofEpochMilli(e.begin).atZone(zone).format(TIME)

    fun dayLabel(day: LocalDate, today: LocalDate, locale: Locale): String = when (day) {
        today -> "Oggi"
        today.plusDays(1) -> "Domani"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    /** Ordine: giorno, poi eventi "tutto il giorno" in testa, poi orario. */
    fun upcoming(events: List<CalendarEvent>, now: Instant, zone: ZoneId): List<CalendarEvent> =
        events.filter { isRelevant(it, now, zone) }
            .sortedWith(compareBy<CalendarEvent>({ dayOf(it, zone) }, { !it.allDay }, { it.begin }, { it.title }))

    fun groupByDay(events: List<CalendarEvent>, now: Instant, zone: ZoneId): List<Pair<LocalDate, List<CalendarEvent>>> =
        upcoming(events, now, zone).groupBy { dayOf(it, zone) }.toList()
}
