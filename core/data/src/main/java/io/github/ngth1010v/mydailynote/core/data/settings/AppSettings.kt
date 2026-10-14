package io.github.ngth1010v.mydailynote.core.data.settings

/** FOLLOW_LAYOUT = end side of the layout direction (right in LTR, left in RTL). */
enum class RailSide { RIGHT, LEFT, FOLLOW_LAYOUT }

enum class TimeFormat { SYSTEM, H12, H24 }

/**
 * Global preferences. The app UI language is not here: it is owned by AppCompat's per-app locale
 * storage (AppCompatDelegate.setApplicationLocales).
 */
data class AppSettings(
    val dynamicColor: Boolean = true,
    val railSide: RailSide = RailSide.RIGHT,
    /** 0..23; entries before this hour belong to the previous day. */
    val dayStartHour: Int = 0,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    /** Palette key for entries with no color of their own. */
    val defaultColor: String = DEFAULT_COLOR,
    val autoBackup: Boolean = false,
    val updateCheck: Boolean = false,
) {
    companion object {
        const val DEFAULT_COLOR = "teal"
    }
}
