package it.driveos.launcher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.apps.FavoritesCodec
import it.driveos.launcher.dashboard.DashboardLayout
import it.driveos.launcher.nav.DestinationHistory
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioPresetCodec
import it.driveos.launcher.vehicle.BindingCodec
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.driveDataStore: DataStore<Preferences> by preferencesDataStore(name = "driveos_settings")

enum class ThemeMode { DARK, LIGHT, SYSTEM, AUTO }

data class DriveSettings(
    val navApp: AppKey? = null,
    val musicApp: AppKey? = null,
    val phoneApp: AppKey? = null,
    val radioApp: AppKey? = null,
    val homeAddress: String = "",
    val workAddress: String = "",
    val recentDestinations: List<String> = emptyList(),
    val layout: DashboardLayout = DashboardLayout.DEFAULT,
    val favorites: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentIndex: Int = 0,
    // Segnali del veicolo collegati dall'utente (fari per il tema, valori per il cruscotto).
    val signalBindings: List<SignalBinding> = emptyList(),
    // Azioni broadcast scoperte nelle app Junsun, da ascoltare (persistite dopo la scansione).
    val discoveredActions: List<String> = emptyList(),
    // Posizione per il calcolo alba/tramonto (gradi), 0,0 = non impostata → usa Roma di default.
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radioPresets: List<RadioPreset> = emptyList(),
    // Avvio intelligente alla connessione Bluetooth (sostituisce AutoLink).
    val smartStartEnabled: Boolean = true,
    val smartStartMusic: Boolean = true,
    val smartStartNav: Boolean = false,
    val smartStartAndroidAuto: Boolean = true,
    val onlineMusicApp: AppKey? = null,
    val offlineMusicApp: AppKey? = null,
    val vehicleLogging: Boolean = false,
    /** Schermata veicolo automatica quando l'auto è ferma (o si apre una portiera). */
    val autoOverview: Boolean = true,
    /** Le finestre Junsun (portiere, radar, retro) e CarPlay non prendono lo schermo: si resta su DriveOS. */
    val keepHome: Boolean = true,
) {
    val lightsBinding: SignalBinding? get() = signalBindings.firstOrNull { it.role == SignalRole.LIGHTS }
    val gaugeBindings: List<SignalBinding> get() = signalBindings.filter { it.role == SignalRole.GAUGE }
}

/** Preferenze persistenti (DataStore): sopravvivono a riavvii e aggiornamenti dell'app. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.driveDataStore

    private object Keys {
        val NAV = stringPreferencesKey("nav_app")
        val MUSIC = stringPreferencesKey("music_app")
        val PHONE = stringPreferencesKey("phone_app")
        val RADIO = stringPreferencesKey("radio_app")
        val HOME = stringPreferencesKey("home_address")
        val WORK = stringPreferencesKey("work_address")
        val RECENT = stringPreferencesKey("recent_destinations")
        val LAYOUT = stringPreferencesKey("dashboard_layout")
        val FAVORITES = stringPreferencesKey("favorites")
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = intPreferencesKey("accent_index")
        val SIGNALS = stringPreferencesKey("signal_bindings")
        val ACTIONS = stringPreferencesKey("discovered_actions")
        val LAT = stringPreferencesKey("latitude")
        val LON = stringPreferencesKey("longitude")
        val PRESETS = stringPreferencesKey("radio_presets")
        val SS_ENABLED = stringPreferencesKey("ss_enabled")
        val SS_MUSIC = stringPreferencesKey("ss_music")
        val SS_NAV = stringPreferencesKey("ss_nav")
        val SS_AA = stringPreferencesKey("ss_aa")
        val ONLINE_MUSIC = stringPreferencesKey("online_music_app")
        val OFFLINE_MUSIC = stringPreferencesKey("offline_music_app")
        val VEH_LOG = stringPreferencesKey("vehicle_logging")
        val THEME_V2 = stringPreferencesKey("theme_auto_default")
        val AUTO_OVERVIEW = stringPreferencesKey("auto_overview")
        val KEEP_HOME = stringPreferencesKey("keep_home")
    }

    val settings: Flow<DriveSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            DriveSettings(
                navApp = AppKey.decode(p[Keys.NAV]),
                musicApp = AppKey.decode(p[Keys.MUSIC]),
                phoneApp = AppKey.decode(p[Keys.PHONE]),
                radioApp = AppKey.decode(p[Keys.RADIO]),
                homeAddress = p[Keys.HOME].orEmpty(),
                workAddress = p[Keys.WORK].orEmpty(),
                recentDestinations = DestinationHistory.decode(p[Keys.RECENT]),
                layout = DashboardLayout.fromId(p[Keys.LAYOUT]),
                favorites = FavoritesCodec.decode(p[Keys.FAVORITES]),
                // Dalla 0.8.3 il predefinito è "Automatico" (fari → scuro/chiaro); una sola volta
                // anche per chi aveva un'altra scelta, poi vale quella che sceglie.
                themeMode = if (p[Keys.THEME_V2] != "1") ThemeMode.AUTO else p[Keys.THEME]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.AUTO,
                accentIndex = p[Keys.ACCENT] ?: 0,
                signalBindings = BindingCodec.decode(p[Keys.SIGNALS]),
                discoveredActions = p[Keys.ACTIONS]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
                latitude = p[Keys.LAT]?.toDoubleOrNull() ?: 0.0,
                longitude = p[Keys.LON]?.toDoubleOrNull() ?: 0.0,
                radioPresets = RadioPresetCodec.decode(p[Keys.PRESETS]),
                smartStartEnabled = p[Keys.SS_ENABLED] != "0",
                smartStartMusic = p[Keys.SS_MUSIC] != "0",
                smartStartNav = p[Keys.SS_NAV] == "1",
                smartStartAndroidAuto = p[Keys.SS_AA] != "0",
                onlineMusicApp = AppKey.decode(p[Keys.ONLINE_MUSIC]),
                offlineMusicApp = AppKey.decode(p[Keys.OFFLINE_MUSIC]),
                vehicleLogging = p[Keys.VEH_LOG] != "0",
                autoOverview = p[Keys.AUTO_OVERVIEW] != "0",
                keepHome = p[Keys.KEEP_HOME] != "0",
            )
        }

    suspend fun setNavApp(key: AppKey?) = setApp(Keys.NAV, key)
    suspend fun setMusicApp(key: AppKey?) = setApp(Keys.MUSIC, key)
    suspend fun setPhoneApp(key: AppKey?) = setApp(Keys.PHONE, key)
    suspend fun setRadioApp(key: AppKey?) = setApp(Keys.RADIO, key)

    suspend fun setThemeModeByName(mode: ThemeMode) = setThemeMode(mode)

    suspend fun upsertBinding(binding: SignalBinding) {
        store.edit {
            it[Keys.SIGNALS] = BindingCodec.encode(BindingCodec.upsert(BindingCodec.decode(it[Keys.SIGNALS]), binding))
        }
    }

    suspend fun removeBinding(id: String, role: SignalRole) {
        store.edit {
            it[Keys.SIGNALS] = BindingCodec.encode(BindingCodec.remove(BindingCodec.decode(it[Keys.SIGNALS]), id, role))
        }
    }

    /** Azzera tutti i collegamenti (fari/gauge/spie) scelti a mano. */
    suspend fun clearAllBindings() {
        store.edit { it[Keys.SIGNALS] = "" }
    }

    suspend fun setAutoOverview(on: Boolean) {
        store.edit { it[Keys.AUTO_OVERVIEW] = if (on) "1" else "0" }
    }

    suspend fun setKeepHome(on: Boolean) {
        store.edit { it[Keys.KEEP_HOME] = if (on) "1" else "0" }
    }

    suspend fun setVehicleLogging(on: Boolean) {
        store.edit { it[Keys.VEH_LOG] = if (on) "1" else "0" }
    }

    suspend fun setDiscoveredActions(actions: List<String>) {
        store.edit { it[Keys.ACTIONS] = actions.joinToString("\n") }
    }

    suspend fun setLocation(lat: Double, lon: Double) {
        store.edit { it[Keys.LAT] = lat.toString(); it[Keys.LON] = lon.toString() }
    }

    suspend fun setRadioPresets(presets: List<RadioPreset>) {
        store.edit { it[Keys.PRESETS] = RadioPresetCodec.encode(presets) }
    }

    suspend fun setSmartStartEnabled(on: Boolean) { store.edit { it[Keys.SS_ENABLED] = if (on) "1" else "0" } }
    suspend fun setSmartStartMusic(on: Boolean) { store.edit { it[Keys.SS_MUSIC] = if (on) "1" else "0" } }
    suspend fun setSmartStartNav(on: Boolean) { store.edit { it[Keys.SS_NAV] = if (on) "1" else "0" } }
    suspend fun setSmartStartAndroidAuto(on: Boolean) { store.edit { it[Keys.SS_AA] = if (on) "1" else "0" } }
    suspend fun setOnlineMusicApp(key: AppKey?) = setApp(Keys.ONLINE_MUSIC, key)
    suspend fun setOfflineMusicApp(key: AppKey?) = setApp(Keys.OFFLINE_MUSIC, key)

    suspend fun setHomeAddress(address: String) {
        store.edit { it[Keys.HOME] = address.trim() }
    }

    suspend fun setWorkAddress(address: String) {
        store.edit { it[Keys.WORK] = address.trim() }
    }

    suspend fun addRecentDestination(destination: String) {
        store.edit {
            it[Keys.RECENT] = DestinationHistory.encode(DestinationHistory.add(DestinationHistory.decode(it[Keys.RECENT]), destination))
        }
    }

    suspend fun removeRecentDestination(destination: String) {
        store.edit {
            it[Keys.RECENT] = DestinationHistory.encode(DestinationHistory.remove(DestinationHistory.decode(it[Keys.RECENT]), destination))
        }
    }

    suspend fun setLayout(layout: DashboardLayout) {
        store.edit { it[Keys.LAYOUT] = layout.id }
    }

    suspend fun toggleFavorite(key: AppKey) {
        store.edit {
            val current = FavoritesCodec.decode(it[Keys.FAVORITES])
            it[Keys.FAVORITES] = FavoritesCodec.encode(FavoritesCodec.toggle(current, key.encode()))
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.name; it[Keys.THEME_V2] = "1" }
    }

    suspend fun setAccent(index: Int) {
        store.edit { it[Keys.ACCENT] = index }
    }

    private suspend fun setApp(prefKey: Preferences.Key<String>, key: AppKey?) {
        store.edit { if (key == null) it.remove(prefKey) else it[prefKey] = key.encode() }
    }
}
