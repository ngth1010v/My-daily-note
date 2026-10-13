package io.github.ngth1010v.mydailynote.speech.api

import kotlinx.coroutines.flow.Flow

/** A language the app can recognize. Adding one = add a model to [ModelRegistry] + translations. */
enum class VoiceLanguage(val tag: String) {
    EN("en"),
    VI("vi");

    companion object {
        /** Settings value for "detect English or Vietnamese per clip" (refiner only; live engine uses the app language). */
        const val AUTO_TAG = "auto"

        fun fromTag(tag: String?): VoiceLanguage? =
            entries.firstOrNull { tag != null && tag.startsWith(it.tag, ignoreCase = true) }
    }
}

object SpeechEngineIds {
    const val VOSK = "vosk"
    const val ANDROID = "android"
}

data class SpeechOptions(
    /** Silence after speech that ends recording; 0 = only [SpeechEngine.stop] ends it. */
    val autoStopMs: Int = 1500,
    /** Keep the recorded audio in [SpeechEvent.Final] (for a second-pass refiner). */
    val keepAudio: Boolean = false,
)

enum class SpeechError {
    /** The language's model is not downloaded yet. */
    NO_MODEL,
    /** Engine or language not available on this device. */
    NOT_AVAILABLE,
    /** Microphone could not be opened. */
    AUDIO,
    /** Nothing was recognized. */
    NO_MATCH,
    ENGINE,
}

sealed interface SpeechEvent {
    /** Mic is open and listening. */
    data object Listening : SpeechEvent
    /** Input level, 0..1, for a level meter. */
    data class Level(val level: Float) : SpeechEvent
    /** Live best guess so far (low accuracy). */
    data class Partial(val text: String) : SpeechEvent
    /** Recording ended; the engine's complete result. [audio] is 16 kHz mono PCM if requested. */
    class Final(val text: String, val audio: ShortArray? = null) : SpeechEvent
    /** A second pass is running on the audio. */
    data object Refining : SpeechEvent
    /** Higher-accuracy replacement for [Final]. */
    data class Refined(val text: String) : SpeechEvent
    data class Error(val error: SpeechError) : SpeechEvent
}

/**
 * A streaming recognizer. [start] returns a cold flow: collecting it opens the mic; it completes
 * after [SpeechEvent.Final] or [SpeechEvent.Error]. [stop] ends recording gracefully (a Final still
 * arrives); cancelling the collector aborts without a result.
 */
interface SpeechEngine {
    val id: String

    /** True if this engine can recognize [language] right now (e.g. model installed). */
    fun isAvailable(language: VoiceLanguage): Boolean

    /** Loads heavy resources ahead of time so the first tap starts instantly. Safe to call often. */
    suspend fun prepare(language: VoiceLanguage) {}

    fun start(language: VoiceLanguage, options: SpeechOptions): Flow<SpeechEvent>

    fun stop()
}

/** A slower, more accurate second pass over recorded audio (e.g. Whisper). */
interface SpeechRefiner {
    fun isAvailable(modelId: String): Boolean

    suspend fun prepare(modelId: String) {}

    /**
     * Returns the improved text, or null to keep the streaming result. [prompt] = words the user
     * often says (biases spelling); [autoLanguage] = pick English or Vietnamese from the audio.
     */
    suspend fun refine(
        audio: ShortArray,
        language: VoiceLanguage,
        modelId: String,
        prompt: String? = null,
        autoLanguage: Boolean = false,
    ): String?
}

/** Speech/no-speech decision for one mic chunk (e.g. Silero VAD). */
interface SpeechDetector {
    /** Call before each recording. */
    fun reset()

    /** True if the chunk contains speech; null if the detector is unavailable (energy fallback). */
    fun isSpeech(chunk: ShortArray, count: Int): Boolean?
}
