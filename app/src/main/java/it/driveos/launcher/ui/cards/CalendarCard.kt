package it.driveos.launcher.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.calendar.AgendaFormatter
import it.driveos.launcher.calendar.CalendarEvent
import it.driveos.launcher.calendar.CalendarState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Etichetta breve "Oggi 15:00" / "Domani 9:30" / "Gio 10:00". */
fun shortWhen(e: CalendarEvent, zone: ZoneId, today: LocalDate, locale: Locale = Locale.getDefault()): String {
    val day = AgendaFormatter.dayOf(e, zone)
    val dayText = when (day) {
        today -> "Oggi"
        today.plusDays(1) -> "Domani"
        else -> day.format(java.time.format.DateTimeFormatter.ofPattern("EEE d", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    return if (e.allDay) dayText else "$dayText ${AgendaFormatter.startLabel(e, zone)}"
}

@Composable
fun EventColorBar(e: CalendarEvent, modifier: Modifier = Modifier) {
    val c = e.color?.let { Color(it).copy(alpha = 1f) } ?: MaterialTheme.colorScheme.primary
    androidx.compose.foundation.layout.Box(modifier.clip(RoundedCornerShape(3.dp)).background(c))
}

@Composable
fun CalendarCard(
    state: CalendarState,
    onOpenDetail: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val now = Instant.now()
    val today = LocalDate.now(zone)
    val upcoming = remember(state.events, today) { AgendaFormatter.upcoming(state.events, now, zone) }
    val todayCount = upcoming.count { AgendaFormatter.dayOf(it, zone) == today }

    DashCard(
        "Agenda", Icons.Filled.DateRange, onOpenDetail, modifier,
        trailing = if (state.permission && state.calendars > 0) "oggi $todayCount" else null,
    ) { size, _ ->
        when {
            !state.permission -> Info(size, "Mostra qui i tuoi impegni di Google Calendar.", "Collega", onConnect)
            !state.loaded -> Text("Caricamento…", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.calendars == 0 -> Info(size, "Nessun calendario sincronizzato su questa radio.", null, onOpenDetail)
            upcoming.isEmpty() -> Info(size, "Nessun impegno nei prossimi giorni.", null, onOpenDetail)
            else -> EventList(upcoming, size, zone, today)
        }
    }
}

@Composable
private fun ColumnScope.Info(size: CardSize, text: String, button: String?, onClick: () -> Unit) {
    Text(
        text,
        fontSize = if (size == CardSize.COMPACT) 15.sp else 19.sp,
        maxLines = if (size == CardSize.COMPACT) 1 else 4,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (button != null) {
        Spacer(Modifier.weight(1f))
        CardButton(button, Icons.Filled.DateRange, onClick, Modifier.fillMaxWidth(), height = if (size == CardSize.COMPACT) 48.dp else 64.dp, primary = true)
    }
}

@Composable
private fun EventList(events: List<CalendarEvent>, size: CardSize, zone: ZoneId, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rowHeight = if (size == CardSize.COMPACT) 50.dp else 60.dp
        val rows = (maxHeight / rowHeight).toInt().coerceAtLeast(1)
        Column {
            events.take(rows).forEach { e ->
                Row(Modifier.fillMaxWidth().height(rowHeight).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    EventColorBar(e, Modifier.width(5.dp).fillMaxHeight())
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(shortWhen(e, zone, today), fontSize = 14.sp, color = colors.primary, maxLines = 1, fontWeight = FontWeight.SemiBold)
                        Text(e.title, fontSize = if (size == CardSize.COMPACT) 15.sp else 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                    }
                }
            }
        }
    }
}
