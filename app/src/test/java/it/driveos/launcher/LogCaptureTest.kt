package it.driveos.launcher

import it.driveos.launcher.vehicle.LogCapture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LogCaptureTest {
    @Test fun keepsVehicleLinesAndSkipsNoise() {
        val c = LogCapture { 1000L }
        assertNotNull(c.accept("D/CanDoorMsgbox( 1234): SetShowFlg = true"))
        assertNull(c.accept("D/SurfaceFlinger( 99): vsync 16.6ms"))
        assertNotNull(c.accept("I/ActivityManager( 55): START u0 {cmp=com.suding.speedplay/.ui.MainActivity}"))
    }

    @Test fun dedupesAndRedactsLongNumbers() {
        val c = LogCapture { 1000L }
        val e = c.accept("D/HfpClient( 7): call from 3331234567 connected")
        assertEquals("call from # connected", e?.message)
        assertNull(c.accept("D/HfpClient( 7): call from 3331234567 connected"))
    }

    @Test fun rateLimitedPerSecond() {
        var t = 0L
        val c = LogCapture { t }
        val accepted = (1..20).count { c.accept("D/CanFunc( 1): tick $it") != null }
        assertEquals(6, accepted)
    }

    @Test fun ignoresSelinuxNoise() {
        val c = LogCapture { 1000L }
        assertNull(c.accept("I/com.ts.MainUI( 5): type=1400 audit(0.0:3085): avc: denied { ioctl } for path=/proc/forfan_touch_ctrl"))
    }

    @Test fun doorEventOpensBurstForUnknownTags() {
        var t = 0L
        val c = LogCapture { t }
        assertNull(c.accept("V/SomethingNew( 9): mystery before"))
        c.accept("D/CanDoorMsgbox( 1): SetShowFlg = true")
        t = 500
        assertEquals("val=5", c.accept("V/SomethingNew( 9): val=5")?.message)
        t = 4000
        assertNull(c.accept("V/SomethingNew( 9): val=6"))
    }
}
