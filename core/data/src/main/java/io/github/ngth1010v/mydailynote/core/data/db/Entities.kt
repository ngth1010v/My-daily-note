package io.github.ngth1010v.mydailynote.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("achievedAt"), Index("groupId"), Index("searchText")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val description: String?,
    /** Epoch millis, UTC. */
    val achievedAt: Long,
    val zoneOffsetSeconds: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val colorOverride: String?,
    val iconOverride: String?,
    val groupId: Long?,
    val source: String,
    /** Lowercase diacritic-free copy of [text] for search and suggestions. */
    val searchText: String,
    /** Soft delete (for instant undo); purged later. */
    val deletedAt: Long?,
)

@Fts4(contentEntity = EntryEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "entries_fts")
data class EntryFts(val searchText: String)

@Entity(tableName = "action_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Blank for the built-in group until the user renames it: shown as the localized "Other". */
    val name: String,
    val color: String?,
    val icon: String?,
    val description: String?,
    val createdAt: Long,
    /** Order on the Group screen, ascending. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
    /** The built-in group that catches unrouted actions; there is exactly one and it can't be deleted. */
    @ColumnInfo(defaultValue = "0") val isDefault: Boolean = false,
    /** Icon category ([io.github.ngth1010v.mydailynote.core.data.autostyle.IconMeaning.group]) it was auto-created for. */
    val category: Int? = null,
)

/**
 * A distinct action text (ignoring case and accents), in exactly one group. Entries belong to it by
 * [searchText]; it can also exist with no entries (created on the Group screen).
 */
@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("searchText", unique = true), Index("groupId")],
)
data class ActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    /** [io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer.normalize] of [text]. */
    val searchText: String,
    val groupId: Long,
    /** Order inside its group, ascending. */
    val position: Int,
    val color: String?,
    val icon: String?,
    val createdAt: Long,
)

data class ActionWithStats(
    @Embedded val action: ActionEntity,
    /** Live entries logged with this action's text. */
    val count: Int,
    val lastAt: Long?,
)

data class EntryWithGroup(
    @Embedded val entry: EntryEntity,
    @ColumnInfo(name = "groupColor") val groupColor: String?,
    @ColumnInfo(name = "groupIcon") val groupIcon: String?,
)

/** A distinct action text with the color and icon it is shown with (its own, else its group's). */
data class StyledRow(
    val text: String,
    val color: String,
    val icon: String,
    val lastAt: Long,
)

data class SuggestionRow(
    val text: String,
    val colorOverride: String?,
    val iconOverride: String?,
    val count: Int,
)
