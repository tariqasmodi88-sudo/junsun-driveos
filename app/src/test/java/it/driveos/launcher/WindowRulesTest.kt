package it.driveos.launcher

import it.driveos.launcher.vehicle.WindowKind
import it.driveos.launcher.vehicle.WindowRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowRulesTest {
    @Test fun classifiesJunsunWindows() {
        assertEquals(WindowKind.DOOR, WindowRules.classify("com.ts.MainUI", "com.ts.can.CanDoorMsgbox"))
        assertEquals(WindowKind.RADAR, WindowRules.classify("com.ts.MainUI", "com.ts.can.CanRadarActivity"))
        assertEquals(WindowKind.REVERSE, WindowRules.classify("com.ts.MainUI", "com.ts.backcar.BackcarMainActivity"))
        assertEquals(WindowKind.PROJECTION, WindowRules.classify("com.suding.speedplay", "x.Main"))
        assertEquals(WindowKind.OTHER, WindowRules.classify("com.ts.MainUI", "com.ts.main.Media.USBMainActivity"))
    }

    @Test fun bounceOnlyWhenWanted() {
        assertTrue(WindowRules.bounceHome(WindowKind.DOOR, false))
        assertFalse(WindowRules.bounceHome(WindowKind.PROJECTION, false))
        assertTrue(WindowRules.bounceHome(WindowKind.PROJECTION, true))
        assertFalse(WindowRules.bounceHome(WindowKind.OTHER, true))
    }
}
