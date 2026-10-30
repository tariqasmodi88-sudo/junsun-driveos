package it.driveos.launcher

import it.driveos.launcher.ui.theme.AutoSource
import it.driveos.launcher.ui.theme.AutoThemeResolver
import it.driveos.launcher.ui.theme.SunTimes
import it.driveos.launcher.vehicle.ActionFilter
import it.driveos.launcher.vehicle.BindingCodec
import it.driveos.launcher.vehicle.GetpropParser
import it.driveos.launcher.vehicle.LightsResolver
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalIds
import it.driveos.launcher.vehicle.SignalRole
import it.driveos.launcher.vehicle.SnapshotDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class VehicleLogicTest {

    @Test
    fun actionFilterKeepsActionsAndDropsClasses() {
        assertEquals(listOf("com.ts.can.ACTION_ILL"), ActionFilter.extract("(com.ts.can.ACTION_ILL"))
        assertTrue(ActionFilter.looksLikeAction("com.xyauto.canbus.DOOR_STATE"))
        assertTrue(ActionFilter.looksLikeAction("com.xx.action.radio.update"))
        assertTrue(ActionFilter.looksLikeAction("com.pvetec.mcu.ill"))
        assertFalse(ActionFilter.looksLikeAction("com.xx.radio.MainActivity"))
        assertFalse(ActionFilter.looksLikeAction("com.google.android.gms.ACTION_X"))
        assertFalse(ActionFilter.looksLikeAction("res.layout.main"))
        assertFalse(ActionFilter.looksLikeAction("android.intent.action.MAIN"))
    }

    @Test
    fun getpropParser() {
        val map = GetpropParser.parse("[ro.build.type]: [userdebug]\n[sys.ill.state]: [1]\ngarbage\n[empty]: []")
        assertEquals("userdebug", map["ro.build.type"])
        assertEquals("1", map["sys.ill.state"])
        assertEquals("", map["empty"])
        assertEquals(3, map.size)
    }

    @Test
    fun snapshotDiffReportsChangedAddedRemoved() {
        val d = SnapshotDiff.diff(mapOf("a" to "1", "b" to "2"), mapOf("a" to "1", "b" to "3", "c" to "x"))
        assertEquals(listOf("b", "c"), d.map { it.id })
        val r = SnapshotDiff.diff(mapOf("a" to "1"), emptyMap())
        assertNull(r.single().after)
    }

    @Test
    fun bindingCodecRoundTripAndUpsert() {
        val lights = SignalBinding("bc:com.x.ILL|state", SignalRole.LIGHTS, "Fari", "1", null)
        val fuel = SignalBinding("prop:car.fuel", SignalRole.GAUGE, "Carburante", unit = "L")
        val list = listOf(lights, fuel)
        assertEquals(list, BindingCodec.decode(BindingCodec.encode(list)))
        assertTrue(BindingCodec.decode(null).isEmpty())
        val other = SignalBinding("prop:sys.ill", SignalRole.LIGHTS, "Fari", "1", "0")
        val updated = BindingCodec.upsert(list, other)
        assertEquals(1, updated.count { it.role == SignalRole.LIGHTS })
        assertEquals("prop:sys.ill", updated.first { it.role == SignalRole.LIGHTS }.id)
        assertEquals(1, BindingCodec.remove(updated, "prop:car.fuel", SignalRole.GAUGE).size)
    }

    @Test
    fun lightsResolver() {
        val b = SignalBinding("prop:sys.ill", SignalRole.LIGHTS, "Fari", "1", "0")
        assertEquals(true, LightsResolver.resolve(b, "1"))
        assertEquals(false, LightsResolver.resolve(b, "0"))
        assertEquals(true, LightsResolver.resolve(b, "2"))
        assertNull(LightsResolver.resolve(b, null))
        assertNull(LightsResolver.resolve(null, "1"))
        assertNull(LightsResolver.resolve(b.copy(offValue = null), "7"))
    }

    @Test
    fun signalIds() {
        val id = SignalIds.broadcast("com.x.CAN", "fuel")
        assertEquals("com.x.CAN", SignalIds.actionOf(id))
        assertEquals("bc", SignalIds.source(id))
        assertNull(SignalIds.actionOf(SignalIds.prop("a")))
        assertTrue(SignalIds.describe(SignalIds.broadcastCount("com.x.CAN")).contains("conteggio"))
    }

    @Test
    fun sunTimesRome() {
        // Roma: 21 giugno alba ~03:35 UTC, tramonto ~18:48 UTC; 21 dicembre ~06:33 / ~15:42 UTC
        val summer = SunTimes.compute(LocalDate.of(2026, 6, 21), 41.9, 12.5)!!
        assertTrue(abs(summer.sunriseUtcMin - (3 * 60 + 35)) < 10)
        assertTrue(abs(summer.sunsetUtcMin - (18 * 60 + 48)) < 10)
        val winter = SunTimes.compute(LocalDate.of(2026, 12, 21), 41.9, 12.5)!!
        assertTrue(abs(winter.sunriseUtcMin - (6 * 60 + 33)) < 10)
        assertTrue(abs(winter.sunsetUtcMin - (15 * 60 + 42)) < 10)
        assertTrue(SunTimes.isNight(LocalDate.of(2026, 12, 21), 17.0 * 60, 41.9, 12.5))
        assertFalse(SunTimes.isNight(LocalDate.of(2026, 12, 21), 12.0 * 60, 41.9, 12.5))
    }

    @Test
    fun autoThemePriority() {
        assertEquals(false to AutoSource.LIGHTS, AutoThemeResolver.resolve(false, systemNight = true, sunNight = true))
        assertEquals(true to AutoSource.SYSTEM, AutoThemeResolver.resolve(null, systemNight = true, sunNight = false))
        assertEquals(true to AutoSource.SUN, AutoThemeResolver.resolve(null, systemNight = false, sunNight = true))
    }
}
