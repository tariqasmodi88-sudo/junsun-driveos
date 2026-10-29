package it.driveos.launcher

import it.driveos.launcher.vehicle.LogcatParser
import it.driveos.launcher.vehicle.RadioFreq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogcatParserTest {
    @Test fun doorsFromRealLines() {
        assertEquals(LogcatParser.DOOR to "aperta",
            LogcatParser.parse("10-27 19:50:09.094 2163 2163 D CanDoorMsgbox: SetShowFlg = true"))
        assertEquals(LogcatParser.DOOR to "chiuse",
            LogcatParser.parse("10-27 19:50:11.995 2163 2163 D CanDoorMsgbox: SetShowFlg = false"))
    }

    @Test fun lightsFromRealLine() {
        assertEquals(LogcatParser.LIGHTS to "off",
            LogcatParser.parse("10-27 19:50:34.697 2163 2163 I MainUI : BROADCAST_ADAS_LIGHT_STATE       off"))
        assertEquals(LogcatParser.LIGHTS to "on",
            LogcatParser.parse("MainUI : BROADCAST_ADAS_LIGHT_STATE       on"))
    }

    @Test fun parkingAndReverse() {
        assertEquals(LogcatParser.PARKING to "1",
            LogcatParser.parse("2163 2163 D CanFunc : CanFunc.mbRadarUIUpdate = true"))
        assertEquals(LogcatParser.REVERSE to "1",
            LogcatParser.parse("START u0 {cmp=com.ts.MainUI/com.ts.backcar.BackcarMainActivity}"))
    }

    @Test fun reverseOnAndOffFromCarEventHandler() {
        // righe reali dal registro del 29/10/2026
        assertEquals(LogcatParser.REVERSE to "1", LogcatParser.parse(
            "10-29 20:41:04.446   542   871 I SmartPlatformService/CarEventHandler: parseMsg(), car status: reverse"))
        assertEquals(LogcatParser.REVERSE to "0", LogcatParser.parse(
            "10-29 20:41:06.899   542   871 I SmartPlatformService/CarEventHandler: parseMsg(), car status: normal"))
    }

    @Test fun ignoresNoise() {
        assertNull(LogcatParser.parse("10-27 19:50:01 D MPE_MA : mnl2mpe_hdlr: mpe recv"))
        assertNull(LogcatParser.parse("libPowerHal: cpu_ctrl set freq: 1700000"))  // freq ma senza contesto radio
    }

    @Test fun radioFreqOnlyInRadioContext() {
        assertEquals(LogcatParser.RADIO_FREQ to "101500",
            LogcatParser.parse("MainUI : radio set FM 101.5"))
        assertEquals(LogcatParser.RADIO_FREQ to "101500",
            LogcatParser.parse("com.ts.main.radio freq=101500"))
        assertNull(LogcatParser.parse("cpu freq 101500")) // "cpu" non è contesto radio → ignorato
    }

    @Test fun radioFreqNormalization() {
        assertEquals(101500, RadioFreq.toKhz("101.5"))
        assertEquals(101500, RadioFreq.toKhz("10150"))
        assertEquals(101500, RadioFreq.toKhz("101500"))
        assertNull(RadioFreq.toKhz("500"))
    }
}
