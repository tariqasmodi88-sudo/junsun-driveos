package it.driveos.launcher

import it.driveos.launcher.media.MediaSources
import it.driveos.launcher.media.PlaybackMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMathTest {
    @Test fun advancesOnlyWhilePlaying() {
        assertEquals(15_000L, PlaybackMath.currentPosition(10_000, 1_000, 6_000, 1f, true, 200_000))
        assertEquals(10_000L, PlaybackMath.currentPosition(10_000, 1_000, 6_000, 1f, false, 200_000))
    }

    @Test fun clampsToDuration() {
        assertEquals(200_000L, PlaybackMath.currentPosition(199_000, 0, 10_000, 1f, true, 200_000))
        assertEquals(20_000L, PlaybackMath.currentPosition(10_000, 0, 10_000, 1f, true, 0))
    }

    @Test fun progressAndFormat() {
        assertEquals(0.5f, PlaybackMath.progress(50, 100), 0.0001f)
        assertEquals(0f, PlaybackMath.progress(50, 0), 0.0001f)
        assertEquals("3:07", PlaybackMath.format(187_000))
        assertEquals("1:02:03", PlaybackMath.format(3_723_000))
    }

    @Test fun audioSourceHeuristic() {
        assertTrue(MediaSources.isLikelyAudio("Radio", "com.syu.radio", emptySet(), null))
        assertTrue(MediaSources.isLikelyAudio("BT Music", "com.x.btmusic", emptySet(), null))
        assertTrue(MediaSources.isLikelyAudio("Qualcosa", "com.y", setOf("com.y"), null))
        assertFalse(MediaSources.isLikelyAudio("Chrome", "com.android.chrome", emptySet(), null))
    }
}
