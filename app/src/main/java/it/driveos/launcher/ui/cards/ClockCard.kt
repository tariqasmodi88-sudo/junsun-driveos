package it.driveos.launcher.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ora corrente, aggiornata allo scoccare di ogni minuto (nessun polling continuo). */
@Composable
fun rememberMinuteClock(): State<LocalDateTime> = produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(((60 - value.second) * 1000L).coerceAtLeast(500L))
    }
}

fun formatTime(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatDate(t: LocalDateTime, locale: Locale = Locale.getDefault()): String =
    t.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

@Composable
fun ClockCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val now by rememberMinuteClock()
    val time = remember(now) { formatTime(now) }
    val date = remember(now) { formatDate(now) }
    DashCard(title = null, icon = null, onClick = onClick, modifier = modifier) { _, wide ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Il testo si adatta allo spazio della scheda: grande ma mai tagliato.
            val timeDp = minOf(maxHeight.value * 0.5f, maxWidth.value * 0.27f).coerceAtLeast(28f)
            val dateDp = (timeDp * 0.24f).coerceIn(15f, 30f)
            val density = LocalDensity.current
            val timeSp = with(density) { timeDp.dp.toSp() }
            val dateSp = with(density) { dateDp.dp.toSp() }
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (wide) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Text(
                    time,
                    fontSize = timeSp,
                    lineHeight = timeSp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    date,
                    fontSize = dateSp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
