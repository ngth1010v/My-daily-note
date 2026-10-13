package io.github.ngth1010v.mydailynote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ngth1010v.mydailynote.speech.api.ModelRegistry
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import io.github.ngth1010v.mydailynote.speech.whisper.SileroVad
import io.github.ngth1010v.mydailynote.speech.whisper.WhisperRefiner
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the offline engines on a real recording. Needs, inside the app's files dir:
 * - test/en.wav: 16 kHz mono 16-bit speech of "I drank a glass of water and walked the dog"
 * - the English Vosk model and/or a Whisper model (installed from the app)
 * Each test is skipped when its inputs are missing.
 */
@RunWith(AndroidJUnit4::class)
class SpeechOnDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = context.container

    private fun audio(): ShortArray {
        val wav = File(context.filesDir, "test/en.wav")
        assumeTrue("missing ${wav.path}", wav.exists())
        val bytes = wav.readBytes()
        // Find the "data" chunk instead of assuming a 44-byte header.
        var i = 12
        while (i + 8 <= bytes.size) {
            val id = String(bytes, i, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(bytes, i + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") {
                val pcm = ByteBuffer.wrap(bytes, i + 8, minOf(size, bytes.size - i - 8)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                return ShortArray(pcm.remaining()).also { pcm.get(it) }
            }
            i += 8 + size
        }
        error("no data chunk")
    }

    @Test
    fun voskRecognizesEnglish() {
        val pcm = audio()
        val path = container.models.path(ModelRegistry.vosk(VoiceLanguage.EN).id)
        assumeTrue("English Vosk model not installed", path != null)
        val model = Model(path!!.absolutePath)
        val rec = Recognizer(model, 16000f)
        val parts = mutableListOf<String>()
        pcm.toList().chunked(1600).forEach { chunk ->
            val arr = chunk.toShortArray()
            if (rec.acceptWaveForm(arr, arr.size)) parts += JSONObject(rec.result).optString("text")
        }
        parts += JSONObject(rec.finalResult).optString("text")
        rec.close()
        model.close()
        val text = parts.joinToString(" ").trim()
        println("VOSK: $text")
        assertTrue("got '$text'", "water" in text && "dog" in text)
    }

    @Test
    fun whisperRecognizesEnglish() = runBlocking {
        val pcm = audio()
        val id = ModelRegistry.whisper.firstOrNull { container.models.isInstalled(it.id) }?.id
        assumeTrue("no Whisper model installed", id != null)
        val refiner = WhisperRefiner(container.models, context)
        assertTrue("native whisper library failed to load", refiner.isAvailable(id!!))
        val t0 = System.currentTimeMillis()
        refiner.prepare(id)
        val t1 = System.currentTimeMillis()
        val text = refiner.refine(pcm, VoiceLanguage.EN, id).orEmpty().lowercase()
        println("WHISPER ($id, load ${t1 - t0} ms, transcribe ${System.currentTimeMillis() - t1} ms): $text")
        assertTrue("got '$text'", "water" in text && "dog" in text)
    }

    @Test
    fun sileroVadSeparatesSpeechFromSilence() {
        val vad = SileroVad(context)
        fun anySpeech(pcm: ShortArray): Boolean {
            vad.reset()
            return pcm.toList().chunked(1600).any { vad.isSpeech(it.toShortArray(), it.size) == true }
        }
        assertTrue("no speech found in the test clip", anySpeech(audio()))
        assertFalse("speech found in silence", anySpeech(ShortArray(16_000 * 2)))
    }

    @Test
    fun whisperAutoLanguageMatchesExplicitEnglish() = runBlocking {
        val pcm = audio()
        val id = ModelRegistry.whisper.firstOrNull { container.models.isInstalled(it.id) }?.id
        assumeTrue("no Whisper model installed", id != null)
        val refiner = WhisperRefiner(container.models, context)
        val explicit = refiner.refine(pcm, VoiceLanguage.EN, id!!)
        val auto = refiner.refine(pcm, VoiceLanguage.VI, id, autoLanguage = true)
        println("WHISPER explicit: $explicit / auto: $auto")
        assertEquals(explicit?.lowercase()?.filter { it.isLetter() }, auto?.lowercase()?.filter { it.isLetter() })
    }
}
