package io.github.ngth1010v.mydailynote.speech.api

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioGainTest {
    @Test
    fun quietClipIsBoostedToTargetPeak() {
        val out = AudioGain.normalize(shortArrayOf(0, 4000, -4000))
        assertEquals(29490, out[1].toInt())
        assertEquals(-29490, out[2].toInt())
    }

    @Test
    fun boostIsCappedAndLoudClipUntouched() {
        assertEquals(800, AudioGain.normalize(shortArrayOf(100, -50))[0].toInt())
        val loud = shortArrayOf(32000, -100)
        assertArrayEquals(loud, AudioGain.normalize(loud))
    }
}
