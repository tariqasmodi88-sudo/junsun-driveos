package it.driveos.launcher

import it.driveos.launcher.calendar.AgendaFormatter
import it.driveos.launcher.calendar.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

class AgendaFormatterTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val now = ZonedDateTime.of(2026, 10, 25, 13, 0, 0, 0, zone).toInstant()
    private val today = LocalDate.of(2026, 10, 25)

    private fun timed(id: Long, h1: Int, h2: Int, day: Int = 25) = CalendarEvent(
        id, "E$id",
        ZonedDateTime.of(2026, 10, day, h1, 0, 0, 0, zone).toInstant().toEpochMilli(),
        ZonedDateTime.of(2026, 10, day, h2, 0, 0, 0, zone).toInstant().toEpochMilli(),
        false, "", null,
    )

    private fun allDay(id: Long, day: Int) = CalendarEvent(
        id, "A$id",
        LocalDate.of(2026, 10, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        LocalDate.of(2026, 10, day + 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        true, "", null,
    )

    @Test fun pastEventsAreHidden() {
        assertFalse(AgendaFormatter.isRelevant(timed(1, 9, 10), now, zone))
        assertTrue(AgendaFormatter.isRelevant(timed(2, 12, 14), now, zone))
        assertTrue(AgendaFormatter.isRelevant(allDay(3, 25), now, zone))
        assertFalse(AgendaFormatter.isRelevant(allDay(4, 24), now, zone))
    }

    @Test fun allDayEventsUseUtcDay() {
        assertEquals(today, AgendaFormatter.dayOf(allDay(1, 25), zone))
    }

    @Test fun orderingAndGrouping() {
        val events = listOf(timed(1, 15, 16), timed(2, 9, 10, day = 26), allDay(3, 25), timed(4, 14, 15))
        val up = AgendaFormatter.upcoming(events, now, zone)
        assertEquals(listOf(3L, 4L, 1L, 2L), up.map { it.eventId })
        val groups = AgendaFormatter.groupByDay(events, now, zone)
        assertEquals(listOf(today, today.plusDays(1)), groups.map { it.first })
    }

    @Test fun labels() {
        assertEquals("14:00–15:00", AgendaFormatter.timeLabel(timed(1, 14, 15), zone))
        assertEquals("Tutto il giorno", AgendaFormatter.timeLabel(allDay(2, 25), zone))
        assertEquals("Oggi", AgendaFormatter.dayLabel(today, today, Locale.ITALIAN))
        assertEquals("Domani", AgendaFormatter.dayLabel(today.plusDays(1), today, Locale.ITALIAN))
        assertEquals("Martedì 27 ottobre", AgendaFormatter.dayLabel(today.plusDays(2), today, Locale.ITALIAN))
    }
}
