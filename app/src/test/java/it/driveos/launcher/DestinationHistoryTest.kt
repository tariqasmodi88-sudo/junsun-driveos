package it.driveos.launcher

import it.driveos.launcher.nav.DestinationHistory
import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationHistoryTest {
    @Test fun newestFirstWithoutDuplicates() {
        var list = emptyList<String>()
        list = DestinationHistory.add(list, "Milano")
        list = DestinationHistory.add(list, "Roma")
        list = DestinationHistory.add(list, "milano ")
        assertEquals(listOf("milano", "Roma"), list)
    }

    @Test fun keepsAtMostMax() {
        var list = emptyList<String>()
        repeat(20) { list = DestinationHistory.add(list, "Luogo $it") }
        assertEquals(DestinationHistory.MAX, list.size)
        assertEquals("Luogo 19", list.first())
    }

    @Test fun ignoresBlankAndRoundTrips() {
        val list = DestinationHistory.add(listOf("A"), "   ")
        assertEquals(listOf("A"), list)
        val l2 = listOf("Via Roma 1, Torino", "Duomo")
        assertEquals(l2, DestinationHistory.decode(DestinationHistory.encode(l2)))
    }

    @Test fun removeIgnoresCase() {
        assertEquals(listOf("B"), DestinationHistory.remove(listOf("a", "B"), "A"))
    }
}
