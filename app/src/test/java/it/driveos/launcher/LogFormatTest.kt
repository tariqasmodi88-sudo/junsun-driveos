package it.driveos.launcher

import it.driveos.launcher.vehicle.LogFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFormatTest {
    @Test fun escapesWhenNeeded() {
        assertEquals("ok", LogFormat.escape("ok"))
        assertEquals("\"a,b\"", LogFormat.escape("a,b"))
        assertEquals("\"he said \"\"hi\"\"\"", LogFormat.escape("he said \"hi\""))
    }

    @Test fun lineFormat() {
        assertEquals("1000,2026-10-25T18:00,prop:x,0,1", LogFormat.line(1000, "2026-10-25T18:00", "prop:x", "0", "1"))
        assertEquals("5,t,prop:y,,1", LogFormat.line(5, "t", "prop:y", null, "1"))
    }

    @Test fun diffOnlyChanges() {
        val before = mapOf("a" to "1", "b" to "2", "c" to "3")
        val after = mapOf("a" to "1", "b" to "9", "d" to "4") // b cambia, c sparisce, d compare
        val lines = LogFormat.diffLines(100, "t", before, after)
        assertEquals(3, lines.size)
        assertTrue(lines.any { it.contains(",b,2,9") })
        assertTrue(lines.any { it.contains(",d,,4") })
        assertTrue(lines.any { it.contains(",c,3,") })
        assertTrue(LogFormat.diffLines(1, "t", before, before).isEmpty())
    }
}
