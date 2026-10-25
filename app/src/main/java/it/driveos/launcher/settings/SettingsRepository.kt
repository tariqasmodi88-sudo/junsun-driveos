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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.driveDataStore: DataStore<Preferences> by preferencesDataStore(name = "driveos_settings")

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class DriveSettings(
    val navApp: AppKey? = null,
    val musicApp: AppKey? = null,
    val phoneApp: AppKey? = null,
    val homeAddress: String = "",
    val favorites: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentIndex: Int = 0,
)

/** Preferenze persistenti (DataStore): sopravvivono a riavvii e aggiornamenti dell'app. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.driveDataStore

    private object Keys {
        val NAV = stringPreferencesKey("nav_app")
        val MUSIC = stringPreferencesKey("music_app")
        val PHONE = stringPreferencesKey("phone_app")
        val HOME = stringPreferencesKey("home_address")
        val FAVORITES = stringPreferencesKey("favorites")
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = intPreferencesKey("accent_index")
    }

    val settings: Flow<DriveSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            DriveSettings(
                navApp = AppKey.decode(p[Keys.NAV]),
                musicApp = AppKey.decode(p[Keys.MUSIC]),
                phoneApp = AppKey.decode(p[Keys.PHONE]),
                homeAddress = p[Keys.HOME].orEmpty(),
                favorites = FavoritesCodec.decode(p[Keys.FAVORITES]),
                themeMode = p[Keys.THEME]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.DARK,
                accentIndex = p[Keys.ACCENT] ?: 0,
            )
        }

    suspend fun setNavApp(key: AppKey?) = setApp(Keys.NAV, key)
    suspend fun setMusicApp(key: AppKey?) = setApp(Keys.MUSIC, key)
    suspend fun setPhoneApp(key: AppKey?) = setApp(Keys.PHONE, key)

    suspend fun setHomeAddress(address: String) {
        store.edit { it[Keys.HOME] = address.trim() }
    }

    suspend fun toggleFavorite(key: AppKey) {
        store.edit {
            val current = FavoritesCodec.decode(it[Keys.FAVORITES])
            it[Keys.FAVORITES] = FavoritesCodec.encode(FavoritesCodec.toggle(current, key.encode()))
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setAccent(index: Int) {
        store.edit { it[Keys.ACCENT] = index }
    }

    private suspend fun setApp(prefKey: Preferences.Key<String>, key: AppKey?) {
        store.edit { if (key == null) it.remove(prefKey) else it[prefKey] = key.encode() }
    }
}
