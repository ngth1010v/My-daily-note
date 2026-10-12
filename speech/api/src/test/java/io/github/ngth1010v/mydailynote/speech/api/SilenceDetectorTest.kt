package io.github.ngth1010v.mydailynote.speech.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceDetectorTest {
    private fun SilenceDetector.feedMany(db: Float, chunks: Int): Boolean {
        repeat(chunks) { if (feed(db, 100)) return true }
        return false
    }

    @Test fun stopsAfterSilenceFollowingSpeech() {
        val vad = SilenceDetector(autoStopMs = 1500)
        assertFalse(vad.feedMany(-60f, 5)) // background
        assertFalse(vad.feedMany(-20f, 10)) // speaking
        assertTrue(vad.heardSpeech)
        assertFalse(vad.feedMany(-60f, 14)) // 1.4 s silence
        assertTrue(vad.feed(-60f, 100)) // 1.5 s
    }

    @Test fun speechInFirstChunkIsNotMistakenForNoise() {
        val vad = SilenceDetector(autoStopMs = 500)
        vad.feed(-20f, 100)
        assertTrue(vad.heardSpeech)
        assertFalse(vad.feedMany(-60f, 4))
        assertTrue(vad.feed(-60f, 100))
    }

    @Test fun manualModeNeverAutoStopsAfterSpeech() {
        val vad = SilenceDetector(autoStopMs = 0)
        vad.feedMany(-20f, 5)
        assertFalse(vad.feedMany(-60f, 100))
    }

    @Test fun givesUpWithoutSpeech() {
        val vad = SilenceDetector(autoStopMs = 1500, noSpeechTimeoutMs = 2000)
        assertFalse(vad.feedMany(-60f, 19))
        assertTrue(vad.feed(-60f, 100))
        assertFalse(vad.heardSpeech)
    }

    @Test fun hardLimit() {
        val vad = SilenceDetector(autoStopMs = 0, maxDurationMs = 1000)
        assertFalse(vad.feedMany(-20f, 9))
        assertTrue(vad.feed(-20f, 100))
    }

    @Test fun dbfs() {
        assertEquals(-90f, SilenceDetector.dbfs(ShortArray(10), 10))
        val full = ShortArray(10) { Short.MAX_VALUE }
        assertEquals(0f, SilenceDetector.dbfs(full, 10), 0.01f)
        assertEquals(1f, SilenceDetector.level(0f))
        assertEquals(0f, SilenceDetector.level(-80f))
    }
}
