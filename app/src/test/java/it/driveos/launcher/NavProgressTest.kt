package it.driveos.launcher

import it.driveos.launcher.nav.NavProgress
import it.driveos.launcher.nav.NavProgress.Turn
import it.driveos.launcher.nav.NavRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavProgressTest {
    // linea dritta verso nord (~111 m per 0.001°), poi a destra verso est
    private val pts = (0..10).map { 45.0 + it * 0.001 to 9.0 } + (1..5).map { 45.010 to 9.0 + it * 0.001 }
    private val route = NavRoute.Route(
        "Via Roma 1", 45.010, 9.005, pts, 1500.0, 300.0,
        listOf(
            NavProgress.Step(10, "turn", "right", "Via Roma"),
            NavProgress.Step(15, "arrive", "", ""),
        ),
    )

    @Test fun prossima_svolta_e_distanza() {
        val s = NavProgress.compute(route, 45.005 to 9.0)
        assertNotNull(s); s!!
        assertEquals(Turn.RIGHT, s.turn)
        assertEquals("Via Roma", s.street)
        assertEquals("Gira a destra", s.instruction)
        assertEquals(556.0, s.distanceM, 15.0)
        assertEquals(5, s.traveledIndex)
    }

    @Test fun dopo_la_svolta_arrivo() {
        val s = NavProgress.compute(route, 45.010 to 9.002)!!
        assertEquals(Turn.ARRIVE, s.turn)
        assertEquals("Via Roma 1", s.street)
        assertTrue(s.remainingM < 300)
    }

    @Test fun manovra_ravvicinata_mostra_poi() {
        val close = route.copy(steps = listOf(NavProgress.Step(10, "turn", "right", "A"), NavProgress.Step(11, "turn", "left", "B")))
        assertEquals(Turn.LEFT, NavProgress.compute(close, 45.002 to 9.0)!!.then)
        assertNull(NavProgress.compute(route, 45.002 to 9.0)!!.then)
    }

    @Test fun testi_italiani() {
        assertEquals("Alla rotonda prendi la 2ª uscita", NavProgress.instruction("roundabout", "right", 2))
        assertEquals("Tieni la sinistra", NavProgress.instruction("turn", "slight left", null))
        assertEquals("Al bivio tieni la destra", NavProgress.instruction("fork", "slight right", null))
        assertFalse(NavProgress.isAnnounced("new name", "straight"))
        assertTrue(NavProgress.isAnnounced("continue", "left"))
        assertFalse(NavProgress.isAnnounced("depart", "right"))
    }

    @Test fun formati() {
        assertEquals("Ora", NavProgress.formatDistance(8.0))
        assertEquals("60 m", NavProgress.formatDistance(57.0))
        assertEquals("350 m", NavProgress.formatDistance(342.0))
        assertEquals("1,2 km", NavProgress.formatDistance(1234.0))
        assertEquals("12 min", NavProgress.formatDuration(720.0))
        assertEquals("1 h 05", NavProgress.formatDuration(3900.0))
    }
}
