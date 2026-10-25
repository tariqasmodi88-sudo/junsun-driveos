package it.driveos.launcher

import it.driveos.launcher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {
    @Test fun encodeDecodeRoundTrip() {
        val key = AppKey("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
        assertEquals(key, AppKey.decode(key.encode()))
    }

    @Test fun decodeRejectsInvalidValues() {
        assertNull(AppKey.decode(null))
        assertNull(AppKey.decode(""))
        assertNull(AppKey.decode("   "))
        assertNull(AppKey.decode("senza-slash"))
        assertNull(AppKey.decode("/solo.classe"))
        assertNull(AppKey.decode("solo.pacchetto/"))
        assertNull(AppKey.decode("a/b/c"))
    }

    @Test fun decodeTrimsSpaces() {
        assertEquals(AppKey("com.waze", "com.waze.MainActivity"), AppKey.decode(" com.waze / com.waze.MainActivity "))
    }
}
