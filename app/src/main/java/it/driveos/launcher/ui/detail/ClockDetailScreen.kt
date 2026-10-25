package it.driveos.launcher.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.calendar.CalendarEvent
import it.driveos.launcher.ui.cards.formatDate
import it.driveos.launcher.ui.cards.formatTime
import it.driveos.launcher.ui.cards.rememberMinuteClock
import it.driveos.launcher.ui.cards.shortWhen
import java.time.LocalDate
import java.time.ZoneId

/** Orologio a tutto schermo (utile anche di notte). Un tocco torna alla Home. */
@Composable
fun ClockDetailScreen(nextEvent: CalendarEvent?, onBack: () -> Unit) {
    val now by rememberMinuteClock()
    val zone = remember { ZoneId.systemDefault() }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
    ) {
        val density = LocalDensity.current
        val timeSp = with(density) { minOf(maxHeight.value * 0.45f, maxWidth.value * 0.3f).dp.toSp() }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatTime(now), fontSize = timeSp, lineHeight = timeSp, fontWeight = FontWeight.Light, maxLines = 1, color = MaterialTheme.colorScheme.onBackground)
            Text(formatDate(now), fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (nextEvent != null) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Prossimo: ${shortWhen(nextEvent, zone, LocalDate.now(zone))} · ${nextEvent.title}",
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
