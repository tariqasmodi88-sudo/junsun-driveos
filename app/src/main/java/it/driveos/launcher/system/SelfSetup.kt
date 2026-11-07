package it.driveos.launcher.system

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

/**
 * Permessi "da ADB" concessi da DriveOS stesso, senza telefono ne' Termux.
 *
 * La radio tiene acceso il debug di rete (porta 5555): DriveOS si collega a se stessa (127.0.0.1:5555)
 * come farebbe adb dal telefono. La prima volta la radio chiede "Consentire il debug?": si tocca
 * "Consenti sempre" e da li' in poi funziona da solo, anche dopo un reset di Android.
 *
 * Concede: lettura log (retromarcia dal registro, nomi RDS), "mostra sopra altre app",
 * accesso alle notifiche e servizio di Accessibilita'.
 */
object SelfSetup {
    private const val TAG = "DriveOS"
    @Volatile var running = false
        private set
    @Volatile var lastResult: String? = null
        private set

    /** true se manca qualcosa (registro, Accessibilita', sovrapposizione): dopo un reset di Android spariscono. */
    fun needed(ctx: Context): Boolean {
        val a11y = runCatching {
            android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        return !logsGranted(ctx) || !a11y.contains(ctx.packageName) || !android.provider.Settings.canDrawOverlays(ctx)
    }

    fun logsGranted(ctx: Context) =
        ctx.checkSelfPermission(android.Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    private fun keyPair(ctx: Context): AdbKeyPair {
        val priv = File(ctx.filesDir, "adb/adbkey")
        val pub = File(ctx.filesDir, "adb/adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }

    /**
     * Esegue la configurazione (bloccante: chiamare da un thread di sfondo).
     * Se il permesso ai log era mancante, alla fine riavvia DriveOS (serve un processo nuovo per leggere i log).
     */
    fun run(ctx: Context): Boolean {
        if (running) return false
        running = true
        try {
            val pkg = ctx.packageName
            val hadLogs = logsGranted(ctx)
            val a11y = "$pkg/$pkg.vehicle.DriveAccessibilityService"
            val notif = "$pkg/$pkg.notifications.DriveNotificationListener"
            Dadb.create("127.0.0.1", 5555, keyPair(ctx), connectTimeout = 3_000, socketTimeout = 90_000).use { d ->
                fun sh(cmd: String): String = runCatching { d.shell(cmd).allOutput.trim() }
                    .getOrElse { e -> "errore: ${e.message}" }
                    .also { Log.i(TAG, "setup: $cmd -> $it") }
                sh("pm grant $pkg android.permission.READ_LOGS")
                sh("appops set $pkg SYSTEM_ALERT_WINDOW allow")
                sh("cmd notification allow_listener $notif")
                val cur = sh("settings get secure enabled_accessibility_services")
                if (!cur.contains(a11y)) {
                    val list = cur.takeIf { it.isNotBlank() && it != "null" && !it.startsWith("errore") }
                    sh("settings put secure enabled_accessibility_services " + if (list != null) "$list:$a11y" else a11y)
                    sh("settings put secure accessibility_enabled 1")
                }
                lastResult = "ok"
                if (!hadLogs) {
                    // processo nuovo con il permesso ai log: si chiude e si riapre la Home
                    sh("am force-stop $pkg; sleep 1; am start -n $pkg/.MainActivity")
                }
            }
            return true
        } catch (t: Throwable) {
            lastResult = t.message ?: t.javaClass.simpleName
            Log.w(TAG, "setup automatico non riuscito: $t")
            return false
        } finally {
            running = false
        }
    }
}
