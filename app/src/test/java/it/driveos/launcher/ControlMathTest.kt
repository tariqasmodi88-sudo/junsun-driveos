package it.driveos.launcher

import it.driveos.launcher.controls.ControlMath
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlMathTest {
    @Test fun volumePercent() {
        assertEquals(0, ControlMath.toPercent(0, 15))
        assertEquals(100, ControlMath.toPercent(15, 15))
        assertEquals(53, ControlMath.toPercent(8, 15))
        assertEquals(0, ControlMath.toPercent(5, 0))
    }

    @Test fun volumeFromPercent() {
        assertEquals(0, ControlMath.fromPercent(0, 15))
        assertEquals(15, ControlMath.fromPercent(100, 15))
        assertEquals(8, ControlMath.fromPercent(53, 15))
        assertEquals(15, ControlMath.fromPercent(150, 15))
    }

    @Test fun volumeStep() {
        assertEquals(15, ControlMath.step(15, 1, 15))
        assertEquals(0, ControlMath.step(0, -1, 15))
        assertEquals(6, ControlMath.step(5, 1, 15))
    }

    @Test fun brightness() {
        assertEquals(0f, ControlMath.brightnessFraction(0))
        assertEquals(1f, ControlMath.brightnessFraction(255))
        assertEquals(255, ControlMath.toSystemBrightness(1f))
        assertEquals(1, ControlMath.toSystemBrightness(0f))
    }
}
