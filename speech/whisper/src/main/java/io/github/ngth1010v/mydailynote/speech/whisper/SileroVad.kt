package io.github.ngth1010v.mydailynote.speech.whisper

import android.content.Context
import io.github.ngth1010v.mydailynote.speech.api.SpeechDetector
import java.io.File

/** The Silero VAD model ships as an asset; whisper.cpp needs a real file. */
internal object SileroModel {
    private const val FILE = "ggml-silero-v5.1.2.bin"

    /** Absolute path, or "" if it could not be extracted (callers then run without VAD). */
    @Synchronized
    fun path(context: Context): String {
        val file = File(context.filesDir, FILE)
        if (!file.exists() || file.length() == 0L) {
            runCatching {
                context.assets.open(FILE).use { input -> file.outputStream().use { input.copyTo(it) } }
            }.onFailure { file.delete() }
        }
        return if (file.exists()) file.absolutePath else ""
    }
}

/**
 * Streaming Silero VAD for the mic's auto-stop: much less fooled by background noise and soft
 * speech than an energy threshold. Feeds whole 512-sample windows, carrying the remainder over.
 */
class SileroVad(context: Context) : SpeechDetector {
    private val appContext = context.applicationContext
    private var ctx = 0L
    private var failed = false
    private val carry = FloatArray(WINDOW)
    private var carried = 0
    /** Verdict of the last full window; reused while a chunk only fills the carry-over. */
    private var lastSpeech = false

    override fun reset() {
        carried = 0
        lastSpeech = false
        if (ctx == 0L && !failed) {
            val path = if (WhisperNative.loaded) SileroModel.path(appContext) else ""
            ctx = if (path.isEmpty()) 0L else runCatching { WhisperNative.vadInit(path) }.getOrDefault(0L)
            failed = ctx == 0L
        }
        if (ctx != 0L) WhisperNative.vadReset(ctx)
    }

    override fun isSpeech(chunk: ShortArray, count: Int): Boolean? {
        if (ctx == 0L) return null
        val total = carried + count
        val whole = total / WINDOW * WINDOW
        if (whole == 0) {
            for (i in 0 until count) carry[carried + i] = chunk[i] / 32768f
            carried = total
            return lastSpeech
        }
        val samples = FloatArray(whole)
        System.arraycopy(carry, 0, samples, 0, carried)
        val used = whole - carried
        for (i in 0 until used) samples[carried + i] = chunk[i] / 32768f
        carried = count - used
        for (i in 0 until carried) carry[i] = chunk[used + i] / 32768f
        val p = WhisperNative.vadMaxProb(ctx, samples)
        if (p < 0f) return null
        lastSpeech = p >= THRESHOLD
        return lastSpeech
    }


    private companion object {
        const val WINDOW = 512
        const val THRESHOLD = 0.5f
    }
}
