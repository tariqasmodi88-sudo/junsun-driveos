package it.driveos.launcher.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.DisplayMetrics
import android.view.InputDevice
import android.view.WindowManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.ZonedDateTime

/**
 * Raccoglie, con sole API pubbliche e senza permessi, le informazioni che servono
 * allo sviluppo (sostituisce l'acquisizione via ADB). Non legge dati personali,
 * a parte l'elenco delle app installate.
 */
object DeviceInfoCollector {

    fun collect(context: Context): JSONObject {
        val pm = context.packageManager
        val root = JSONObject()
        root.put("generato", ZonedDateTime.now().toString())
        root.put("appVersione", versionOf(pm, context.packageName) ?: JSONObject.NULL)

        root.put("build", JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("product", Build.PRODUCT)
            put("hardware", Build.HARDWARE)
            put("board", Build.BOARD)
            put("display", Build.DISPLAY)
            put("fingerprint", Build.FINGERPRINT)
            put("type", Build.TYPE)
            put("tags", Build.TAGS)
            put("release", Build.VERSION.RELEASE)
            put("sdk", Build.VERSION.SDK_INT)
            put("securityPatch", Build.VERSION.SECURITY_PATCH)
            put("abi", JSONArray(Build.SUPPORTED_ABIS.toList()))
        })

        val metrics = context.resources.displayMetrics
        val real = DisplayMetrics()
        runCatching {
            @Suppress("DEPRECATION")
            context.getSystemService(WindowManager::class.java)?.defaultDisplay?.getRealMetrics(real)
        }
        root.put("display", JSONObject().apply {
            put("realWidthPx", real.widthPixels)
            put("realHeightPx", real.heightPixels)
            put("appWidthPx", metrics.widthPixels)
            put("appHeightPx", metrics.heightPixels)
            put("densityDpi", metrics.densityDpi)
            put("density", metrics.density.toDouble())
            put("smallestWidthDp", context.resources.configuration.smallestScreenWidthDp)
        })

        val am = context.getSystemService(ActivityManager::class.java)
        if (am != null) {
            val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            root.put("memoria", JSONObject().apply {
                put("totaleMB", mem.totalMem / MB)
                put("disponibileMB", mem.availMem / MB)
                put("memoryClassMB", am.memoryClass)
                put("lowRam", am.isLowRamDevice)
            })
        }

        root.put("cpu", JSONObject().apply {
            put("core", Runtime.getRuntime().availableProcessors())
            put("cpuinfo", cpuInfoSummary())
        })

        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        root.put("home", JSONObject().apply {
            val def = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            put("predefinita", def?.let { "${it.packageName}/${it.name}" } ?: JSONObject.NULL)
            put("candidati", handlers(pm, home, 0))
        })

        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        root.put("appAvviabili", JSONArray().apply {
            pm.queryIntentActivities(launcher, 0).forEach { info ->
                val a = info.activityInfo
                put(JSONObject().apply {
                    put("label", runCatching { info.loadLabel(pm).toString() }.getOrDefault(""))
                    put("package", a.packageName)
                    put("activity", a.name)
                    put("sistema", (a.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                    put("versione", versionOf(pm, a.packageName) ?: JSONObject.NULL)
                })
            }
        })

        root.put("navigazione", JSONObject().apply {
            put("geo", handlers(pm, view("geo:0,0?q=Roma")))
            put("googleNavigation", handlers(pm, view("google.navigation:q=Roma")))
            put("waze", handlers(pm, view("https://waze.com/ul?q=Roma&navigate=yes")))
        })

        root.put("voce", JSONObject().apply {
            put("riconoscimentoDisponibile", SpeechRecognizer.isRecognitionAvailable(context))
            put("servizi", JSONArray(
                pm.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
                    .map { "${it.serviceInfo.packageName}/${it.serviceInfo.name}" }
            ))
            put("recognizeSpeech", handlers(pm, Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)))
            put("voiceCommand", handlers(pm, Intent(Intent.ACTION_VOICE_COMMAND)))
            put("assist", handlers(pm, Intent(Intent.ACTION_ASSIST)))
        })

        root.put("multimedia", JSONObject().apply {
            put("mediaBrowserService", JSONArray(
                pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0)
                    .map { "${it.serviceInfo.packageName}/${it.serviceInfo.name}" }
            ))
            put("musicPlayer", handlers(pm, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)))
        })

        root.put("telefono", JSONObject().apply {
            put("dial", handlers(pm, Intent(Intent.ACTION_DIAL)))
            put("contatti", handlers(pm, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS)))
        })

        root.put("features", JSONArray(pm.systemAvailableFeatures.mapNotNull { it.name }.sorted()))

        val sensors = context.getSystemService(SensorManager::class.java)
        root.put("sensori", JSONArray(
            sensors?.getSensorList(Sensor.TYPE_ALL)?.map { "${it.name} (tipo ${it.type})" } ?: emptyList<String>()
        ))

        root.put("dispositiviInput", JSONArray().apply {
            InputDevice.getDeviceIds().forEach { id ->
                InputDevice.getDevice(id)?.let { d ->
                    put(JSONObject().apply {
                        put("nome", d.name)
                        put("sources", "0x" + Integer.toHexString(d.sources))
                        put("virtuale", d.isVirtual)
                    })
                }
            }
        })
        return root
    }

    /** Voci principali per la schermata di riepilogo. */
    fun summary(j: JSONObject): List<Pair<String, String>> {
        val b = j.optJSONObject("build")
        val d = j.optJSONObject("display")
        val m = j.optJSONObject("memoria")
        val c = j.optJSONObject("cpu")
        val h = j.optJSONObject("home")
        val v = j.optJSONObject("voce")
        val n = j.optJSONObject("navigazione")
        fun count(arr: JSONArray?) = (arr?.length() ?: 0).toString()
        return listOf(
            "Modello" to "${b?.optString("manufacturer")} ${b?.optString("model")}",
            "Android" to "${b?.optString("release")} (API ${b?.optInt("sdk")})",
            "Build" to b?.optString("display").orEmpty(),
            "ABI" to (b?.optJSONArray("abi")?.join(", ")?.replace("\"", "") ?: ""),
            "CPU" to "${c?.optInt("core")} core · ${c?.optString("cpuinfo")}",
            "Display" to "${d?.optInt("realWidthPx")}×${d?.optInt("realHeightPx")} px · " +
                "${d?.optInt("densityDpi")} dpi · sw${d?.optInt("smallestWidthDp")}dp",
            "RAM" to "${m?.optLong("totaleMB")} MB",
            "Home predefinita" to (h?.opt("predefinita")?.toString() ?: "—"),
            "Launcher installati" to count(h?.optJSONArray("candidati")),
            "App avviabili" to count(j.optJSONArray("appAvviabili")),
            "Riconoscimento vocale" to
                if (v?.optBoolean("riconoscimentoDisponibile") == true) "Disponibile" else "Non disponibile",
            "App per indirizzi (geo:)" to count(n?.optJSONArray("geo")),
            "Dispositivi di input" to count(j.optJSONArray("dispositiviInput")),
        )
    }

    private const val MB = 1024L * 1024L

    private fun view(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

    private fun handlers(pm: PackageManager, intent: Intent, flags: Int = PackageManager.MATCH_DEFAULT_ONLY) =
        JSONArray(pm.queryIntentActivities(intent, flags).map { "${it.activityInfo.packageName}/${it.activityInfo.name}" })

    @Suppress("DEPRECATION")
    private fun versionOf(pm: PackageManager, pkg: String): String? =
        runCatching { pm.getPackageInfo(pkg, 0).versionName }.getOrNull()

    private fun cpuInfoSummary(): String = runCatching {
        File("/proc/cpuinfo").readLines()
            .filter { it.startsWith("Hardware") || it.startsWith("model name") || it.startsWith("Processor") }
            .map { it.substringAfter(':').trim() }
            .distinct()
            .take(3)
            .joinToString(" | ")
    }.getOrDefault("")
}
