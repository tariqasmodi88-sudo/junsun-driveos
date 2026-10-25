package it.driveos.launcher

import it.driveos.launcher.apps.AppSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {
    @Test fun emptyQueryMatchesEverything() {
        assertTrue(AppSearch.matches("Waze", ""))
        assertTrue(AppSearch.matches("Waze", "   "))
    }

    @Test fun ignoresCaseAndAccents() {
        assertTrue(AppSearch.matches("Città Meteo", "citta"))
        assertTrue(AppSearch.matches("GOOGLE MAPS", "maps"))
        assertTrue(AppSearch.matches("Impostazioni", "IMPOST"))
    }

    @Test fun allWordsMustMatch() {
        assertTrue(AppSearch.matches("Google Maps", "maps goo"))
        assertFalse(AppSearch.matches("Google Maps", "maps waze"))
    }

    @Test fun sortIsAccentInsensitive() {
        val sorted = AppSearch.sortByLabel(listOf("Zeta", "Èlite", "alfa", "Bravo")) { it }
        assertEquals(listOf("alfa", "Bravo", "Èlite", "Zeta"), sorted)
    }
}
