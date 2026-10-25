package it.driveos.launcher.automation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stato "online/offline" della radio (rete propria o tethering dal telefono). */
class ConnectivityWatcher(context: Context) {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val _online = MutableStateFlow(currentOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _online.value = currentOnline() }
        override fun onLost(network: Network) { _online.value = currentOnline() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    fun start() {
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        _online.value = currentOnline()
    }

    fun refresh() { _online.value = currentOnline() }

    private fun currentOnline(): Boolean {
        val c = cm ?: return false
        val net = c.activeNetwork ?: return false
        val caps = c.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
