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
    val name: String,
    val color: String?,
    val icon: String?,
    val description: String?,
    val createdAt: Long,
)

data class EntryWithGroup(
    @Embedded val entry: EntryEntity,
    @ColumnInfo(name = "groupColor") val groupColor: String?,
    @ColumnInfo(name = "groupIcon") val groupIcon: String?,
)

data class SuggestionRow(
    val text: String,
    val colorOverride: String?,
    val iconOverride: String?,
    val count: Int,
)
