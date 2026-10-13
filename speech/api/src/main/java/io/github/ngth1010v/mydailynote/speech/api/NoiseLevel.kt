package io.github.ngth1010v.mydailynote.speech.api

import kotlin.math.log10
import kotlin.math.sqrt

/** Rough signal-to-noise estimate of a recorded clip, to denoise only clips that need it. */
object NoiseLevel {
    /** Below this SNR the background is loud enough that RNNoise helps more than it distorts. */
    const val DENOISE_BELOW_DB = 20f
    private const val FRAME = 480 // 30 ms at 16 kHz

    /**
     * Loudest 10 % of 30 ms frames (speech) vs. quietest 10 % (background), in dB. Null when the
     * clip is too short to tell.
     */
    fun snrDb(pcm: ShortArray): Float? {
        val frames = pcm.size / FRAME
        if (frames < 10) return null
        val rms = FloatArray(frames) { f ->
            var sum = 0.0
            for (i in f * FRAME until (f + 1) * FRAME) sum += pcm[i].toDouble() * pcm[i]
            sqrt(sum / FRAME).toFloat()
        }.sorted()
        val tenth = frames / 10
        val noise = rms.take(tenth).average().coerceAtLeast(1.0)
        val speech = rms.takeLast(tenth).average().coerceAtLeast(1.0)
        return (20 * log10(speech / noise)).toFloat()
    }

    fun isNoisy(pcm: ShortArray): Boolean = snrDb(pcm)?.let { it < DENOISE_BELOW_DB } ?: false
}
