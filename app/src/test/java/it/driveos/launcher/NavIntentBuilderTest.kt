package it.driveos.launcher

import it.driveos.launcher.nav.KnownNavApps
import it.driveos.launcher.nav.NavIntentBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavIntentBuilderTest {
    private val dest = "Piazza del Duomo, Milano"
    private val encoded = "Piazza%20del%20Duomo%2C%20Milano"

    @Test fun blankDestinationGivesNoRequest() {
        assertNull(NavIntentBuilder.forDestination(KnownNavApps.WAZE, "  "))
        assertNull(NavIntentBuilder.genericFallback(""))
    }

    @Test fun googleMapsUsesNavigationScheme() {
        val r = NavIntentBuilder.forDestination(KnownNavApps.GOOGLE_MAPS, dest)!!
        assertEquals("google.navigation:q=$encoded", r.uri)
        assertEquals(KnownNavApps.GOOGLE_MAPS, r.packageName)
    }

    @Test fun wazeUsesUniversalLink() {
        val r = NavIntentBuilder.forDestination(KnownNavApps.WAZE, dest)!!
        assertEquals("https://waze.com/ul?q=$encoded&navigate=yes", r.uri)
        assertEquals(KnownNavApps.WAZE, r.packageName)
    }

    @Test fun otherAppsUseGeoWithTheirPackage() {
        val r = NavIntentBuilder.forDestination("com.example.navi", dest)!!
        assertEquals("geo:0,0?q=$encoded", r.uri)
        assertEquals("com.example.navi", r.packageName)
    }

    @Test fun noPackageGivesGenericGeo() {
        val r = NavIntentBuilder.forDestination(null, dest)!!
        assertEquals("geo:0,0?q=$encoded", r.uri)
        assertNull(r.packageName)
    }

    @Test fun fallbackHasNoPackage() {
        assertNull(NavIntentBuilder.genericFallback(dest)!!.packageName)
    }

    @Test fun specialCharactersAreEncoded() {
        val r = NavIntentBuilder.forDestination(null, "Via Sant'Anna 3 & co")!!
        assertEquals("geo:0,0?q=Via%20Sant%27Anna%203%20%26%20co", r.uri)
    }
}
