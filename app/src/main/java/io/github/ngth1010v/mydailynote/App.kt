package io.github.ngth1010v.mydailynote

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import io.github.ngth1010v.mydailynote.core.data.EntryRepository
import io.github.ngth1010v.mydailynote.core.data.backup.BackupRepository
import io.github.ngth1010v.mydailynote.core.data.db.AppDatabase
import io.github.ngth1010v.mydailynote.core.data.settings.SettingsRepository
import io.github.ngth1010v.mydailynote.speech.api.MicCapture
import io.github.ngth1010v.mydailynote.speech.api.ModelManager
import io.github.ngth1010v.mydailynote.speech.api.PlatformSpeechEngine
import io.github.ngth1010v.mydailynote.speech.api.VoicePipeline
import io.github.ngth1010v.mydailynote.speech.vosk.VoskEngine
import io.github.ngth1010v.mydailynote.speech.whisper.SileroVad
import io.github.ngth1010v.mydailynote.speech.whisper.WhisperRefiner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Locale

class App : Application() {
    val container by lazy { AppContainer(this) }
}

/** Manual dependency wiring. Everything is lazy so app start only pays for what the first screen uses. */
class AppContainer(private val context: Context) {
    /** Outlives screens: model downloads keep going when the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.create(context) }
    val settings by lazy { SettingsRepository(context) }
    val entries by lazy { EntryRepository(database) }
    val backup by lazy { BackupRepository(database) }
    val models by lazy { ModelManager(context, appScope) }
    val voice by lazy {
        VoicePipeline(
            engines = listOf(VoskEngine(models, MicCapture(detector = SileroVad(context))), PlatformSpeechEngine(context)),
            refiner = WhisperRefiner(models, context),
        )
    }

    /** Current UI language: the per-app choice if set, else the system's. */
    fun appLanguage(): String =
        AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
}

val Context.container: AppContainer get() = (applicationContext as App).container
