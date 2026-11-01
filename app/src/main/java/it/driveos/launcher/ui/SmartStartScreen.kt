package it.driveos.launcher.ui

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.settings.DriveSettings

/**
 * Avvio intelligente: alla connessione Bluetooth del telefono, DriveOS avvia la sorgente
 * giusta (musica online o locale) restando sulla propria Home. Sostituisce AutoLink.
 */
@Composable
fun SmartStartScreen(
    settings: DriveSettings,
    appLabel: (AppKey) -> String?,
    btConnected: Boolean,
    online: Boolean,
    androidAutoInstalled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleNav: (Boolean) -> Unit,
    onToggleAndroidAuto: (Boolean) -> Unit,
    onPickOnline: () -> Unit,
    onPickOffline: () -> Unit,
    onRunNow: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    fun describe(key: AppKey?) = if (key == null) tr("Non configurata") else appLabel(key) ?: tr("App non trovata")

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tr("Avvio intelligente"), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text("Bluetooth: ${if (btConnected) "telefono collegato" else "nessun telefono"} · " +
                    "Rete: ${if (online) "online" else "offline"}",
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.onSurface, modifier = Modifier.padding(14.dp))
            }

            Text(tr("Quando il telefono si collega in Bluetooth, DriveOS avvia da solo la musica ") +
                tr("(Spotify se c'è rete, altrimenti i brani locali) senza cambiare schermata."),
                fontSize = 14.sp, color = colors.onSurfaceVariant)

            ToggleRow(tr("Attiva avvio intelligente"), tr("Sostituisce AutoLink"), settings.smartStartEnabled, onToggleEnabled)
            ToggleRow(tr("Avvia la musica"), tr("Alla connessione del telefono"), settings.smartStartMusic, onToggleMusic)
            SettingRowLite(tr("Musica online (streaming)"), describe(settings.onlineMusicApp), onPickOnline)
            SettingRowLite(tr("Musica offline (locale)"), describe(settings.offlineMusicApp), onPickOffline)
            ToggleRow(tr("Avvia la navigazione"), tr("Apre l'app di navigazione scelta"), settings.smartStartNav, onToggleNav)
            ToggleRow(tr("Collega CarPlay/Android Auto"),
                if (androidAutoInstalled) tr("Si collega in secondo piano: resti sulla Home DriveOS") else tr("Android Auto non risulta installato"),
                settings.smartStartAndroidAuto, onToggleAndroidAuto)

            Spacer(Modifier.height(8.dp))
            Surface(onClick = onRunNow, shape = RoundedCornerShape(16.dp), color = colors.primary, contentColor = colors.onPrimary,
                modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(tr("Avvia ora (prova)"), fontSize = 17.sp, fontWeight = FontWeight.Medium) }
            }
            Text(tr("Nota: l'avvio della riproduzione è «best-effort» — alcune app partono solo se ") +
                tr("sono già state aperte almeno una volta dopo l'accensione."),
                fontSize = 13.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
                Text(subtitle, fontSize = 14.sp, color = colors.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SettingRowLite(title: String, value: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Text(value, fontSize = 14.sp, color = colors.onSurfaceVariant)
        }
    }
}
