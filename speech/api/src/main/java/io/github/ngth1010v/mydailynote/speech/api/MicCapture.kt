package io.github.ngth1010v.mydailynote.speech.api

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.max

class MicUnavailableException(message: String) : Exception(message)

/**
 * Blocking microphone loop: 16 kHz mono 16-bit PCM in 100 ms chunks. Run it on a background
 * thread. Audio is only kept in memory (when [SpeechOptions.keepAudio]) and returned to the caller.
 */
class MicCapture(
    val sampleRate: Int = SAMPLE_RATE,
    /** Model-based speech detection for auto-stop; null = energy only. */
    private val detector: SpeechDetector? = null,
) {

    /**
     * Records until [shouldStop] returns true or the [SilenceDetector] ends the phrase.
     * [onChunk] receives each chunk (buffer is reused) and its 0..1 level.
     * Returns the whole recording if [SpeechOptions.keepAudio], else null.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record(
        options: SpeechOptions,
        shouldStop: () -> Boolean,
        onChunk: (buffer: ShortArray, count: Int, level: Float) -> Unit,
    ): ShortArray? {
        val chunk = sampleRate / 10
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) throw MicUnavailableException("Unsupported audio format")
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, chunk * 2 * 4),
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw MicUnavailableException("AudioRecord not initialized")
        }
        val vad = SilenceDetector(options.autoStopMs)
        detector?.reset()
        val buffer = ShortArray(chunk)
        val kept = if (options.keepAudio) ShortArrayBuilder(sampleRate * 5) else null
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicUnavailableException("Microphone busy")
            }
            while (!shouldStop()) {
                val n = record.read(buffer, 0, buffer.size)
                if (n < 0) throw MicUnavailableException("AudioRecord.read error $n")
                if (n == 0) continue
                kept?.append(buffer, n)
                val db = SilenceDetector.dbfs(buffer, n)
                onChunk(buffer, n, SilenceDetector.level(db))
                if (vad.feed(db, n * 1000 / sampleRate, detector?.isSpeech(buffer, n))) break
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
        return kept?.toArray()
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}

private class ShortArrayBuilder(initial: Int) {
    private var data = ShortArray(initial)
    private var size = 0

    fun append(src: ShortArray, count: Int) {
        if (size + count > data.size) data = data.copyOf(max(data.size * 2, size + count))
        System.arraycopy(src, 0, data, size, count)
        size += count
    }

    fun toArray(): ShortArray = data.copyOf(size)
}
