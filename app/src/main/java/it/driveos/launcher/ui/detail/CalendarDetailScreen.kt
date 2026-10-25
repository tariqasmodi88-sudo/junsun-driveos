package it.driveos.launcher.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.calendar.AgendaFormatter
import it.driveos.launcher.calendar.CalendarEvent
import it.driveos.launcher.calendar.CalendarState
import it.driveos.launcher.ui.ScreenHeader
import it.driveos.launcher.ui.cards.CardButton
import it.driveos.launcher.ui.cards.EventColorBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun CalendarDetailScreen(
    state: CalendarState,
    onConnect: () -> Unit,
    onOpenCalendarApp: () -> Unit,
    onOpenEvent: (CalendarEvent) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val groups = remember(state.events, today) { AgendaFormatter.groupByDay(state.events, Instant.now(), zone) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Agenda", onBack) {
            CardButton("Apri Calendar", Icons.Filled.DateRange, onOpenCalendarApp, Modifier.widthIn(max = 300.dp), height = 56.dp)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            when {
                !state.permission -> {
                    Text("Mostra qui gli impegni del tuo Google Calendar.", fontSize = 22.sp, color = colors.onSurface)
                    Text("DriveOS legge soltanto: non crea né modifica eventi.", fontSize = 17.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    CardButton("Collega calendario", Icons.Filled.DateRange, onConnect, Modifier.widthIn(max = 360.dp).fillMaxWidth(), height = 72.dp, primary = true)
                }

                state.calendars == 0 -> {
                    Text("Nessun calendario sincronizzato su questa radio.", fontSize = 22.sp, color = colors.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "In Impostazioni Android → Account aggiungi il tuo account Google e attiva la sincronizzazione " +
                            "di Calendar. Se l'opzione Calendar non c'è, installa Google Calendar dal Play Store e riprova.",
                        fontSize = 18.sp, color = colors.onSurfaceVariant,
                    )
                }

                groups.isEmpty() -> Text("Nessun impegno nei prossimi 14 giorni.", fontSize = 22.sp, color = colors.onSurfaceVariant)

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { (day, events) ->
                        item(key = "d$day") {
                            Text(
                                AgendaFormatter.dayLabel(day, today, Locale.getDefault()),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                            )
                        }
                        items(events, key = { "e${it.eventId}_${it.begin}" }) { e ->
                            Surface(
                                onClick = { onOpenEvent(e) },
                                shape = RoundedCornerShape(18.dp),
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
                            ) {
                                Row(Modifier.height(76.dp).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    EventColorBar(e, Modifier.width(6.dp).fillMaxHeight().padding(vertical = 12.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        AgendaFormatter.timeLabel(e, zone),
                                        fontSize = 17.sp,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.width(150.dp),
                                        maxLines = 1,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(e.title, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                                        if (e.location.isNotBlank()) {
                                            Text(e.location, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                                        }
                                    }
                                    if (e.location.isNotBlank()) {
                                        CardButton("Naviga", Icons.Filled.Place, { onNavigate(e.location) }, Modifier.padding(horizontal = 10.dp), height = 56.dp, primary = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
