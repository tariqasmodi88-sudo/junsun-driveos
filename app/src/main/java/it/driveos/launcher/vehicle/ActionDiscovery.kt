package it.driveos.launcher.vehicle

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

/**
 * Trova i nomi delle azioni broadcast che le app del firmware pubblicano, leggendo i loro APK
 * (file pubblici, nessun permesso). Così DriveOS può mettersi in ascolto di esse e scoprire
 * quali portano i dati del veicolo (fari, carburante, pneumatici, warning) e il controllo radio.
 *
 * Due livelli, per essere sia completi sia veloci:
 *  - TUTTE le app non-Google: si legge solo il MANIFEST (piccolo) → cattura le azioni dei
 *    receiver dichiarati (Toppal, TXZ, Junsun, ts.car… qualunque sia il nome del package).
 *  - App "veicolo" (nome che contiene parole chiave): si legge anche il DEX → cattura anche
 *    le azioni costruite nel codice.
 *
 * Non esegue codice delle altre app: legge solo testo dentro i loro file.
 */
object ActionDiscovery {

    /** Parole nei package che indicano app di veicolo/firmware: meritano la scansione completa (dex). */
    private val vehiclePackageHints = listOf(
        "canbus", "can", "mcu", "carinfo", "car", "radio", "fm", "vehicle", "setting",
        "factory", "powermanager", "backcar", "reverse", "dvr", "ill", "cluster",
        "toppal", "txz", "junsun", "forfan", "suding", "ts.", "obd", "cluster", "security",
    )

    /** Package da saltare del tutto (framework e Google: solo rumore). */
    private val excludedPrefixes = listOf(
        "com.google.", "com.android.vending", "androidx.", "com.android.chrome",
    )

    data class Scan(val actions: List<String>, val scannedApps: List<String>, val vehicleApps: List<String>)

    fun discover(context: Context, maxBytesPerApp: Long = 40L * 1024 * 1024): Scan {
        val pm = context.packageManager
        val actions = LinkedHashSet<String>()
        val scanned = ArrayList<String>()
        val vehicleApps = ArrayList<String>()

        val packages = pm.getInstalledApplications(0)
            .filter { app ->
                val p = app.packageName.lowercase()
                p != context.packageName && excludedPrefixes.none { p.startsWith(it) }
            }
            .sortedBy { it.packageName }

        for (app in packages) {
            val apkPath = app.sourceDir ?: continue
            val file = File(apkPath)
            if (!file.canRead()) continue
            val p = app.packageName.lowercase()
            val deep = vehiclePackageHints.any { p.contains(it) }
            runCatching {
                scanApk(file, if (deep) maxBytesPerApp else 0L).forEach { actions.add(it) }
                scanned.add(app.packageName)
                if (deep) vehicleApps.add(app.packageName)
            }
        }
        return Scan(actions.toList().sorted(), scanned, vehicleApps)
    }

    /**
     * Legge il manifest sempre; il dex (classes*.dex) solo se dexBudget > 0.
     * resources.arsc viene letto solo insieme al dex (per le app veicolo).
     */
    private fun scanApk(apk: File, dexBudget: Long): Set<String> {
        val scanner = StringScanner(minLength = 10)
        ZipFile(apk).use { zip ->
            // 1) manifest (sempre, piccolo)
            zip.getEntry("AndroidManifest.xml")?.let { entry ->
                zip.getInputStream(entry).use { feedAll(it, scanner, Long.MAX_VALUE) }
            }
            // 2) dex + arsc (solo app veicolo)
            if (dexBudget > 0) {
                var budget = dexBudget
                zip.entries().toList().filter { it.name.endsWith(".dex") || it.name == "resources.arsc" }.forEach { entry ->
                    if (budget > 0) zip.getInputStream(entry).use { budget -= feedAll(it, scanner, budget) }
                }
            }
        }
        val out = LinkedHashSet<String>()
        scanner.finish().forEach { raw -> ActionFilter.extract(raw).forEach { out.add(it) } }
        return out
    }

    /** Alimenta lo scanner e restituisce i byte letti. */
    private fun feedAll(input: java.io.InputStream, scanner: StringScanner, max: Long): Long {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (total < max) {
            val read = input.read(buffer)
            if (read < 0) break
            scanner.feed(buffer, read)
            total += read
        }
        return total
    }
}
