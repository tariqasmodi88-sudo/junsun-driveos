package it.driveos.launcher

import it.driveos.launcher.ui.theme.AutoSource
import it.driveos.launcher.ui.theme.AutoThemeResolver
import it.driveos.launcher.ui.theme.SunTimes
import it.driveos.launcher.vehicle.ActionFilter
import it.driveos.launcher.vehicle.BindingCodec
import it.driveos.launcher.vehicle.CandidateFinder
import it.driveos.launcher.vehicle.GetpropParser
import it.driveos.launcher.vehicle.LightsResolver
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalIds
import it.driveos.launcher.vehicle.SignalRole
import it.driveos.launcher.vehicle.SnapshotDiff
import it.driveos.launcher.vehicle.StringScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class VehicleLogicTest {

    @Test
    fun scannerFindsAsciiStringsInBinary() {
        val bytes = byteArrayOf(0, 1, 2) + "(com.ts.can.ACTION_ILL".toByteArray() + byteArrayOf(0, 5) + "xy".toByteArray()
        val found = StringScanner.scan(bytes)
        assertTrue(found.contains("(com.ts.can.ACTION_ILL"))
        assertFalse(found.any { it == "xy" })
    }

    @Test
    fun scannerFindsUtf16StringsAndWorksInChunks() {
        val utf16 = "com.junsun.radio.FREQ_CHANGED".toByteArray(Charsets.UTF_16LE)
        val data = byteArrayOf(0x1D, 0x00) + utf16 + byteArrayOf(0, 0, 7)
        val scanner = StringScanner()
        // a pezzi di 5 byte: le stringhe non devono spezzarsi tra un blocco e l'altro
        data.toList().chunked(5).forEach { scanner.feed(it.toByteArray()) }
        assertTrue(scanner.finish().contains("com.junsun.radio.FREQ_CHANGED"))
    }

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
    fun candidateFinderRanksConsistentSignals() {
        val on1 = mapOf("prop:sys.ill" to "1", "set:system/screen_brightness" to "60", "prop:x.counter" to "5", "bc:a|#" to "3")
        val off1 = mapOf("prop:sys.ill" to "0", "set:system/screen_brightness" to "200", "prop:x.counter" to "9", "bc:a|#" to "4")
        val on2 = mapOf("prop:sys.ill" to "1", "set:system/screen_brightness" to "70")
        val off2 = mapOf("prop:sys.ill" to "0", "set:system/screen_brightness" to "200")
        val c = CandidateFinder.find(listOf(on1, on2), listOf(off1, off2))
        // la luminosita' non e' coerente tra i due "accesi" (60 vs 70): scartata
        assertEquals(listOf("prop:sys.ill"), c.map { it.id })
        assertEquals("1", c[0].onValue)
        assertEquals("0", c[0].offValue)
        assertTrue(CandidateFinder.find(listOf(on1), emptyList()).isEmpty())
    }

    @Test
    fun noisySignalsRankLower() {
        val on = listOf(mapOf("prop:a" to "1", "prop:b" to "1"))
        val off = listOf(mapOf("prop:a" to "0", "prop:b" to "0"))
        val c = CandidateFinder.find(on, off, mapOf("prop:a" to 2, "prop:b" to 40))
        assertEquals("prop:a", c.first().id)
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
