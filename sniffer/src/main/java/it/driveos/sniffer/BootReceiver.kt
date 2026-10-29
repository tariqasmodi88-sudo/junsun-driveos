package it.driveos.sniffer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Se la registrazione era attiva, riparte da sola all'accensione della radio. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val on = context.getSharedPreferences("sniffer", Context.MODE_PRIVATE).getBoolean("on", false)
        if (on) runCatching { context.startForegroundService(Intent(context, SniffService::class.java)) }
    }
}
