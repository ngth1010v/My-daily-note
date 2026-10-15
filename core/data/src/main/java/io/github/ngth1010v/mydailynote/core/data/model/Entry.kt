package io.github.ngth1010v.mydailynote.core.data.model

import java.time.Instant
import java.time.ZoneOffset

/** VOICE: logged by voice input in older versions; kept so their entries and backups still load. */
enum class EntrySource { TEXT, VOICE }

/** One logged occurrence of a small action. */
data class Entry(
    val id: Long = 0,
    val text: String,
    val description: String? = null,
    val achievedAt: Instant,
    val zoneOffset: ZoneOffset,
    val createdAt: Instant = achievedAt,
    val updatedAt: Instant = createdAt,
    /** Entry's own color/icon; null = inherit from group, then app default. */
    val colorOverride: String? = null,
    val iconOverride: String? = null,
    val groupId: Long? = null,
    val source: EntrySource = EntrySource.TEXT,
    /** Group's color/icon (read-only, resolved by a join). */
    val groupColor: String? = null,
    val groupIcon: String? = null,
) {
    val color: String? get() = colorOverride ?: groupColor
    val icon: String? get() = iconOverride ?: groupIcon
}

/** A reusable "kind of action": a named, ordered set of [Action]s. */
data class Group(
    val id: Long = 0,
    /** Blank for the built-in group until renamed: show the localized "Other" then. */
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val description: String? = null,
    val createdAt: Instant,
    val position: Int = 0,
    /** The built-in group catching unrouted actions; can't be deleted. */
    val isDefault: Boolean = false,
    /** Icon category it was auto-created for, if any. */
    val category: Int? = null,
)

/** A distinct action text in a group, with how often and when it was last logged. */
data class Action(
    val id: Long = 0,
    val text: String,
    val groupId: Long,
    val position: Int = 0,
    val color: String? = null,
    val icon: String? = null,
    val createdAt: Instant,
    val count: Int = 0,
    val lastAt: Instant? = null,
)

/** A previously logged text, offered for one-tap re-logging. */
data class Suggestion(
    val text: String,
    val color: String?,
    val icon: String?,
    val count: Int,
)
