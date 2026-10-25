package it.driveos.launcher.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class CalendarState(
    val permission: Boolean = false,
    val loaded: Boolean = false,
    val calendars: Int = 0,
    val events: List<CalendarEvent> = emptyList(),
    val error: String? = null,
)

/**
 * Legge gli impegni dal calendario di sistema (CalendarContract), cioe' gli account
 * Google sincronizzati sulla radio. Solo lettura: non crea ne' modifica eventi.
 */
class CalendarRepository(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var observerRegistered = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun refresh() {
        scope.launch {
            if (!hasPermission()) {
                _state.value = CalendarState(permission = false, loaded = true)
                return@launch
            }
            if (!observerRegistered) {
                runCatching {
                    app.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
                    observerRegistered = true
                }
            }
            _state.value = withContext(Dispatchers.IO) {
                runCatching { query(DAYS_AHEAD) }.getOrElse {
                    CalendarState(permission = true, loaded = true, error = it.message)
                }
            }
        }
    }

    private fun query(days: Long): CalendarState {
        val cr = app.contentResolver
        val calendars = cr.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.VISIBLE}=1",
            null,
            null,
        )?.use { it.count } ?: 0

        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(days).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val events = mutableListOf<CalendarEvent>()
        cr.query(uri, projection, "${CalendarContract.Instances.VISIBLE}=1", null, "${CalendarContract.Instances.BEGIN} ASC")
            ?.use { c ->
                while (c.moveToNext()) {
                    events += CalendarEvent(
                        eventId = c.getLong(0),
                        title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(senza titolo)",
                        begin = c.getLong(2),
                        end = c.getLong(3),
                        allDay = c.getInt(4) == 1,
                        location = c.getString(5).orEmpty().trim(),
                        color = if (c.isNull(6)) null else c.getInt(6),
                    )
                }
            }
        return CalendarState(permission = true, loaded = true, calendars = calendars, events = events)
    }

    companion object {
        const val DAYS_AHEAD = 14L
    }
}
