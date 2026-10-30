package it.driveos.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import it.driveos.launcher.settings.ThemeMode
import it.driveos.launcher.ui.KeysScreen
import it.driveos.launcher.ui.VehicleOverviewScreen
import it.driveos.launcher.ui.VehicleOverviewState
import it.driveos.launcher.ui.VehicleScreen
import it.driveos.launcher.ui.theme.DriveOSTheme
import it.driveos.launcher.vehicle.ActiveAlert
import it.driveos.launcher.vehicle.KeyAction
import it.driveos.launcher.vehicle.KeyBinding
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Genera le immagini delle schermate (schermo della radio: 1280x720, 240 dpi).
 * Parte solo con DRIVEOS_SHOTS=1 (workflow "Anteprime schermate"); nel build normale e' saltato.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w853dp-h480dp-land-hdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, mode: ThemeMode, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            DriveOSTheme(mode, 0, null) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        rule.mainClock.advanceTimeBy(600)
        // Si disegna la vista radice a mano: captureToImage attenderebbe l'animazione infinita
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

    private val base = VehicleOverviewState(
        lightsOn = false, reverse = false, rearview = null, doorOpen = false, parking = null, phone = true,
        projection = false, radioKhz = 97900, speedKmh = 0.0, alerts = emptyList(), logsGranted = true,
        openDoors = emptyList(), handbrake = false, batteryV = "14.1",
    )

    @Test fun overviewNightReverse() {
        assumeTrue(System.getenv("DRIVEOS_SHOTS") == "1")
        shot("1_veicolo_notte_retro", ThemeMode.DARK) {
            VehicleOverviewScreen(base.copy(lightsOn = true, reverse = true, handbrake = true, belt = true,
                rearLevels = listOf(0, 0, 2, 3), speedKmh = 0.0), onDebug = {})
        }
    }

    @Test fun overviewDayDoors() {
        assumeTrue(System.getenv("DRIVEOS_SHOTS") == "1")
        shot("2_veicolo_giorno_porte", ThemeMode.LIGHT) {
            VehicleOverviewScreen(base.copy(doorOpen = true, openDoors = listOf("ant. dx", "post. sx", "baule"),
                alerts = listOf(ActiveAlert("Sensori parcheggio"))), onDebug = {})
        }
    }

    @Test fun overviewNightDriving() {
        assumeTrue(System.getenv("DRIVEOS_SHOTS") == "1")
        shot("3_veicolo_notte_marcia", ThemeMode.DARK) {
            VehicleOverviewScreen(base.copy(lightsOn = true, speedKmh = 48.0, belt = true), onDebug = {})
        }
    }

    @Test fun keys() {
        assumeTrue(System.getenv("DRIVEOS_SHOTS") == "1")
        shot("4_tasti_e_volante", ThemeMode.DARK) {
            KeysScreen(
                bindings = listOf(KeyBinding("can:C30/01", "02", KeyAction.VOL_UP), KeyBinding("can:C30/01", "04", KeyAction.VOL_DOWN)),
                learning = KeyAction.MUTE, message = "Premi ora il tasto per «Muto»…",
                onLearn = {}, onClear = {}, onCancel = {}, onBack = {},
            )
        }
    }

    @Test fun debug() {
        assumeTrue(System.getenv("DRIVEOS_SHOTS") == "1")
        shot("5_debug_veicolo", ThemeMode.LIGHT) {
            VehicleScreen(
                signals = linkedMapOf("can:door_fl" to "0", "can:lights" to "1", "can:battery_v" to "14.1",
                    "can:rear" to "1 1 3 1", "can:C14/3f" to "", "prop:forfan_light_state" to "on"),
                logsGranted = true, loggingOn = true, loggedChanges = 312, onToggleLogging = {},
                canSerialOn = true, onToggleCanSerial = {}, onOpenKeys = {}, onExportLog = {}, onClearLog = {},
                exportStatus = null, onBack = {},
            )
        }
    }
}
