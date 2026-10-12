package io.github.ngth1010v.mydailynote.speech.api

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Energy-based voice activity detection for short phrases. Learns the background noise level,
 * treats chunks clearly above it as speech, and asks to stop after [autoStopMs] of silence that
 * follows speech. Also stops after [noSpeechTimeoutMs] without any speech and at [maxDurationMs].
 */
class SilenceDetector(
    private val autoStopMs: Int,
    private val noSpeechTimeoutMs: Int = 8_000,
    private val maxDurationMs: Int = 30_000,
) {
    private var noiseDb = Float.NaN
    private var elapsedMs = 0
    private var silenceMs = 0
    var heardSpeech = false
        private set

    /** Feeds one chunk's loudness in dBFS; returns true when recording should stop. */
    fun feed(db: Float, chunkMs: Int): Boolean {
        elapsedMs += chunkMs
        // Start from a quiet floor: the user often speaks in the very first chunk.
        if (noiseDb.isNaN()) noiseDb = minOf(db, INITIAL_NOISE_DB)
        val threshold = max(MIN_SPEECH_DB, noiseDb + SPEECH_MARGIN_DB)
        if (db > threshold) {
            heardSpeech = true
            silenceMs = 0
        } else {
            silenceMs += chunkMs
            // Track the floor slowly so a loud room raises the threshold.
            noiseDb += (db - noiseDb) * 0.1f
        }
        return when {
            elapsedMs >= maxDurationMs -> true
            !heardSpeech -> elapsedMs >= noSpeechTimeoutMs
            else -> autoStopMs > 0 && silenceMs >= autoStopMs
        }
    }

    companion object {
        private const val MIN_SPEECH_DB = -45f
        private const val INITIAL_NOISE_DB = -60f
        private const val SPEECH_MARGIN_DB = 12f

        /** dBFS of 16-bit PCM samples (-90 for silence, 0 for full scale). */
        fun dbfs(samples: ShortArray, count: Int): Float {
            if (count <= 0) return -90f
            var sum = 0.0
            for (i in 0 until count) {
                val s = samples[i] / 32768.0
                sum += s * s
            }
            val rms = sqrt(sum / count)
            return if (rms <= 1e-5) -90f else (20 * log10(rms)).toFloat()
        }

        /** Maps dBFS to 0..1 for a level meter. */
        fun level(db: Float): Float = ((db + 60f) / 60f).coerceIn(0f, 1f)
    }
}
