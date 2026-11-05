package it.driveos.launcher

import it.driveos.launcher.system.WakeGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WakeGuardTest {
    private val main = "com.ts.MainUI"
    private val radio = "com.ts.main.radio.RadioMainActivity"

    @Before fun reset() {
        WakeGuard.keepHome = true
        WakeGuard.arm(1_000)
    }

    @Test fun bouncesJunsunAppRightAfterWake() {
        assertTrue(WakeGuard.shouldBounce(main, radio, 5_000, homeInFront = false, reverse = false))
        assertTrue(WakeGuard.shouldBounce(main, null, 20_000, homeInFront = false, reverse = false))
    }

    @Test fun stopsAfterWindow() {
        assertFalse(WakeGuard.shouldBounce(main, radio, 1_000 + WakeGuard.WINDOW_MS + 1, false, false))
    }

    @Test fun neverOverReverseRadarSettingsOrProjection() {
        assertFalse(WakeGuard.shouldBounce(main, radio, 5_000, false, reverse = true))
        assertFalse(WakeGuard.shouldBounce(main, "com.ts.backcar.BackcarMainActivity", 5_000, false, false))
        assertFalse(WakeGuard.shouldBounce(main, "com.ts.can.CanRadarActivity", 5_000, false, false))
        assertFalse(WakeGuard.shouldBounce(main, "com.ts.main.factory.FsBackCarSetActivity", 5_000, false, false))
        assertFalse(WakeGuard.shouldBounce("com.suding.speedplay", "x.Main", 5_000, false, false))
        assertFalse(WakeGuard.shouldBounce("com.google.android.apps.maps", "x.Main", 5_000, false, false))
    }

    @Test fun nothingWhenHomeAlreadyInFront() {
        assertFalse(WakeGuard.shouldBounce(main, radio, 5_000, homeInFront = true, reverse = false))
    }

    @Test fun userTouchStopsIt() {
        WakeGuard.userActed()
        assertFalse(WakeGuard.shouldBounce(main, radio, 5_000, false, false))
        WakeGuard.arm(50_000)
        assertTrue(WakeGuard.shouldBounce(main, radio, 52_000, false, false))
    }

    @Test fun limitedNumberOfBounces() {
        repeat(WakeGuard.MAX_BOUNCES) { WakeGuard.bounced() }
        assertFalse(WakeGuard.shouldBounce(main, radio, 5_000, false, false))
    }

    @Test fun respectsKeepHomeSetting() {
        WakeGuard.keepHome = false
        assertFalse(WakeGuard.shouldBounce(main, radio, 5_000, false, false))
    }
}
