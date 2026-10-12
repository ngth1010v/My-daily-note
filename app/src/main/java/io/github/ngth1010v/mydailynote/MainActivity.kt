package io.github.ngth1010v.mydailynote

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ngth1010v.mydailynote.core.ui.theme.MyDailyNoteTheme
import io.github.ngth1010v.mydailynote.feature.home.HomeLaunch

class MainActivity : AppCompatActivity() {
    /** Pending request from a widget, tile, shortcut or share; consumed by the Home screen. */
    private var launch by mutableStateOf<HomeLaunch?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launch = intent.toLaunch()
        val container = container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Draw nothing until settings load (a few ms) so the theme never flashes.
            val s = settings ?: return@setContent
            MyDailyNoteTheme(themeMode = s.themeMode, dynamicColor = s.dynamicColor) {
                MainScaffold(
                    container = container,
                    settings = s,
                    launch = launch,
                    onLaunchHandled = { launch = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunch()?.let { launch = it }
    }

    private fun Intent?.toLaunch(): HomeLaunch? = when (this?.action) {
        ACTION_ADD_VOICE -> HomeLaunch.Voice
        ACTION_ADD_TEXT -> HomeLaunch.Text
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() }?.let(HomeLaunch::Share)
        else -> null
    }

    companion object {
        const val ACTION_ADD_VOICE = "io.github.ngth1010v.mydailynote.action.ADD_VOICE"
        const val ACTION_ADD_TEXT = "io.github.ngth1010v.mydailynote.action.ADD_TEXT"
    }
}
