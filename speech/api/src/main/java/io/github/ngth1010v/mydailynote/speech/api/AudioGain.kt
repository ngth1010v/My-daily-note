package io.github.ngth1010v.mydailynote.speech.api

import kotlin.math.abs

/** Peak normalization for recorded clips: quiet speech is boosted, loud speech left alone. */
object AudioGain {
    private const val TARGET_PEAK = 0.9f * Short.MAX_VALUE
    /** Caps the boost so near-silent clips don't turn into amplified noise. */
    const val MAX_GAIN = 8f

    fun normalize(pcm: ShortArray): ShortArray {
        var peak = 0
        for (s in pcm) peak = maxOf(peak, abs(s.toInt()))
        if (peak == 0) return pcm
        val gain = minOf(TARGET_PEAK / peak, MAX_GAIN)
        if (gain <= 1f) return pcm
        return ShortArray(pcm.size) { i -> (pcm[i] * gain).toInt().coerceIn(-32768, 32767).toShort() }
    }
}
