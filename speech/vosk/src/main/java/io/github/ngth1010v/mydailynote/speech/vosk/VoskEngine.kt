package io.github.ngth1010v.mydailynote.speech.vosk

import android.annotation.SuppressLint
import io.github.ngth1010v.mydailynote.speech.api.MicCapture
import io.github.ngth1010v.mydailynote.speech.api.MicUnavailableException
import io.github.ngth1010v.mydailynote.speech.api.ModelManager
import io.github.ngth1010v.mydailynote.speech.api.ModelRegistry
import io.github.ngth1010v.mydailynote.speech.api.SpeechEngine
import io.github.ngth1010v.mydailynote.speech.api.SpeechEngineIds
import io.github.ngth1010v.mydailynote.speech.api.SpeechError
import io.github.ngth1010v.mydailynote.speech.api.SpeechEvent
import io.github.ngth1010v.mydailynote.speech.api.SpeechOptions
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Offline streaming recognition with Vosk (Kaldi). Shows partial words while recording; the final
 * result joins every finished utterance. One model is kept loaded (switching language reloads).
 */
class VoskEngine(
    private val models: ModelManager,
    private val mic: MicCapture = MicCapture(),
) : SpeechEngine {
    override val id = SpeechEngineIds.VOSK

    private val loadLock = Mutex()
    private var loaded: Pair<String, Model>? = null

    @Volatile private var stopRequested = false

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
    }

    override fun isAvailable(language: VoiceLanguage): Boolean =
        models.isInstalled(ModelRegistry.vosk(language).id)

    override suspend fun prepare(language: VoiceLanguage) {
        load(language)
    }

    private suspend fun load(language: VoiceLanguage): Model? = loadLock.withLock {
        val spec = ModelRegistry.vosk(language)
        loaded?.let { (id, model) -> if (id == spec.id) return model }
        val path = models.path(spec.id) ?: return null
        withContext(Dispatchers.IO) {
            loaded?.second?.close()
            loaded = null
            runCatching { Model(path.absolutePath) }.getOrNull()?.also { loaded = spec.id to it }
        }
    }

    /** Caller must hold RECORD_AUDIO. */
    @SuppressLint("MissingPermission")
    override fun start(language: VoiceLanguage, options: SpeechOptions): Flow<SpeechEvent> = channelFlow {
        stopRequested = false
        val model = load(language)
        if (model == null) {
            send(SpeechEvent.Error(if (isAvailable(language)) SpeechError.ENGINE else SpeechError.NO_MODEL))
            return@channelFlow
        }
        val recognizer = Recognizer(model, mic.sampleRate.toFloat())
        try {
            send(SpeechEvent.Listening)
            val committed = StringBuilder()
            var lastPartial = ""
            val audio = try {
                mic.record(options, shouldStop = { stopRequested || isClosedForSend }) { buf, n, level ->
                    trySend(SpeechEvent.Level(level))
                    if (recognizer.acceptWaveForm(buf, n)) {
                        committed.appendWords(recognizer.result.field("text"))
                        lastPartial = committed.toString()
                    } else {
                        lastPartial = join(committed, recognizer.partialResult.field("partial"))
                    }
                    trySend(SpeechEvent.Partial(lastPartial))
                }
            } catch (e: MicUnavailableException) {
                send(SpeechEvent.Error(SpeechError.AUDIO))
                return@channelFlow
            }
            committed.appendWords(recognizer.finalResult.field("text"))
            val text = committed.toString()
            send(if (text.isBlank()) SpeechEvent.Error(SpeechError.NO_MATCH) else SpeechEvent.Final(text, audio))
        } finally {
            recognizer.close()
        }
    }.buffer(Channel.UNLIMITED).flowOn(Dispatchers.IO)

    override fun stop() {
        stopRequested = true
    }

    private fun String.field(name: String): String =
        runCatching { JSONObject(this).optString(name) }.getOrDefault("").trim()

    private fun StringBuilder.appendWords(words: String) {
        if (words.isBlank()) return
        if (isNotEmpty()) append(' ')
        append(words)
    }

    private fun join(committed: CharSequence, partial: String): String = when {
        partial.isBlank() -> committed.toString()
        committed.isEmpty() -> partial
        else -> "$committed $partial"
    }
}
