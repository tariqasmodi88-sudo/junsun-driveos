package it.driveos.launcher

import it.driveos.launcher.system.DriveLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveLockTest {
    @Test fun parkedNeverLocks() {
        var t = 0L; val l = DriveLock { t }
        repeat(30) { assertTrue(l.onTouch()); t += 100 }
    }

    @Test fun movingLocksAfterSixQuickTaps() {
        var t = 1000L; val l = DriveLock { t }; l.moving = true
        repeat(6) { assertTrue(l.onTouch()); t += 200 }
        assertFalse(l.onTouch())
        assertTrue(l.isLocked())
        t += 10_100
        assertTrue(l.onTouch())
    }

    @Test fun slowTapsRefill() {
        var t = 1000L; val l = DriveLock { t }; l.moving = true
        repeat(20) { assertTrue(l.onTouch()); t += 3000 }
    }
}
