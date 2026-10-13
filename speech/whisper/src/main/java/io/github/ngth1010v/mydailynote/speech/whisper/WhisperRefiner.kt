package io.github.ngth1010v.mydailynote.speech.whisper

import android.content.Context
import io.github.ngth1010v.mydailynote.speech.api.AudioGain
import io.github.ngth1010v.mydailynote.speech.api.ModelManager
import io.github.ngth1010v.mydailynote.speech.api.ScoredWord
import io.github.ngth1010v.mydailynote.speech.api.SpeechRefiner
import io.github.ngth1010v.mydailynote.speech.api.TranscriptVote
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object WhisperNative {
    val loaded: Boolean = runCatching { System.loadLibrary("whisper_jni") }.isSuccess

    external fun init(modelPath: String): Long
    external fun free(ptr: Long)

    /** One "<prob>\t<word>" line per word. [beamSize] <= 1 = greedy; [vadPath] / [prompt] empty = unused. */
    external fun transcribe(
        ptr: Long, audio: FloatArray, language: String, threads: Int,
        beamSize: Int, temperature: Float, vadPath: String, prompt: String,
    ): String?

    /** The more likely of [candidates] ("en,vi") for the clip. */
    external fun detectLanguage(ptr: Long, audio: FloatArray, threads: Int, candidates: String): String

    /** RNNoise over 16 kHz mono PCM; same length out. */
    external fun denoise(pcm: ShortArray): ShortArray

    external fun vadInit(modelPath: String): Long
    external fun vadFree(ptr: Long)
    external fun vadReset(ptr: Long)

    /** Max speech probability over whole 512-sample windows; -1 on error. */
    external fun vadMaxProb(ptr: Long, audio: FloatArray): Float
}

/**
 * Second pass with whisper.cpp over the recorded clip (16 kHz mono): RNNoise removes background
 * noise, quiet speech is boosted, Silero VAD (inside whisper) skips the silent parts, and decoding
 * is beam search primed with the user's own vocabulary. If any word is low-confidence the clip is
 * decoded again (raw audio, then a sampled run) and the runs vote. Keeps one model loaded.
 */
class WhisperRefiner(private val models: ModelManager, context: Context) : SpeechRefiner {
    private val appContext = context.applicationContext
    private val lock = Mutex()
    private var loadedId: String? = null
    private var ctx = 0L

    override fun isAvailable(modelId: String): Boolean = WhisperNative.loaded && models.isInstalled(modelId)

    override suspend fun prepare(modelId: String) {
        lock.withLock { ensureLoaded(modelId) }
    }

    override suspend fun refine(
        audio: ShortArray,
        language: VoiceLanguage,
        modelId: String,
        prompt: String?,
        autoLanguage: Boolean,
    ): String? =
        lock.withLock {
            withContext(Dispatchers.Default) {
                if (!ensureLoaded(modelId)) return@withContext null
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
                val vad = SileroModel.path(appContext)
                val clean = AudioGain.normalize(runCatching { WhisperNative.denoise(audio) }.getOrDefault(audio))
                val lang = if (!autoLanguage) language.tag else WhisperNative.detectLanguage(
                    ctx, toFloat(clean), threads, VoiceLanguage.entries.joinToString(",") { it.tag },
                )
                fun decode(pcm: ShortArray, beam: Int, temperature: Float): List<ScoredWord>? =
                    WhisperNative.transcribe(ctx, toFloat(pcm), lang, threads, beam, temperature, vad, prompt.orEmpty())
                        ?.let(::parseWords)

                val first = decode(clean, BEAM, 0f) ?: return@withContext null
                var runs = listOf(first)
                if (TranscriptVote.needsRecheck(first)) {
                    // Different input / decoding on purpose: the denoiser can hurt, greedy + temperature explores.
                    runs = runs + listOfNotNull(decode(AudioGain.normalize(audio), BEAM, 0f), decode(clean, 1, 0.4f))
                }
                TranscriptVote.choose(runs).joinToString(" ") { it.text }.takeIf { it.isNotBlank() }
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
        const val BEAM = 5

        /** Parses "<prob>\t<word>" lines; drops non-speech tags Whisper emits ("[BLANK_AUDIO]", "(music)"). */
        fun parseWords(raw: String): List<ScoredWord> {
            val words = ArrayList<ScoredWord>()
            var depth = 0
            for (line in raw.lineSequence()) {
                val tab = line.indexOf('\t')
                if (tab < 0) continue
                val prob = line.substring(0, tab).toFloatOrNull() ?: continue
                val text = line.substring(tab + 1)
                val opens = text.count { it == '[' || it == '(' }
                val closes = text.count { it == ']' || it == ')' }
                val inTag = depth > 0 || opens > 0
                depth = (depth + opens - closes).coerceAtLeast(0)
                if (inTag || text == "♪") continue
                words += ScoredWord(text, prob)
            }
            return words
        }
    }
}
