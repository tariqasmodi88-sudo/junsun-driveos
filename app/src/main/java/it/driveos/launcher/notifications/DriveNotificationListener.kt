package it.driveos.launcher.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import it.driveos.launcher.DriveOSApp
import it.driveos.launcher.nav.NavGuidance
import it.driveos.launcher.nav.NavGuidanceStore

/**
 * Accesso alle notifiche concesso dall'utente. Serve a due cose sole:
 * 1. leggere le sessioni multimediali attive (titolo, artista, comandi);
 * 2. leggere la notifica di guida dell'app di navigazione (prossima manovra, arrivo).
 * Tutte le altre notifiche vengono ignorate subito.
 */
class DriveNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching { activeNotifications?.forEach { handle(it) } }
        (application as? DriveOSApp)?.mediaRepository?.start()
    }

    override fun onListenerDisconnected() {
        NavGuidanceStore.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NavGuidanceStore.remove(sbn.packageName, sbn.key)
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName !in NavGuidanceStore.watchedPackages) return
        val n = sbn.notification ?: return
        val isNavigation = n.category == "navigation" // Notification.CATEGORY_NAVIGATION
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (!isNavigation && !isOngoing) return

        val e = n.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = (e.getCharSequence(Notification.EXTRA_TEXT) ?: e.getCharSequence(Notification.EXTRA_BIG_TEXT))
            ?.toString().orEmpty().trim()
        val sub = e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty().trim()
        if (title.isEmpty() && text.isEmpty()) return

        val icon = runCatching {
            n.getLargeIcon()?.loadDrawable(this)?.toBitmap(160, 160)?.asImageBitmap()
        }.getOrNull()

        NavGuidanceStore.update(NavGuidance(sbn.packageName, sbn.key, title, text, sub, icon))
    }
}
