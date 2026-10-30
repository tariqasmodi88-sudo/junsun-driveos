package it.driveos.launcher

import it.driveos.launcher.vehicle.KeyAction
import it.driveos.launcher.vehicle.KeyBinding
import it.driveos.launcher.vehicle.KeyBindingCodec
import it.driveos.launcher.vehicle.KeyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMapTest {
    @Test fun codecRoundTripAndReplace() {
        val a = KeyBinding("can:C30/01", "02", KeyAction.VOL_UP)
        val b = KeyBinding("can:C30/01", "04", KeyAction.VOL_DOWN)
        val list = listOf(a, b)
        assertEquals(list, KeyBindingCodec.decode(KeyBindingCodec.encode(list)))
        val replaced = KeyBindingCodec.set(list, KeyBinding("can:C31/00", "01", KeyAction.VOL_UP))
        assertEquals(2, replaced.size)
        assertEquals("can:C31/00", replaced.first { it.action == KeyAction.VOL_UP }.signal)
        assertEquals(1, KeyBindingCodec.clear(replaced, KeyAction.VOL_UP).size)
    }

    @Test fun stateSignalsAreNotKeys() {
        assertFalse(KeyMapper.isKeyCandidate("can:door_fl"))
        assertFalse(KeyMapper.isKeyCandidate("can:C7d/05"))
        assertFalse(KeyMapper.isKeyCandidate("can:C22/01"))
        assertFalse(KeyMapper.isKeyCandidate("can:S41"))
        assertFalse(KeyMapper.isKeyCandidate("prop:forfan.want2start"))
        assertTrue(KeyMapper.isKeyCandidate("can:C30/01"))
    }
}
