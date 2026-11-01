package it.driveos.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.calendar.CalendarEvent
import it.driveos.launcher.calendar.CalendarState
import it.driveos.launcher.controls.SystemControls
import it.driveos.launcher.dashboard.CardType
import it.driveos.launcher.dashboard.DashboardLayout
import it.driveos.launcher.media.MediaInfo
import it.driveos.launcher.nav.NavGuidance
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioState
import it.driveos.launcher.settings.DriveSettings
import it.driveos.launcher.settings.ThemeMode
import it.driveos.launcher.ui.*
import it.driveos.launcher.ui.cards.*
import it.driveos.launcher.ui.detail.*
import it.driveos.launcher.ui.theme.DriveOSTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Altre schermate dell'app (stesso meccanismo di ScreenshotTest; solo con DRIVEOS_SHOTS=1). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w853dp-h480dp-land-hdpi")
class ScreenshotTest2 {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun on() = assumeTrue(System.getenv("DRIVEOS_SHOTS") == "1")

    private fun shot(name: String, mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            DriveOSTheme(mode, 0, null) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        rule.mainClock.advanceTimeBy(600)
        val view = rule.activity.window.decorView
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1280, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(720, android.view.View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 1280, 720)
        val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val dir = File("build/screenshots").also { it.mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Schermata + barra in basso, come sul dispositivo. */
    @Composable private fun WithBar(current: Screen, body: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { body() }
            BottomBar(current, {}, {}, {}, {}, {}, {}, {})
        }
    }

    private fun icon(): ImageBitmap = ImageBitmap(48, 48)
    private fun app(label: String, pkg: String) = AppEntry(AppKey(pkg, "$pkg.Main"), label, icon(), false)
    private val waze = app("Waze", "com.waze")
    private val spotify = app("Spotify", "com.spotify.music")
    private val apps = listOf(waze, spotify, app("Telefono", "p.tel"), app("Maps", "com.google.maps"),
        app("YouTube", "yt"), app("Radio", "p.radio"), app("Impostazioni", "p.set"), app("File", "p.files"),
        app("Android Auto", "p.aa"), app("Chrome", "p.chrome"), app("Calendario", "p.cal"), app("Foto", "p.foto"))
    private val now = System.currentTimeMillis()
    private val calendar = CalendarState(permission = true, loaded = true, calendars = 2, events = listOf(
        CalendarEvent(1, "Riunione fornitori", now + 3_600_000, now + 7_200_000, false, "Milano", null),
        CalendarEvent(2, "Dentista", now + 90_000_000, now + 93_600_000, false, "Via Roma 12", null),
        CalendarEvent(3, "Cena con Marco", now + 180_000_000, now + 187_200_000, false, "", null),
    ))
    private val media = MediaInfo("com.spotify.music", "Nel blu dipinto di blu", "Domenico Modugno", "Raccolta",
        null, true, 215_000, 80_000, android.os.SystemClock.elapsedRealtime(), 1f)
    private val guidance = NavGuidance("com.waze", "k", "Gira a destra su Via Roma", "tra 300 m", "12 min · 6,4 km", null)
    private val settings = DriveSettings(navApp = waze.key, musicApp = spotify.key, homeAddress = "Via Verdi 5, Torino",
        workAddress = "Corso Francia 100, Torino", recentDestinations = listOf("Aeroporto Caselle", "Stazione Porta Nuova"),
        layout = DashboardLayout.DEFAULT)

    @Composable private fun home() = DashboardScreen(DashboardLayout.DEFAULT) { type ->
        when (type) {
            CardType.CLOCK -> ClockCard(onClick = {})
            CardType.NAV -> NavCard(waze, true, guidance, settings.recentDestinations, {}, {}, {}, {}, {}, {})
            CardType.MUSIC -> MusicCard(true, media, spotify, spotify, {}, {}, {}, {}, {}, {})
            CardType.CALENDAR -> CalendarCard(calendar, {}, {})
        }
    }

    @Test fun home_notte() { on(); shot("10_home_notte") { WithBar(Screen.DASHBOARD) { home() } } }
    @Test fun home_giorno() { on(); shot("11_home_giorno", ThemeMode.LIGHT) { WithBar(Screen.DASHBOARD) { home() } } }
    @Test fun home_spie() { on(); shot("12_home_con_spie") {
        WithBar(Screen.DASHBOARD) { Column { VehicleStatusStrip(listOf(it.driveos.launcher.vehicle.ActiveAlert("Portiera aperta")), {}); Box(Modifier.weight(1f)) { home() } } } } }
    @Test fun controlli() { on(); shot("13_controlli_volume") {
        WithBar(Screen.DASHBOARD) { Column {
            ControlsPanel(SystemControls.VolumeState(10, 15, 67), 0.8f, true, {}, {}, {}, {}, {})
            Box(Modifier.weight(1f)) { home() } } } } }
    @Test fun nav() { on(); shot("14_navigazione") { WithBar(Screen.NAV) {
        NavDetailScreen(waze, guidance, settings.homeAddress, settings.workAddress, settings.recentDestinations,
            "", {}, true, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } } }
    @Test fun musica() { on(); shot("15_musica") { WithBar(Screen.MUSIC) {
        MusicDetailScreen(true, media, spotify, spotify, listOf(spotify), {}, {}, {}, {}, {}, {}, {}) } } }
    @Test fun calendario() { on(); shot("16_calendario") { WithBar(Screen.CALENDAR) {
        CalendarDetailScreen(calendar, {}, {}, {}, {}, {}) } } }
    @Test fun orologio() { on(); shot("17_orologio") { WithBar(Screen.CLOCK) { ClockDetailScreen(calendar.events.first(), {}) } } }
    @Test fun appDrawer() { on(); shot("18_app") { WithBar(Screen.APPS) {
        AppDrawerScreen(apps, true, listOf(waze.key.encode(), spotify.key.encode()), {}, {}, {}, {}) } } }
    @Test fun radio() { on(); shot("19_radio") { WithBar(Screen.RADIO) {
        RadioScreen(RadioState(khz = 97900, playing = true), true,
            listOf(RadioPreset(92000, "VIVA FM"), RadioPreset(97900, "Radio Deejay"), RadioPreset(101500, "RTL 102.5"), RadioPreset(104500, "R101")),
            97900, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } } }
    @Test fun smart() { on(); shot("20_avvio_intelligente") { WithBar(Screen.SETTINGS) {
        SmartStartScreen(settings, { k -> apps.firstOrNull { it.key == k }?.label }, true, true, true,
            {}, {}, {}, {}, {}, {}, {}, {}) } } }
    @Test fun impostazioni() { on(); shot("21_impostazioni") { WithBar(Screen.SETTINGS) {
        SettingsScreen(settings = settings, appLabel = { k -> apps.firstOrNull { it.key == k }?.label }, onPickNav = {}, onEditHome = {}, onEditWork = {},
            onLayout = {}, notificationAccess = true, onNotificationAccess = {}, calendarAccess = true, onCalendarAccess = {}, onPickMusic = {}, onPickPhone = {},
            onPickRadio = {}, onThemeMode = {}, onAccent = {}, autoThemeSource = "fari", onOpenVehicle = {}, onOpenRadio = {}, onOpenSmartStart = {},
            smartStartEnabled = true, autoOverview = true, autoOverviewHasGps = true, onToggleAutoOverview = {}, onOpenOverview = {}, keepHome = true,
            accessibilityOn = true, onAccessibility = {}, overlayGranted = false, onOverlay = {}, onToggleKeepHome = {}, onEditLocation = {},
            onHomeSettings = {}, onAndroidSettings = {}, onDeviceInfo = {}, onBack = {}) } } }
    @Test fun info() { on(); shot("22_info_dispositivo") { WithBar(Screen.SETTINGS) { DeviceInfoScreen(onBack = {}) } } }
}
