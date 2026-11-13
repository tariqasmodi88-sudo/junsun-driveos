package it.driveos.launcher

import it.driveos.launcher.vehicle.CanDecoder
import it.driveos.launcher.vehicle.CanQuery
import it.driveos.launcher.vehicle.CanSignalFilter
import it.driveos.launcher.vehicle.CanStreamParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanSerialTest {
    private fun bytes(s: String) = s.trim().split(" ").map { it.toInt(16).toByte() }.toByteArray()
    private fun hex(b: ByteArray) = b.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }
    /** Messaggio CAN completo e con le somme di controllo giuste: es. can(0x22, 3, 1, 1, 1). */
    private fun can(cmd: Int, vararg data: Int) = hex(CanQuery.frame(CanQuery.raise(cmd, *data)))

    // frame reali del giro del 30/10 (porta ant. sx aperta, ant. sx + baule, retro, batteria)
    private val status = "23 53 09 41 04 00 00 00 00 00 00 00 1f"
    private val door = can(0x24, 0x04, 0x01)
    private val trunkFl = can(0x24, 0x04, 0x11)
    private val closed = can(0x24, 0x04, 0x00)
    private val hbrake = can(0x41, 0x01, 0xa0)
    private val batt = can(0x7d, 0x0b, 0x14, 0x37)
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
        assertEquals("1", decode(can(0x41, 0x01, 0x80))[CanDecoder.HANDBRAKE]) // bit a 0 = tirato
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
        assertEquals(2, (a + b).count { it.type != 'X' })
    }

    @Test fun lightsAndBelt() {
        val m = decode(can(0x7d, 0x01, 0xe0), can(0x7d, 0x06, 0x3d))
        assertEquals("1", m[CanDecoder.LIGHTS]); assertEquals("1", m[CanDecoder.LOWBEAM])
        assertEquals("1", m[CanDecoder.BELT])
        val blink = decode(can(0x7d, 0x01, 0xd0))
        assertEquals("1", blink[CanDecoder.LIGHTS]); assertEquals(null, blink["can:C7d/01"])
        assertEquals("0", decode(can(0x7d, 0x01, 0x00))[CanDecoder.LIGHTS])
    }

    @Test fun parkingSensors() {
        val m = decode(can(0x22, 3, 1, 1, 1), can(0x23, 0, 0, 0, 0))
        assertEquals("3 1 1 1", m[CanDecoder.REAR_SENSORS])
        assertEquals(listOf(3, 3, 3, 3), CanDecoder.sensorLevels(m, CanDecoder.REAR_SENSORS))
        assertEquals(listOf(0, 3, 3, 3), CanDecoder.sensorLevels(mapOf(CanDecoder.REAR_SENSORS to "0 3 3 2"), CanDecoder.REAR_SENSORS))
        assertEquals(listOf(0, 0, 0, 0), CanDecoder.sensorLevels(mapOf(CanDecoder.REAR_SENSORS to "0 10 10 0"), CanDecoder.REAR_SENSORS))
        assertEquals(listOf(0, 2, 1, 3), CanDecoder.sensorLevels(mapOf(CanDecoder.REAR_SENSORS to "12 5 8 2"), CanDecoder.REAR_SENSORS))
        assertEquals(listOf(0, 0, 0, 0), CanDecoder.sensorLevels(m, CanDecoder.FRONT_SENSORS))
    }

    @Test fun realFramesHaveTheChecksumsWeCompute() {
        // catture del 30/10 e del 07/11: le somme calcolate coincidono con quelle vere
        assertEquals("23 43 06 2e 24 02 04 01 d4 9c", can(0x24, 0x04, 0x01))
        val p = CanStreamParser()
        val real = "23 4b 03 10 05 01 5c " + status + " 23 43 08 2e 22 04 03 01 01 01 d3 92"
        assertEquals(listOf('K', 'S', 'C'), p.feed(bytes(real)).map { it.type })
        assertEquals(0L, p.rejected)
    }

    @Test fun brokenFramesAreDroppedNotDecoded() {
        // messaggio "porta aperta" con un byte cambiato: prima diventava una porta aperta inesistente
        val p = CanStreamParser()
        val bad = bytes(door).also { it[7] = 0x1f }
        assertEquals(0, p.feed(bad).count { it.type != 'X' })
        assertEquals(1L, p.rejected)
        // somma esterna giusta ma interna (Raise) sbagliata: scartato lo stesso
        val inner = bytes("23 43 06 2e 24 02 04 01 d5 00").also { b -> var x = 0; for (i in 1..8) x = x xor (b[i].toInt() and 0xFF); b[9] = x.toByte() }
        assertEquals(0, CanStreamParser().feed(inner).count { it.type != 'X' })
    }

    @Test fun frameCutByTheRadioDoesNotEatTheNextOnes() {
        // la radio si e' presa la coda di un messaggio: a DriveOS arriva solo la testa, poi messaggi interi
        val p = CanStreamParser()
        val head = bytes(door).copyOfRange(0, 5)
        val got = p.feed(head) + p.feed(bytes(status)) + p.feed(bytes(trunkFl)) + p.feed(bytes(closed))
        val good = got.filter { it.type != 'X' }
        assertEquals(listOf('S', 'C', 'C'), good.map { it.type })
        assertEquals(emptyList<String>(), CanDecoder.openDoors(good.flatMap { CanDecoder.decode(it).entries }.associate { it.key to it.value }))
    }

    @Test fun parkPilotStatus() {
        assertEquals("1", decode(can(0x25, 0x02, 0x00))[CanDecoder.PARK])
        assertEquals("0", decode(can(0x25, 0x00, 0x00))[CanDecoder.PARK])
    }

    private fun CanSignalFilter.feed(now: Long, vararg frames: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val p = CanStreamParser()
        for (f in frames) p.feed(bytes(f)).forEach { apply(CanDecoder.decode(it), now, out) }
        return out
    }

    @Test fun sensorsClearWhenParkPilotTurnsOff() {
        // sequenza vera (cattura 30/10, s1_16): spento -> "00.." -> "01 01 01 01" -> silenzio
        val f = CanSignalFilter()
        assertEquals("1 1 1 1", f.feed(0, can(0x25, 0x02, 0x00), can(0x22, 1, 1, 1, 1))[CanDecoder.REAR_SENSORS])
        val off = f.feed(1000, can(0x25, 0x00, 0x00))
        assertEquals("0 0 0 0", off[CanDecoder.REAR_SENSORS]); assertEquals("0", off[CanDecoder.PARK])
        // la coda dopo lo spegnimento non riaccende i sensori
        val tail = f.feed(1300, can(0x22, 0, 0, 0, 0), can(0x23, 0, 0, 0, 0), can(0x22, 1, 1, 1, 1), can(0x23, 1, 1, 1, 1))
        assertNull(tail[CanDecoder.REAR_SENSORS]); assertNull(tail[CanDecoder.FRONT_SENSORS])
        // riacceso: tornano subito
        assertEquals("3 1 1 1", f.feed(1400, can(0x25, 0x02, 0x00), can(0x22, 3, 1, 1, 1))[CanDecoder.REAR_SENSORS])
        // anche senza il messaggio di accensione (preso dalla radio), passato il silenzio i sensori valgono
        val g = CanSignalFilter()
        g.feed(0, can(0x22, 2, 1, 1, 1)); g.feed(100, can(0x25, 0x00, 0x00))
        assertEquals("4 1 1 1", g.feed(100 + CanSignalFilter.MUTE_MS, can(0x22, 4, 1, 1, 1))[CanDecoder.REAR_SENSORS])
    }

    @Test fun sensorsExpireAndOnlyChangesPass() {
        val f = CanSignalFilter()
        assertEquals("2 1 1 1", f.feed(0, can(0x22, 2, 1, 1, 1))[CanDecoder.REAR_SENSORS])
        assertNull(f.feed(300, can(0x22, 2, 1, 1, 1))[CanDecoder.REAR_SENSORS]) // uguale: non ripubblicato
        assertEquals(emptyMap<String, String>(), f.expire(300 + CanSignalFilter.EXPIRE_MS))
        assertEquals("0 0 0 0", f.expire(301 + CanSignalFilter.EXPIRE_MS)[CanDecoder.REAR_SENSORS])
        // i tasti sono eventi: passano sempre, anche identici
        assertEquals("05", f.feed(5000, "23 4b 03 10 05 01 5c")[CanDecoder.KEY])
        assertEquals("05", f.feed(5100, "23 4b 03 10 05 01 5c")[CanDecoder.KEY])
        // lettura ferma: sensori azzerati, poi tutto ripubblicato
        f.feed(6000, can(0x22, 2, 1, 1, 1), door)
        assertEquals(mapOf(CanDecoder.REAR_SENSORS to "0 0 0 0"), f.reset())
        assertEquals("1", f.feed(7000, door)[CanDecoder.DOOR_FL])
    }

    @Test fun unknownFramesAreLoggedRaw() {
        val m = decode(can(0x22, 2, 1, 1, 1))
        assertEquals("01 01 01", m["can:C22/02"])
    }
}

class CanSerialJunkTest {
    @org.junit.Test fun bytesFuoriProtocolloDiventanoEventoX() {
        val p = CanStreamParser()
        val key = byteArrayOf(0x55, 0x10, 0x07, 0x00)
        val frames = p.feed(key, key.size)
        org.junit.Assert.assertEquals(1, frames.size)
        org.junit.Assert.assertEquals('X', frames[0].type)
        val sig = CanDecoder.decode(frames[0])
        org.junit.Assert.assertEquals("55 10 07 00", sig[CanDecoder.RAW_BYTES])
    }
    @org.junit.Test fun soloZeriNonSonoUnTasto() {
        val frames = CanStreamParser().feed(ByteArray(4), 4)
        org.junit.Assert.assertTrue(frames.isEmpty())
    }
}

class CanQueryTest {
    private fun hex(b: ByteArray) = b.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    @org.junit.Test fun engineQueryBytes() {
        // Raise: 2E 90 02 41 02 + NOT(90+02+41+02)=2A ; esterno: 23 43 06 … XOR
        org.junit.Assert.assertEquals("2E 90 02 41 02 2A", hex(it.driveos.launcher.vehicle.CanQuery.raise(0x90, 0x41, 0x02)))
        org.junit.Assert.assertEquals("23 43 06 2E 90 02 41 02 2A 90", hex(it.driveos.launcher.vehicle.CanQuery.ENGINE))
    }

    @org.junit.Test fun engineDataDecoded() {
        // 0x41/02: giri 850, vel. 0, 14.20 V, 12.5 °C, autonomia 640 km, 38 l
        val d = byteArrayOf(0x02, 0x03, 0x52, 0x00, 0x00, 0x05, 0x8C.toByte(), 0x00, 0x7D, 0x00, 0x02, 0x80.toByte(), 38)
        val p = byteArrayOf(0x2E, 0x41, d.size.toByte()) + d
        val out = it.driveos.launcher.vehicle.CanDecoder.decode(it.driveos.launcher.vehicle.CanFrame('C', p))
        org.junit.Assert.assertEquals("850", out["can:rpm"])
        org.junit.Assert.assertEquals("14.2", out["can:voltage"])
        org.junit.Assert.assertEquals("12.5", out["can:out_temp"])
        org.junit.Assert.assertEquals("640", out["can:odometer_km"])
        org.junit.Assert.assertEquals("38", out["can:fuel_l"])
    }
}
