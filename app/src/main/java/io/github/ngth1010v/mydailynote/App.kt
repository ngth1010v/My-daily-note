package io.github.ngth1010v.mydailynote

import android.app.Application
import android.content.Context
import io.github.ngth1010v.mydailynote.core.data.EntryRepository
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoStyle
import io.github.ngth1010v.mydailynote.core.data.autostyle.IconMeaning
import io.github.ngth1010v.mydailynote.core.data.autostyle.TextEmbedder
import io.github.ngth1010v.mydailynote.core.data.backup.BackupRepository
import io.github.ngth1010v.mydailynote.core.data.db.AppDatabase
import io.github.ngth1010v.mydailynote.core.data.settings.SettingsRepository
import io.github.ngth1010v.mydailynote.core.ui.EntryIcons
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import java.io.File
import kotlin.concurrent.thread

class App : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Voice input was removed: free the speech models older versions downloaded (up to
        // several hundred MB). A no-op once they are gone.
        thread(name = "model-cleanup", priority = Thread.MIN_PRIORITY) {
            File(filesDir, "models").deleteRecursively()
            File(filesDir, "ggml-silero-v5.1.2.bin").delete()
            File(cacheDir, "models").deleteRecursively()
            // JNA (used by Vosk) unpacked its native library here.
            cacheDir.listFiles { f -> f.name.startsWith("jna") }?.forEach { it.deleteRecursively() }
        }
    }
}

/** Manual dependency wiring. Everything is lazy so app start only pays for what the first screen uses. */
class AppContainer(private val context: Context) {
    val database by lazy { AppDatabase.create(context) }
    val settings by lazy { SettingsRepository(context) }
    val entries by lazy { EntryRepository(database) }
    val backup by lazy { BackupRepository(database) }

    /** Loads the ~7 MB embedding model on first use (~150 ms on an emulator): touch it off the main thread. */
    val autoStyle by lazy {
        val embedder = context.assets.open("autostyle/embeddings.bin").use(::TextEmbedder)
        AutoStyle(
            embed = embedder::embed,
            icons = EntryIcons.groups.flatMapIndexed { g, group -> group.icons.map { IconMeaning(it.key, g, it.meaning) } },
            palette = EntryPalette.swatches.chunked(EntryPalette.COLUMNS).map { row -> row.map { it.key } },
        )
    }
}

val Context.container: AppContainer get() = (applicationContext as App).container
