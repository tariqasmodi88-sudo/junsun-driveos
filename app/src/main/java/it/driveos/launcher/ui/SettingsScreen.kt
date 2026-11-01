package it.driveos.launcher.ui

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.BuildConfig
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.dashboard.DashboardLayout
import it.driveos.launcher.ui.cards.LayoutPreview
import it.driveos.launcher.settings.DriveSettings
import it.driveos.launcher.settings.ThemeMode
import it.driveos.launcher.ui.theme.AccentColors
import it.driveos.launcher.ui.theme.AccentNames

@Composable
fun SettingsScreen(
    settings: DriveSettings,
    appLabel: (AppKey) -> String?,
    onPickNav: () -> Unit,
    onEditHome: () -> Unit,
    onEditWork: () -> Unit,
    onLayout: (DashboardLayout) -> Unit,
    notificationAccess: Boolean,
    onNotificationAccess: () -> Unit,
    calendarAccess: Boolean,
    onCalendarAccess: () -> Unit,
    onPickMusic: () -> Unit,
    onPickPhone: () -> Unit,
    onPickRadio: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAccent: (Int) -> Unit,
    autoThemeSource: String?,
    onOpenVehicle: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenSmartStart: () -> Unit,
    smartStartEnabled: Boolean,
    autoOverview: Boolean,
    autoOverviewHasGps: Boolean,
    onToggleAutoOverview: () -> Unit,
    onOpenOverview: () -> Unit,
    keepHome: Boolean,
    accessibilityOn: Boolean,
    onAccessibility: () -> Unit,
    overlayGranted: Boolean,
    onOverlay: () -> Unit,
    onToggleKeepHome: () -> Unit,
    onEditLocation: () -> Unit,
    onHomeSettings: () -> Unit,
    onAndroidSettings: () -> Unit,
    onDeviceInfo: () -> Unit,
    onBack: () -> Unit,
    voiceLanguage: String = tr("Italiano"),
    voiceModel: String? = null,
    onVoiceLanguage: () -> Unit = {},
    onVoiceUpdate: () -> Unit = {},
) {
    fun describe(key: AppKey?): String =
        if (key == null) tr("Non configurata") else appLabel(key) ?: trf("App non trovata ({0})", key.packageName)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tr("Impostazioni"), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle(tr("Schermata Home"))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DashboardLayout.entries.forEach { layout ->
                    LayoutOption(layout, selected = settings.layout == layout, onClick = { onLayout(layout) })
                }
            }

            SectionTitle(tr("Veicolo e radio"))
            SettingRow(tr("Schermata veicolo"), tr("Auto vista dall'alto: portiere, fari, retro, sensori, spie"), onOpenOverview)
            SettingRow(tr("Schermata veicolo automatica"),
                when {
                    !autoOverview -> tr("Spenta: si apre solo a mano. Tocca per attivare")
                    autoOverviewHasGps -> tr("Attiva: si apre quando l'auto è ferma o apri una portiera")
                    else -> tr("Attiva solo con le portiere. Tocca e consenti la posizione per usarla da fermo")
                }, onToggleAutoOverview)
            SettingRow(tr("Accessibilità DriveOS"),
                if (accessibilityOn) tr("Attiva: DriveOS vede le finestre Junsun e riporta la Home davanti")
                else tr("Spenta: tocca, scegli DriveOS e attivala (serve per portiere, retro e CarPlay)"), onAccessibility)
            SettingRow(tr("Mostra sopra altre app"),
                if (overlayGranted) tr("Consentita: DriveOS può tornare davanti da solo")
                else tr("Non consentita: tocca e attivala per DriveOS (serve a riportare la Home davanti)"), onOverlay)
            SettingRow(tr("Resta sulla Home DriveOS"),
                if (keepHome) tr("Sì: portiere, radar, retromarcia e CarPlay non coprono lo schermo")
                else tr("No: le finestre Junsun restano sopra. Tocca per attivare"), onToggleKeepHome)
            SettingRow(tr("Veicolo"), tr("Seriale auto, sensori anteriori, limite tocchi, tasti volante"), onOpenVehicle)
            SettingRow(tr("Radio integrata"), tr("Sintonizzatore e canali memorizzati") + (settings.radioPresets.count { it.khz > 0 }.let { if (it > 0) " ($it)" else "" }), onOpenRadio)
            SettingRow(tr("Avvio intelligente (Bluetooth)"),
                if (smartStartEnabled) tr("Attivo: musica/nav all'arrivo del telefono") else tr("Spento: avvia musica e nav quando colleghi il telefono"), onOpenSmartStart)
            SettingRow(tr("App radio (ripiego esterno)"), describe(settings.radioApp), onPickRadio)

            SectionTitle(tr("App"))
            SettingRow(tr("Navigazione"), describe(settings.navApp), onPickNav)
            SettingRow(tr("Indirizzo di casa"), settings.homeAddress.ifBlank { tr("Non impostato") }, onEditHome)
            SettingRow(tr("Indirizzo di lavoro"), settings.workAddress.ifBlank { tr("Non impostato") }, onEditWork)
            SettingRow(tr("Musica"), describe(settings.musicApp), onPickMusic)
            SettingRow(tr("Telefono"), describe(settings.phoneApp), onPickPhone)
            SettingRow(tr("Lingua comandi vocali"), voiceLanguage, onVoiceLanguage)
            SettingRow(tr("Aggiorna voce"), voiceModel?.let { trf("Installata: {0} · tocca per cercare una versione nuova", it) } ?: tr("Non ancora scaricata"), onVoiceUpdate)

            SectionTitle(tr("Aspetto"))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Chip(
                        label = when (mode) {
                            ThemeMode.DARK -> tr("Scuro")
                            ThemeMode.LIGHT -> tr("Chiaro")
                            ThemeMode.SYSTEM -> tr("Come il sistema")
                            ThemeMode.AUTO -> tr("Automatico")
                        },
                        selected = settings.themeMode == mode,
                        onClick = { onThemeMode(mode) },
                    )
                }
            }
            if (settings.themeMode == ThemeMode.AUTO) {
                Text(
                    tr("Automatico: scuro di notte, chiaro di giorno. ") +
                        (autoThemeSource?.let { trf("Ora decide da: {0}. ", it) } ?: "") +
                        if (settings.lightsBinding == null) tr("Collega i fari in Veicolo per usarli.") else "",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingRow(tr("Posizione (alba/tramonto)"),
                    if (settings.latitude != 0.0 || settings.longitude != 0.0)
                        "%.3f, %.3f".format(settings.latitude, settings.longitude)
                    else tr("Predefinita (Italia centrale)"), onEditLocation)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentColors.forEachIndexed { index, color ->
                    val selected = settings.accentIndex == index
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) Modifier.border(4.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                else Modifier
                            )
                            .clickable { onAccent(index) }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    AccentNames.getOrElse(settings.accentIndex) { "" },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle(tr("Permessi"))
            SettingRow(
                tr("Accesso alle notifiche"),
                if (notificationAccess) tr("Attivo: brano in riproduzione e indicazioni del navigatore")
                else tr("Non attivo: tocca per abilitare DriveOS (serve per Musica e guida)"),
                onNotificationAccess,
            )
            SettingRow(
                tr("Calendario"),
                if (calendarAccess) tr("Collegato (sola lettura)") else tr("Non collegato: tocca per mostrare i tuoi impegni"),
                onCalendarAccess,
            )

            SectionTitle(tr("Sistema"))
            SettingRow(tr("Launcher predefinito"), tr("Scegli quale schermata Home usare (DriveOS o quella Junsun)"), onHomeSettings)
            SettingRow(tr("Impostazioni Android"), tr("Apre le impostazioni di sistema"), onAndroidSettings)
            SettingRow(tr("Info dispositivo"), tr("Riepilogo hardware ed esportazione dei dati per lo sviluppo"), onDeviceInfo)

            SectionTitle(tr("Informazioni"))
            Text(
                trf("DriveOS {0}. Radio, Bluetooth, CANBUS e retrocamera restano ", BuildConfig.VERSION_NAME) +
                    tr("gestiti dalle app Junsun originali: DriveOS non le modifica."),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primary else colors.surfaceVariant,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
    ) {
        Text(label, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
    }
}

@Composable
private fun LayoutOption(layout: DashboardLayout, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.primary.copy(alpha = 0.16f) else colors.surface,
        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, colors.primary) else null,
        modifier = Modifier.width(236.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            LayoutPreview(layout, Modifier.width(208.dp).height(117.dp))
            Spacer(Modifier.height(10.dp))
            Text(layout.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(layout.description, fontSize = 14.sp, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
