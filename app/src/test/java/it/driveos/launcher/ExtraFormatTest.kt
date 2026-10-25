package it.driveos.launcher

import it.driveos.launcher.vehicle.ExtraFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraFormatTest {
    @Test fun byteArrayUnsigned() {
        assertEquals("12,0,255,128", ExtraFormat.stringify(byteArrayOf(12, 0, -1, -128)))
    }

    @Test fun intAndOtherArrays() {
        assertEquals("1,2,3", ExtraFormat.stringify(intArrayOf(1, 2, 3)))
        assertEquals("1,0,1", ExtraFormat.stringify(booleanArrayOf(true, false, true)))
        assertEquals("10,20", ExtraFormat.stringify(shortArrayOf(10, 20)))
    }

    @Test fun scalarsAndNull() {
        assertEquals("42", ExtraFormat.stringify(42))
        assertEquals("ciao", ExtraFormat.stringify("ciao"))
        assertNull(ExtraFormat.stringify(null))
    }

    @Test fun longArraysTruncated() {
        val big = IntArray(500) { 9 }
        val s = ExtraFormat.stringify(big)!!
        assertTrue(s.length <= 402)
        assertTrue(s.endsWith("…"))
    }
}
