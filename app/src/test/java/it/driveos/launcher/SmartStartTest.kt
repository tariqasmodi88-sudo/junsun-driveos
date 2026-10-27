package it.driveos.launcher

import it.driveos.launcher.automation.ConnectionTrigger
import it.driveos.launcher.automation.SmartAction
import it.driveos.launcher.automation.SmartStart
import it.driveos.launcher.automation.SmartStartConfig
import it.driveos.launcher.radio.RadioSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartStartTest {
    private val full = SmartStartConfig(
        enabled = true, playMusic = true, startNav = true, launchAndroidAuto = false,
        hasOnlineMusic = true, hasOfflineMusic = true, hasNav = true,
    )

    @Test fun disabledPlansNothing() {
        assertTrue(SmartStart.plan(full.copy(enabled = false), online = true).isEmpty())
    }

    @Test fun onlinePlaysStreaming() {
        assertEquals(listOf(SmartAction.PLAY_ONLINE, SmartAction.START_NAV), SmartStart.plan(full, online = true))
    }

    @Test fun offlinePlaysLocal() {
        assertEquals(listOf(SmartAction.PLAY_OFFLINE, SmartAction.START_NAV), SmartStart.plan(full, online = false))
    }

    @Test fun fallsBackWhenSourceMissing() {
        val onlyOnline = full.copy(hasOfflineMusic = false)
        // offline ma manca il locale → ripiega sullo streaming
        assertEquals(listOf(SmartAction.PLAY_ONLINE, SmartAction.START_NAV), SmartStart.plan(onlyOnline, online = false))
        val onlyOffline = full.copy(hasOnlineMusic = false)
        assertEquals(listOf(SmartAction.PLAY_OFFLINE, SmartAction.START_NAV), SmartStart.plan(onlyOffline, online = true))
    }

    @Test fun noNavWhenNotConfigured() {
        assertEquals(listOf(SmartAction.PLAY_ONLINE), SmartStart.plan(full.copy(hasNav = false), online = true))
        assertEquals(listOf(SmartAction.PLAY_ONLINE), SmartStart.plan(full.copy(startNav = false), online = true))
    }

    @Test fun androidAutoFirstWhenRequested() {
        val aa = full.copy(launchAndroidAuto = true, startNav = false)
        assertEquals(listOf(SmartAction.LAUNCH_ANDROID_AUTO, SmartAction.PLAY_ONLINE), SmartStart.plan(aa, online = true))
    }

    @Test fun nothingWhenProjectionActive() {
        assertTrue(SmartStart.plan(full.copy(projectionActive = true), online = true).isEmpty())
    }

    @Test fun triggerFiresOnlyOnRisingEdge() {
        val t = ConnectionTrigger()
        assertFalse(t.onState(false))
        assertTrue(t.onState(true))   // connesso: parte
        assertFalse(t.onState(true))  // ancora connesso: non ripete
        assertFalse(t.onState(false)) // disconnesso
        assertTrue(t.onState(true))   // riconnesso: riparte
    }

    @Test fun radioFrequencyParsing() {
        assertEquals(101500, RadioSignals.parseFrequencyKhz("101500"))
        assertEquals(101500, RadioSignals.parseFrequencyKhz("101.5"))
        assertEquals(101500, RadioSignals.parseFrequencyKhz("101,5"))
        assertEquals(101500, RadioSignals.parseFrequencyKhz("1015"))
        assertEquals(101500, RadioSignals.parseFrequencyKhz("10150"))
        assertNull(RadioSignals.parseFrequencyKhz("50"))
        assertNull(RadioSignals.parseFrequencyKhz("abc"))
        assertEquals(87500, RadioSignals.parseFrequencyKhz("87.5"))
    }

    @Test fun radioPlayingParsing() {
        assertEquals(true, RadioSignals.parsePlaying("1"))
        assertEquals(true, RadioSignals.parsePlaying("playing"))
        assertEquals(false, RadioSignals.parsePlaying("pause"))
        assertNull(RadioSignals.parsePlaying("boh"))
    }

    @Test fun extraNameHints() {
        assertTrue(RadioSignals.looksLikeFrequencyExtra("curFreq"))
        assertTrue(RadioSignals.looksLikeFrequencyExtra("fm_frequency"))
        assertFalse(RadioSignals.looksLikeFrequencyExtra("volume"))
        assertTrue(RadioSignals.looksLikePlayingExtra("is_play"))
    }
}
