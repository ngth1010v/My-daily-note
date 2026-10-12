package io.github.ngth1010v.mydailynote.speech.api

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Android's built-in on-device recognizer (Android 12+). Zero download, but whether a language
 * works offline depends on the device's speech service, so it is an optional engine.
 */
class PlatformSpeechEngine(private val context: Context) : SpeechEngine {
    override val id = SpeechEngineIds.ANDROID

    @Volatile private var recognizer: SpeechRecognizer? = null

    override fun isAvailable(language: VoiceLanguage): Boolean = isSupported(context)

    override fun start(language: VoiceLanguage, options: SpeechOptions): Flow<SpeechEvent> = callbackFlow {
        if (!isSupported(context)) {
            trySend(SpeechEvent.Error(SpeechError.NOT_AVAILABLE))
            close()
            return@callbackFlow
        }
        val sr = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = sr
        var lastPartial = ""
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(SpeechEvent.Listening) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                trySend(SpeechEvent.Level(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)))
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.best()?.let { lastPartial = it; trySend(SpeechEvent.Partial(it)) }
            }
            override fun onResults(results: Bundle?) {
                val text = results.best() ?: lastPartial
                trySend(if (text.isBlank()) SpeechEvent.Error(SpeechError.NO_MATCH) else SpeechEvent.Final(text))
                close()
            }
            override fun onError(error: Int) {
                val mapped = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_MATCH
                    SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.AUDIO
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechError.NOT_AVAILABLE
                    else -> SpeechError.ENGINE
                }
                // A partial result is still better than nothing (e.g. stopped mid-sentence).
                trySend(if (lastPartial.isNotBlank() && mapped == SpeechError.NO_MATCH) SpeechEvent.Final(lastPartial) else SpeechEvent.Error(mapped))
                close()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == VoiceLanguage.VI) "vi-VN" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            if (options.autoStopMs > 0) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, options.autoStopMs.toLong())
            }
        }
        sr.startListening(intent)
        awaitClose {
            recognizer = null
            sr.destroy()
        }
    }.flowOn(Dispatchers.Main)

    override fun stop() {
        recognizer?.stopListening()
    }

    private fun Bundle?.best(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        fun isSupported(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }
}
