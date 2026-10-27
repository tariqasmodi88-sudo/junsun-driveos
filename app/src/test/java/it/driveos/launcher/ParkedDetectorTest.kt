package it.driveos.launcher

import it.driveos.launcher.vehicle.ParkedDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkedDetectorTest {
    @Test fun becomesParkedOnlyAfterDelay() {
        val d = ParkedDetector(stopSeconds = 10)
        d.update(0.0, 0)
        assertFalse(d.parked)          // appena fermo: ancora no
        d.update(0.5, 5_000)
        assertFalse(d.parked)
        d.update(0.0, 10_000)
        assertTrue(d.parked)           // 10 s sotto soglia: fermo
    }

    @Test fun shortStopAtTrafficLightDoesNotTrigger() {
        val d = ParkedDetector(stopSeconds = 10)
        d.update(0.0, 0)
        d.update(0.0, 6_000)
        d.update(20.0, 7_000)          // riparte prima dei 10 s
        d.update(0.0, 8_000)
        d.update(0.0, 15_000)          // solo 7 s dal nuovo stop
        assertFalse(d.parked)
    }

    @Test fun movingClearsParkedWithHysteresis() {
        val d = ParkedDetector(stopSeconds = 1)
        d.update(0.0, 0); d.update(0.0, 1_000)
        assertTrue(d.parked)
        d.update(4.0, 2_000)           // zona intermedia: resta fermo
        assertTrue(d.parked)
        d.update(8.0, 3_000)           // sopra 6 km/h: in marcia
        assertFalse(d.parked)
    }

    @Test fun reportsChange() {
        val d = ParkedDetector(stopSeconds = 0)
        assertTrue(d.update(0.0, 0))
        assertFalse(d.update(0.0, 100))
        assertTrue(d.update(30.0, 200))
    }
}
