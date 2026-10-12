package io.github.ngth1010v.mydailynote.speech.whisper

import io.github.ngth1010v.mydailynote.speech.api.ModelManager
import io.github.ngth1010v.mydailynote.speech.api.SpeechRefiner
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object WhisperNative {
    val loaded: Boolean = runCatching { System.loadLibrary("whisper_jni") }.isSuccess

    external fun init(modelPath: String): Long
    external fun free(ptr: Long)
    external fun transcribe(ptr: Long, audio: FloatArray, language: String, threads: Int, audioCtx: Int): String?
}

/**
 * Second pass with whisper.cpp: re-transcribes the recorded clip (16 kHz mono) with a
 * multilingual Whisper model. Keeps one model loaded.
 */
class WhisperRefiner(private val models: ModelManager) : SpeechRefiner {
    private val lock = Mutex()
    private var loadedId: String? = null
    private var ctx = 0L

    override fun isAvailable(modelId: String): Boolean = WhisperNative.loaded && models.isInstalled(modelId)

    override suspend fun prepare(modelId: String) {
        lock.withLock { ensureLoaded(modelId) }
    }

    override suspend fun refine(audio: ShortArray, language: VoiceLanguage, modelId: String): String? =
        lock.withLock {
            withContext(Dispatchers.Default) {
                if (!ensureLoaded(modelId)) return@withContext null
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
                val samples = toFloat(audio)
                val raw = WhisperNative.transcribe(ctx, samples, language.tag, threads, audioCtx(samples.size))
                raw?.let(::clean)
            }
        }

    /** Caller holds [lock]. */
    private suspend fun ensureLoaded(modelId: String): Boolean {
        if (!isAvailable(modelId)) return false
        if (loadedId == modelId && ctx != 0L) return true
        val path = models.path(modelId) ?: return false
        return withContext(Dispatchers.IO) {
            if (ctx != 0L) WhisperNative.free(ctx)
            ctx = WhisperNative.init(path.absolutePath)
            loadedId = if (ctx != 0L) modelId else null
            ctx != 0L
        }
    }

    private fun toFloat(audio: ShortArray): FloatArray {
        // Whisper works on >= 1 s windows; pad short clips with silence.
        val out = FloatArray(maxOf(audio.size, MIN_SAMPLES))
        for (i in audio.indices) out[i] = audio[i] / 32768f
        return out
    }

    private companion object {
        const val MIN_SAMPLES = 16_000 + 1_600

        /** Whisper's encoder sees 50 frames per second (1500 = 30 s); add margin so words at the end survive. */
        fun audioCtx(samples: Int): Int = (samples / 16_000f * 50).toInt().plus(128).coerceAtMost(1500)
        private val nonSpeech = Regex("\\[[^\\]]*]|\\([^)]*\\)|♪")

        /** Drops non-speech tags Whisper emits ("[BLANK_AUDIO]", "(music)"). */
        fun clean(text: String): String? =
            text.replace(nonSpeech, " ").replace(Regex("\\s+"), " ").trim().takeIf { it.isNotEmpty() }
    }
}
