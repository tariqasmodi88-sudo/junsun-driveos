package it.driveos.launcher

import it.driveos.launcher.vehicle.RdsTracker
import it.driveos.launcher.vehicle.RdsTracker.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RdsTrackerTest {
    private var t = 0L
    private fun tracker() = RdsTracker(now = { t }, debounceMs = 1000, timeoutMs = 5000)

    @Test fun tuneResetsName() {
        val r = tracker()
        r.onTune(102500); r.onPs("RTL102.5"); t += 1500
        assertEquals(102500 to "RTL102.5", r.tick())
        r.onTune(101200)
        assertEquals(Status.SEARCHING, r.state.status)
        assertNull(r.state.name)
    }

    @Test fun debounceAndInvalid() {
        val r = tracker()
        r.onTune(99200)
        r.onPs("        "); r.onPs("* RDS *"); r.onPs("--------")
        t += 500; assertNull(r.tick())
        r.onPs(" DEEJAY ")
        t += 500; assertNull(r.tick())
        t += 600; assertEquals(99200 to "DEEJAY", r.tick())
    }

    @Test fun dynamicPsKeepsFirst() {
        val r = tracker()
        r.onTune(101700); r.onPs(" MONEL E"); r.onPs("BELLLA E")
        t += 1100
        assertEquals(101700 to "MONEL E", r.tick())
    }

    @Test fun timeoutFallsBackToFrequency() {
        val r = tracker()
        r.onTune(103000); t += 5100; r.tick()
        assertEquals(Status.NO_RDS, r.state.status)
        assertNull(r.state.name)
    }
}
