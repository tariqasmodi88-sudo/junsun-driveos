package it.driveos.launcher.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.dashboard.CardType
import it.driveos.launcher.dashboard.DashboardLayout
import it.driveos.launcher.dashboard.GRID_COLUMNS
import it.driveos.launcher.dashboard.GRID_ROWS
import kotlin.math.roundToInt

/** Classe di dimensione di una scheda: decide quanta informazione mostrare. */
enum class CardSize { COMPACT, REGULAR, LARGE }

fun cardSizeOf(innerWidth: Dp, innerHeight: Dp): CardSize = when {
    innerHeight < 140.dp || innerWidth < 160.dp -> CardSize.COMPACT
    innerHeight >= 270.dp && innerWidth >= 350.dp -> CardSize.LARGE
    else -> CardSize.REGULAR
}

/**
 * Contenitore comune delle schede: angoli ampi, bordo sottile, tocco su tutta la superficie
 * (apre la vista a tutto schermo). [title] null = nessuna intestazione.
 */
@Composable
fun DashCard(
    title: String?,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.(size: CardSize, wide: Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
            val size = cardSizeOf(maxWidth, maxHeight)
            val wide = maxWidth > maxHeight * 1.3f
            Column(Modifier.fillMaxSize()) {
                if (title != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            title.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                        if (trailing != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "· $trailing",
                                fontSize = 13.sp,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(if (size == CardSize.COMPACT) 6.dp else 10.dp))
                }
                content(size, wide)
            }
        }
    }
}

/** Pulsante dentro le schede: grande, con icona ed etichetta corta. */
@Composable
fun CardButton(
    label: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    primary: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(18.dp),
        color = if (primary) colors.primary else colors.surfaceVariant,
        contentColor = if (primary) colors.onPrimary else colors.onSurface,
    ) {
        Row(
            Modifier.padding(horizontal = if (label == null) 0.dp else 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(if (height >= 64.dp) 28.dp else 24.dp))
            if (label != null) {
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = if (height >= 64.dp) 19.sp else 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Dispone le schede secondo gli slot del layout, su una griglia proporzionale. */
@Composable
fun DashboardGrid(
    layout: DashboardLayout,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    card: @Composable (CardType) -> Unit,
) {
    val slots = layout.slots
    Layout(
        modifier = modifier,
        content = { slots.forEach { slot -> Box { card(slot.card) } } },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gapPx = gap.roundToPx()
        val colW = (width - gapPx * (GRID_COLUMNS - 1)).toFloat() / GRID_COLUMNS
        val rowH = (height - gapPx * (GRID_ROWS - 1)).toFloat() / GRID_ROWS
        val placed = measurables.zip(slots).map { (m, s) ->
            val w = (colW * s.w + gapPx * (s.w - 1)).roundToInt().coerceAtLeast(0)
            val h = (rowH * s.h + gapPx * (s.h - 1)).roundToInt().coerceAtLeast(0)
            m.measure(Constraints.fixed(w, h)) to s
        }
        layout(width, height) {
            placed.forEach { (p, s) ->
                p.place((s.x * (colW + gapPx)).roundToInt(), (s.y * (rowH + gapPx)).roundToInt())
            }
        }
    }
}

/** Miniatura del layout per le impostazioni (usa la stessa definizione della Home). */
@Composable
fun LayoutPreview(layout: DashboardLayout, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    DashboardGrid(layout, modifier, gap = 4.dp) { type ->
        Surface(
            Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            color = if (type == CardType.NAV) colors.primary.copy(alpha = 0.35f) else colors.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (type) {
                    CardType.CLOCK -> Text("12:00", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    CardType.NAV -> Icon(Icons.Filled.Place, null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                    CardType.MUSIC -> Icon(Icons.Filled.PlayArrow, null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                    CardType.CALENDAR -> Icon(Icons.Filled.DateRange, null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
