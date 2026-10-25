package it.driveos.launcher

import it.driveos.launcher.dashboard.CardType
import it.driveos.launcher.dashboard.DashboardLayout
import it.driveos.launcher.dashboard.GRID_COLUMNS
import it.driveos.launcher.dashboard.GRID_ROWS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLayoutTest {
    @Test fun atLeastFiveLayouts() {
        assertTrue(DashboardLayout.entries.size >= 5)
    }

    @Test fun everyLayoutHasEachCardExactlyOnce() {
        DashboardLayout.entries.forEach { layout ->
            assertEquals(CardType.entries.toSet(), layout.slots.map { it.card }.toSet())
            assertEquals(CardType.entries.size, layout.slots.size)
        }
    }

    @Test fun slotsStayInsideTheGridAndDoNotOverlap() {
        DashboardLayout.entries.forEach { layout ->
            val cells = mutableSetOf<Pair<Int, Int>>()
            layout.slots.forEach { s ->
                assertTrue("${layout.id} ${s.card} fuori griglia", s.x >= 0 && s.y >= 0 && s.w > 0 && s.h > 0)
                assertTrue("${layout.id} ${s.card} fuori griglia", s.x + s.w <= GRID_COLUMNS && s.y + s.h <= GRID_ROWS)
                for (x in s.x until s.x + s.w) for (y in s.y until s.y + s.h) {
                    assertTrue("${layout.id}: sovrapposizione in $x,$y", cells.add(x to y))
                }
            }
            assertEquals("${layout.id} lascia celle vuote", GRID_COLUMNS * GRID_ROWS, cells.size)
        }
    }

    @Test fun idsAreUniqueAndRoundTrip() {
        val ids = DashboardLayout.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        DashboardLayout.entries.forEach { assertEquals(it, DashboardLayout.fromId(it.id)) }
        assertEquals(DashboardLayout.DEFAULT, DashboardLayout.fromId("sconosciuto"))
        assertEquals(DashboardLayout.DEFAULT, DashboardLayout.fromId(null))
    }
}
