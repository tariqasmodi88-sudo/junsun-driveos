package it.driveos.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.driveos.launcher.DriveOSApp

/**
 * Avvio della radio (o riaccensione rapida, se il firmware la segnala): fa partire il processo
 * DriveOS anche se era stato chiuso, e riarma il ritorno alla Home dei primi secondi.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as? DriveOSApp)?.scheduleWakeReturn()
    }
}
