package io.github.ngth1010v.mydailynote.core.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** FOLLOW_LAYOUT = end side of the layout direction (right in LTR, left in RTL). */
enum class RailSide { RIGHT, LEFT, FOLLOW_LAYOUT }

enum class TimeFormat { SYSTEM, H12, H24 }

/**
 * Global preferences. The app UI language is not here: it is owned by AppCompat's per-app locale
 * storage (AppCompatDelegate.setApplicationLocales).
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val railSide: RailSide = RailSide.RIGHT,
    /** 0..23; entries before this hour belong to the previous day. */
    val dayStartHour: Int = 0,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    /** Palette key for entries with no color of their own. */
    val defaultColor: String = DEFAULT_COLOR,
    /** Language tag ("en", "vi") for speech; null = follow app language. */
    val voiceLanguage: String? = null,
    /** Speech engine id (see speech:api SpeechEngineIds). */
    val speechEngine: String = DEFAULT_ENGINE,
    /** Silence after speech that ends recording; 0 = manual stop only. */
    val autoStopMs: Int = 1500,
    /** Run the Whisper second pass when a Whisper model is installed. */
    val highAccuracy: Boolean = false,
    val whisperModelId: String = DEFAULT_WHISPER_MODEL,
    val autoBackup: Boolean = false,
    val updateCheck: Boolean = false,
) {
    companion object {
        const val DEFAULT_COLOR = "teal"
        const val DEFAULT_ENGINE = "vosk"
        const val DEFAULT_WHISPER_MODEL = "whisper-tiny-q5_1"
        val AUTO_STOP_CHOICES = listOf(0, 1000, 1500, 2000, 3000)
    }
}
