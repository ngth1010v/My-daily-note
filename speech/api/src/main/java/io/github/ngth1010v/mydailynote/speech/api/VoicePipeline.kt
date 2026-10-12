package io.github.ngth1010v.mydailynote.speech.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Two-pass recognition: a streaming engine shows words live and gives a Final result, then an
 * optional refiner re-reads the recorded audio and may replace it with a better [SpeechEvent.Refined].
 */
class VoicePipeline(
    engines: List<SpeechEngine>,
    private val refiner: SpeechRefiner?,
) {
    private val engines = engines.associateBy { it.id }

    @Volatile private var active: SpeechEngine? = null

    fun engine(id: String): SpeechEngine = engines[id] ?: engines.getValue(SpeechEngineIds.VOSK)

    fun refinerAvailable(modelId: String): Boolean = refiner?.isAvailable(modelId) == true

    suspend fun prepare(engineId: String, language: VoiceLanguage, refineModelId: String?) {
        engine(engineId).prepare(language)
        if (refineModelId != null && refinerAvailable(refineModelId)) refiner?.prepare(refineModelId)
    }

    /** [refineModelId] = Whisper model to use for the second pass, or null to skip it. */
    fun start(
        engineId: String,
        language: VoiceLanguage,
        autoStopMs: Int,
        refineModelId: String?,
    ): Flow<SpeechEvent> = flow {
        val engine = engine(engineId)
        val refine = refineModelId?.takeIf { refinerAvailable(it) }
        active = engine
        try {
            engine.start(language, SpeechOptions(autoStopMs, keepAudio = refine != null)).collect { event ->
                emit(event)
                val audio = (event as? SpeechEvent.Final)?.audio
                if (refine != null && audio != null && event.text.isNotBlank()) {
                    emit(SpeechEvent.Refining)
                    val better = runCatching { refiner?.refine(audio, language, refine) }.getOrNull()
                    if (!better.isNullOrBlank()) emit(SpeechEvent.Refined(better))
                }
            }
        } finally {
            active = null
        }
    }

    /** Ends the current recording gracefully (a Final result still arrives). */
    fun stop() {
        active?.stop()
    }
}

private val SpeechEvent.text: String get() = (this as? SpeechEvent.Final)?.text.orEmpty()
