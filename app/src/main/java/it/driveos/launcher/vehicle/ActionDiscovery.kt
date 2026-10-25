package it.driveos.launcher.vehicle

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.util.zip.ZipFile

/**
 * Trova i nomi delle azioni broadcast che le app Junsun pubblicano, leggendo i loro APK
 * (file pubblici, nessun permesso). Cosi' DriveOS puo' mettersi in ascolto di esse e
 * scoprire quali portano i dati del veicolo (fari, carburante, pneumatici, warning).
 *
 * Non esegue codice delle altre app: legge solo testo dentro i loro file.
 */
object ActionDiscovery {

    /** Pacchetti tipici dei firmware cinesi per auto (CAN/MCU/radio/retro). */
    private val vehiclePackageHints = listOf(
        "canbus", "can", "mcu", "carinfo", "car", "radio", "fm", "vehicle", "setting",
        "factory", "powermanager", "backcar", "reverse", "dvr", "ill", "cluster",
    )

    data class Scan(val actions: List<String>, val scannedApps: List<String>)

    fun discover(context: Context, maxBytesPerApp: Long = 40L * 1024 * 1024): Scan {
        val pm = context.packageManager
        val actions = LinkedHashSet<String>()
        val scanned = ArrayList<String>()

        val packages = pm.getInstalledApplications(0)
            .filter { app ->
                val p = app.packageName.lowercase()
                p != context.packageName && vehiclePackageHints.any { p.contains(it) }
            }
            .sortedBy { it.packageName }

        for (app in packages) {
            val apkPath = app.sourceDir ?: continue
            val file = File(apkPath)
            if (!file.canRead()) continue
            runCatching {
                scanApk(file, maxBytesPerApp).forEach { actions.add(it) }
                scanned.add(app.packageName)
            }
        }
        return Scan(actions.toList().sorted(), scanned)
    }

    /** Legge le voci di testo dell'APK (manifest binario, classes.dex) e ne estrae le azioni. */
    private fun scanApk(apk: File, maxBytes: Long): Set<String> {
        val scanner = StringScanner(minLength = 10)
        var budget = maxBytes
        ZipFile(apk).use { zip ->
            val entries = zip.entries().toList().filter { e ->
                val n = e.name
                n == "AndroidManifest.xml" || n.endsWith(".dex") || n == "resources.arsc"
            }
            for (entry in entries) {
                if (budget <= 0) break
                zip.getInputStream(entry).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (budget > 0) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        scanner.feed(buffer, read)
                        budget -= read
                    }
                }
            }
        }
        val out = LinkedHashSet<String>()
        scanner.finish().forEach { raw -> ActionFilter.extract(raw).forEach { out.add(it) } }
        return out
    }
}
