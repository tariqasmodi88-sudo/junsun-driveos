package it.driveos.launcher

import it.driveos.launcher.vehicle.CanDecoder
import it.driveos.launcher.vehicle.CanStreamParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanSerialTest {
    private fun bytes(s: String) = s.trim().split(" ").map { it.toInt(16).toByte() }.toByteArray()

    // frame reali del giro del 30/10 (porta ant. sx aperta, ant. sx + baule, retro, batteria)
    private val status = "23 53 09 41 04 00 00 00 00 00 00 00 1f"
    private val door = "23 43 06 2e 24 02 04 01 d4 9c"
    private val trunkFl = "23 43 06 2e 24 02 04 11 c4 9c"
    private val closed = "23 43 06 2e 24 02 04 00 d5 9c"
    private val hbrake = "23 43 06 2e 41 02 01 a0 1b 92"
    private val batt = "23 43 07 2e 7d 03 0b 14 37 29 15"
    private val rev = "23 53 09 41 05 00 00 00 00 00 00 00 1e"

    private fun decode(vararg frames: String): Map<String, String> {
        val p = CanStreamParser()
        val all = LinkedHashMap<String, String>()
        for (f in frames) { val b = bytes(f); p.feed(b).forEach { all.putAll(CanDecoder.decode(it)) } }
        return all
    }

    @Test fun doorFrontLeftOnly() {
        val m = decode(status, door)
        assertEquals("1", m[CanDecoder.DOOR_FL]); assertEquals("0", m[CanDecoder.DOOR_FR])
        assertEquals("0", m[CanDecoder.TRUNK])
        assertEquals(listOf("ant. sx"), CanDecoder.openDoors(m))
    }

    @Test fun trunkAndDoorAndClosed() {
        assertEquals(listOf("ant. sx", "baule"), CanDecoder.openDoors(decode(trunkFl)))
        assertEquals(emptyList<String>(), CanDecoder.openDoors(decode(closed)))
        assertNull(CanDecoder.openDoors(decode(status)))
    }

    @Test fun handbrakeReverseBattery() {
        val m = decode(hbrake, rev, batt)
        assertEquals("0", m[CanDecoder.HANDBRAKE]) // bit 0x20 a 1 = freno giu'
        assertEquals("1", decode("23 43 06 2e 41 02 01 80 3b 92")[CanDecoder.HANDBRAKE]) // bit a 0 = tirato
        assertEquals("1", m[CanDecoder.REVERSE])
        assertEquals("14.1", m[CanDecoder.BATTERY_V])
        assertEquals("0", decode(status)[CanDecoder.REVERSE])
    }

    @Test fun resyncAndSplitFrames() {
        val p = CanStreamParser()
        val all = bytes("ff 00 " + door + " " + status)
        val half = all.size / 2
        val a = p.feed(all.copyOfRange(0, half))
        val b = p.feed(all.copyOfRange(half, all.size))
        assertEquals(2, a.size + b.size)
    }

    @Test fun lightsAndBelt() {
        val m = decode("23 43 06 2e 7d 02 01 e0 00 00", "23 43 06 2e 7d 02 06 3d 00 00")
        assertEquals("1", m[CanDecoder.LIGHTS]); assertEquals("1", m[CanDecoder.LOWBEAM])
        assertEquals("1", m[CanDecoder.BELT])
        val blink = decode("23 43 06 2e 7d 02 01 d0 00 00")
        assertEquals("1", blink[CanDecoder.LIGHTS]); assertEquals(null, blink["can:C7d/01"])
        assertEquals("0", decode("23 43 06 2e 7d 02 01 00 00 00")[CanDecoder.LIGHTS])
    }

    @Test fun parkingSensors() {
        val m = decode("23 43 08 2e 22 04 03 01 01 01 aa 55", "23 43 08 2e 23 04 00 00 00 00 aa 55")
        assertEquals("3 1 1 1", m[CanDecoder.REAR_SENSORS])
        assertEquals(listOf(2, 0, 0, 0), CanDecoder.sensorLevels(m, CanDecoder.REAR_SENSORS))
        assertEquals(listOf(0, 0, 0, 0), CanDecoder.sensorLevels(m, CanDecoder.FRONT_SENSORS))
    }

    @Test fun unknownFramesAreLoggedRaw() {
        val m = decode("23 43 08 2e 22 04 02 01 01 01 aa 55")
        assertEquals("01 01 01", m["can:C22/02"])
    }
}
