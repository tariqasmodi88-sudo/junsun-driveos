package it.driveos.launcher.automation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Rileva quando un telefono è collegato in Bluetooth (profilo audio A2DP o handsfree).
 * Su Android 10 non servono permessi a runtime; su versioni più recenti userebbe
 * BLUETOOTH_CONNECT. Espone uno StateFlow e notifica i cambi di stato.
 */
class BluetoothWatcher(context: Context) {
    private val app = context.applicationContext

    private val _connected = MutableStateFlow(currentConnected())
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var receiver: BroadcastReceiver? = null

    private val impl = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            // Qualunque cambio di connessione: rileggiamo lo stato reale dei profili.
            _connected.value = currentConnected()
        }
    }

    fun start() {
        if (receiver != null) { _connected.value = currentConnected(); return }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice_ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice_ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(A2DP_CONNECTION_STATE_CHANGED)
            addAction(HEADSET_CONNECTION_STATE_CHANGED)
        }
        receiver = impl
        runCatching {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(impl, filter)
        }
        _connected.value = currentConnected()
    }

    fun stop() {
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        receiver = null
    }

    fun refresh() { _connected.value = currentConnected() }

    @Suppress("MissingPermission")
    private fun currentConnected(): Boolean = runCatching {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        if (!adapter.isEnabled) return false
        val a2dp = adapter.getProfileConnectionState(BluetoothProfile.A2DP)
        val headset = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
        a2dp == BluetoothProfile.STATE_CONNECTED || headset == BluetoothProfile.STATE_CONNECTED
    }.getOrDefault(false)

    private companion object {
        const val BluetoothDevice_ACTION_ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED"
        const val BluetoothDevice_ACTION_ACL_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED"
        const val A2DP_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"
        const val HEADSET_CONNECTION_STATE_CHANGED = "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"
    }
}
