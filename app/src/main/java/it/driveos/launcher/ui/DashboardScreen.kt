package it.driveos.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.driveos.launcher.dashboard.CardType
import it.driveos.launcher.dashboard.DashboardLayout
import it.driveos.launcher.ui.cards.DashboardGrid

/** Home: le quattro schede disposte secondo il layout scelto. */
@Composable
fun DashboardScreen(layout: DashboardLayout, card: @Composable (CardType) -> Unit) {
    DashboardGrid(layout, Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) { type -> card(type) }
}
