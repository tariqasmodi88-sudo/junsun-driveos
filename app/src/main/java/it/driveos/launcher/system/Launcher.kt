package it.driveos.launcher.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import android.content.ContentUris
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.nav.NavIntentBuilder
import it.driveos.launcher.nav.NavRequest

sealed interface LaunchResult {
    data object Ok : LaunchResult
    data class Error(val message: String) : LaunchResult
}

/** Tutti gli avvii di altre app passano da qui: nessun errore deve chiudere il launcher. */
object Launcher {

    fun launchApp(context: Context, key: AppKey): LaunchResult {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(key.packageName, key.className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(context, intent, "Impossibile aprire l'app: potrebbe essere stata disinstallata.")
    }

    fun navigate(context: Context, navPackage: String?, destination: String): LaunchResult {
        val request = NavIntentBuilder.forDestination(navPackage, destination)
            ?: return LaunchResult.Error("Destinazione vuota.")
        val first = start(context, viewIntent(request), "L'app scelta non accetta la destinazione.")
        if (first is LaunchResult.Ok || request.packageName == null) return first
        // L'app scelta non gestisce l'URI: si lascia scegliere ad Android un'app compatibile.
        val fallback = NavIntentBuilder.genericFallback(destination) ?: return first
        return start(context, viewIntent(fallback), "Nessuna app installata accetta questa destinazione.")
    }

    fun openSystemSettings(context: Context, action: String): LaunchResult {
        val result = start(context, Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "")
        if (result is LaunchResult.Ok || action == Settings.ACTION_SETTINGS) return result
        return start(
            context,
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            "Impossibile aprire le impostazioni di sistema."
        )
    }

    /** Apre Google Calendar se installato, altrimenti l'app calendario predefinita. */
    fun openCalendar(context: Context): LaunchResult {
        context.packageManager.getLaunchIntentForPackage(GOOGLE_CALENDAR)?.let {
            return start(context, it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "")
        }
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
        return start(
            context,
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            "Nessuna app calendario installata. Installa Google Calendar dal Play Store.",
        )
    }

    fun openCalendarEvent(context: Context, eventId: Long, begin: Long, end: Long): LaunchResult {
        val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent, "Nessuna app calendario può aprire l'evento.")
    }

    fun openAppDetails(context: Context): LaunchResult = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        "Impossibile aprire le impostazioni dell'app.",
    )

    private const val GOOGLE_CALENDAR = "com.google.android.calendar"

    private fun viewIntent(request: NavRequest): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(request.uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { request.packageName?.let { setPackage(it) } }

    private fun start(context: Context, intent: Intent, error: String): LaunchResult = try {
        context.startActivity(intent)
        LaunchResult.Ok
    } catch (e: ActivityNotFoundException) {
        LaunchResult.Error(error)
    } catch (e: SecurityException) {
        LaunchResult.Error(error)
    }
}
