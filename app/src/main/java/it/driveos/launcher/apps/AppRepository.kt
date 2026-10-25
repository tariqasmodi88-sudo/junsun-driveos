package it.driveos.launcher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AppEntry(
    val key: AppKey,
    val label: String,
    val icon: ImageBitmap,
    val isSystem: Boolean,
)

/** Elenco delle app avviabili, letto con le API pubbliche di PackageManager. */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun refresh() {
        val list = withContext(Dispatchers.IO) { loadApps() }
        _apps.value = list
        _loaded.value = true
    }

    private fun loadApps(): List<AppEntry> {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val iconPx = (64 * appContext.resources.displayMetrics.density).toInt().coerceAtLeast(48)

        val entries = pm.queryIntentActivities(intent, 0).mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            if (activity.packageName == appContext.packageName) return@mapNotNull null
            // Un'icona difettosa non deve far sparire l'intero elenco.
            runCatching {
                AppEntry(
                    key = AppKey(activity.packageName, activity.name),
                    label = info.loadLabel(pm).toString().ifBlank { activity.packageName },
                    icon = info.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap(),
                    isSystem = (activity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }.getOrNull()
        }
        return AppSearch.sortByLabel(entries.distinctBy { it.key }) { it.label }
    }
}
