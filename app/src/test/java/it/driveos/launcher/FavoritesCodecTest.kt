package it.driveos.launcher

import it.driveos.launcher.apps.FavoritesCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesCodecTest {
    private val a = "com.a/com.a.Main"
    private val b = "com.b/com.b.Main"

    @Test fun emptyOrNullDecodesToEmptyList() {
        assertTrue(FavoritesCodec.decode(null).isEmpty())
        assertTrue(FavoritesCodec.decode("").isEmpty())
    }

    @Test fun roundTripKeepsOrder() {
        val list = listOf(b, a)
        assertEquals(list, FavoritesCodec.decode(FavoritesCodec.encode(list)))
    }

    @Test fun decodeDropsInvalidAndDuplicates() {
        assertEquals(listOf(a, b), FavoritesCodec.decode("$a\nspazzatura\n$a\n\n$b"))
    }

    @Test fun toggleAddsThenRemoves() {
        val added = FavoritesCodec.toggle(listOf(a), b)
        assertEquals(listOf(a, b), added)
        assertEquals(listOf(b), FavoritesCodec.toggle(added, a))
    }
}
