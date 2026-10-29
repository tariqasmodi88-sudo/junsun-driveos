package it.driveos.launcher

import it.driveos.launcher.vehicle.LogcatParser
import it.driveos.launcher.vehicle.WindowKind
import it.driveos.launcher.vehicle.WindowRules
import org.junit.Assert.assertEquals
import org.junit.Test

class LogcatWindowTest {
    @Test fun readsWindowNameFromCarClient() {
        val r = LogcatParser.parse("D/CarClient/Service( 2912): onReceive: name = com.ts.MainUI/com.ts.can.CanRadarActivity")
        assertEquals("win:last" to "com.ts.MainUI/com.ts.can.CanRadarActivity", r)
        assertEquals(WindowKind.RADAR, WindowRules.classify(null, r!!.second))
    }
}
