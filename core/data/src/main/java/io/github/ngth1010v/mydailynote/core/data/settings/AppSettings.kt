package io.github.ngth1010v.mydailynote.core.data.settings

import io.github.ngth1010v.mydailynote.core.data.model.RecommendOptions

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
    val autoBackup: Boolean = false,
    val updateCheck: Boolean = false,
    /** Put each new action in the group it fits best (else in the built-in "Other" group). */
    val autoGroupActions: Boolean = true,
    /** With [autoGroupActions]: make a group when none fits. */
    val autoCreateGroups: Boolean = true,
    /** Show today's likely actions, learned from history, on Home. */
    val recommendActions: Boolean = true,
    val recommend: RecommendOptions = RecommendOptions(),
)
