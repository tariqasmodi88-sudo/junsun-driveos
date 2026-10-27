package it.driveos.launcher

import it.driveos.launcher.vehicle.KnownVehicleSignals
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalRole
import it.driveos.launcher.vehicle.VehicleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleStatusTest {

    @Test fun knownLightsFromForfanProp() {
        assertEquals(true, KnownVehicleSignals.lightsOn(mapOf("prop:forfan_light_state" to "on")))
        assertEquals(false, KnownVehicleSignals.lightsOn(mapOf("prop:forfan_light_state" to "off")))
        assertNull(KnownVehicleSignals.lightsOn(mapOf("prop:other" to "x")))
    }

    @Test fun syntheticLightsBinding() {
        val b = KnownVehicleSignals.syntheticLightsBinding(mapOf("prop:forfan_light_state" to "on"))!!
        assertEquals("prop:forfan_light_state", b.id)
        assertEquals(SignalRole.LIGHTS, b.role)
        assertNull(KnownVehicleSignals.syntheticLightsBinding(mapOf("prop:x" to "y")))
    }

    @Test fun systemNavPackage() {
        assertEquals("com.google.android.apps.maps",
            KnownVehicleSignals.systemNavPackage(mapOf("prop:forfan.navi.name" to "com.google.android.apps.maps")))
        assertNull(KnownVehicleSignals.systemNavPackage(mapOf("prop:forfan.navi.name" to "")))
    }

    @Test fun gaugesAlwaysListedEvenWhenMissing() {
        val bindings = listOf(
            SignalBinding("prop:fuel", SignalRole.GAUGE, "Carburante", unit = "L"),
            SignalBinding("prop:temp", SignalRole.GAUGE, "Temp olio", unit = "°C"),
        )
        val g = VehicleStatus.gauges(bindings, mapOf("prop:fuel" to "42"))
        assertEquals("42", g[0].value)
        assertEquals("—", g[1].value) // assente → trattino, mai inventato
    }

    @Test fun alertsShowOnlyWhenActive() {
        val bindings = listOf(
            SignalBinding("bc:tpms|st", SignalRole.ALERT, "Pressione pneumatici", onValue = "1", offValue = "0"),
            SignalBinding("bc:oil|st", SignalRole.ALERT, "Pressione olio", onValue = "1", offValue = "0"),
        )
        val active = VehicleStatus.activeAlerts(bindings, mapOf("bc:tpms|st" to "1", "bc:oil|st" to "0"))
        assertEquals(listOf("Pressione pneumatici"), active.map { it.label })
        // segnale assente → spia non attiva (niente falsi allarmi)
        assertTrue(VehicleStatus.activeAlerts(bindings, emptyMap()).isEmpty())
    }

    @Test fun alertActiveWhenDifferentFromOffIfNoOnValue() {
        val b = listOf(SignalBinding("prop:door", SignalRole.ALERT, "Porta aperta", onValue = null, offValue = "closed"))
        assertEquals(1, VehicleStatus.activeAlerts(b, mapOf("prop:door" to "open")).size)
        assertTrue(VehicleStatus.activeAlerts(b, mapOf("prop:door" to "closed")).isEmpty())
    }

    @Test fun lightsPrefersExplicitBindingThenKnown() {
        val binding = SignalBinding("bc:ill|s", SignalRole.LIGHTS, "Fari", "1", "0")
        assertEquals(true, VehicleStatus.lightsOn(binding, mapOf("bc:ill|s" to "1")))
        // nessun binding → usa il segnale noto
        assertEquals(true, VehicleStatus.lightsOn(null, mapOf("prop:forfan_light_state" to "on")))
        assertNull(VehicleStatus.lightsOn(null, emptyMap()))
    }

    @Test fun phoneAndProjectionFromRealLog() {
        val sig = mapOf(KnownVehicleSignals.PHONE_HFP_ID to "1", KnownVehicleSignals.PROJECTION_ID to "1")
        assertEquals(true, KnownVehicleSignals.phoneConnected(sig))
        assertTrue(KnownVehicleSignals.projectionActive(sig))
        assertEquals(false, KnownVehicleSignals.phoneConnected(mapOf(KnownVehicleSignals.PHONE_HFP_ID to "0")))
        assertEquals(listOf("Telefono", "CarPlay/AA"), VehicleStatus.knownStatus(sig).map { it.label })
    }
}
