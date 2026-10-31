package it.driveos.launcher

import it.driveos.launcher.radio.RadioBand
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioPresetCodec
import it.driveos.launcher.radio.RadioPresetLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RadioPresetTest {
    @Test fun parseFm() {
        assertEquals(101500, RadioPresetLogic.parseFm("101.5"))
        assertEquals(101500, RadioPresetLogic.parseFm("101,5"))
        assertEquals(87500, RadioPresetLogic.parseFm("87.5"))
        assertEquals(100000, RadioPresetLogic.parseFm("100"))
        assertNull(RadioPresetLogic.parseFm("200"))
        assertNull(RadioPresetLogic.parseFm("abc"))
        assertNull(RadioPresetLogic.parseFm("50"))
    }

    @Test fun display() {
        assertEquals("101.5", RadioPreset(101500, "Radio Deejay").displayFrequency())
        assertEquals("100", RadioPreset(100000, "x").displayFrequency())
    }

    @Test fun addRemoveNormalize() {
        var l = RadioPresetLogic.add(emptyList(), RadioPreset(101500, "A"))
        l = RadioPresetLogic.add(l, RadioPreset(87500, "B"))
        l = RadioPresetLogic.add(l, RadioPreset(101500, "A2")) // sostituisce la stessa frequenza
        assertEquals(2, l.size)
        assertEquals(101500, l.first().khz) // ordine di salvataggio (P1, P2…)
        assertEquals("A2", l.first { it.khz == 101500 }.name)
        assertEquals(1, RadioPresetLogic.remove(l, RadioBand.FM, 87500).size)
    }

    @Test fun codecRoundTrip() {
        val list = listOf(RadioPreset(101500, "Deejay"), RadioPreset(999, "Canale 1", RadioBand.DAB))
        assertEquals(list, RadioPresetCodec.decode(RadioPresetCodec.encode(list)))
        assertEquals(emptyList<RadioPreset>(), RadioPresetCodec.decode(null))
    }
}
