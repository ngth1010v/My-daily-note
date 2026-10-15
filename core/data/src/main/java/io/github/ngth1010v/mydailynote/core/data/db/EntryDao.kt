package io.github.ngth1010v.mydailynote.core.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val SELECT_WITH_GROUP =
    "SELECT e.*, g.color AS groupColor, g.icon AS groupIcon FROM entries e " +
        "LEFT JOIN action_groups g ON e.groupId = g.id"

@Dao
interface EntryDao {
    /** Newest first; the Home list renders it bottom-up (reverse layout). */
    @Query("$SELECT_WITH_GROUP WHERE e.deletedAt IS NULL ORDER BY e.achievedAt DESC, e.id DESC")
    fun pagingNewestFirst(): PagingSource<Int, EntryWithGroup>

    @Query("$SELECT_WITH_GROUP WHERE e.id = :id")
    suspend fun get(id: Long): EntryWithGroup?

    @Query(
        "$SELECT_WITH_GROUP JOIN entries_fts f ON e.id = f.rowid " +
            "WHERE entries_fts MATCH :match AND e.deletedAt IS NULL " +
            "ORDER BY e.achievedAt DESC LIMIT :limit"
    )
    fun search(match: String, limit: Int): Flow<List<EntryWithGroup>>

    /** Most frequent texts starting with [prefix] (normalized, LIKE-escaped), ties broken by recency. */
    @Query(
        "SELECT text, colorOverride, iconOverride, COUNT(*) AS count FROM entries " +
            "WHERE deletedAt IS NULL AND searchText LIKE :prefix || '%' ESCAPE '\\' " +
            "GROUP BY searchText ORDER BY count DESC, MAX(achievedAt) DESC LIMIT :limit"
    )
    fun suggestions(prefix: String, limit: Int): Flow<List<SuggestionRow>>

    /** The most recent entry whose normalized text is [searchText]. */
    @Query(
        "$SELECT_WITH_GROUP WHERE e.deletedAt IS NULL AND e.searchText = :searchText " +
            "ORDER BY e.achievedAt DESC, e.id DESC LIMIT 1"
    )
    suspend fun latestWithText(searchText: String): EntryWithGroup?

    /** The latest style of each distinct text that has a color and an icon, most recent first. */
    @Query(
        "SELECT e.text AS text, COALESCE(e.colorOverride, g.color) AS color, " +
            "COALESCE(e.iconOverride, g.icon) AS icon, MAX(e.achievedAt) AS lastAt FROM entries e " +
            "LEFT JOIN action_groups g ON e.groupId = g.id WHERE e.deletedAt IS NULL " +
            "AND COALESCE(e.colorOverride, g.color) IS NOT NULL AND COALESCE(e.iconOverride, g.icon) IS NOT NULL " +
            "GROUP BY e.searchText ORDER BY lastAt DESC LIMIT :limit"
    )
    suspend fun styledTexts(limit: Int): List<StyledRow>

    /** Entries shown without a color or an icon, oldest first. */
    @Query(
        "$SELECT_WITH_GROUP WHERE e.deletedAt IS NULL AND (COALESCE(e.colorOverride, g.color) IS NULL " +
            "OR COALESCE(e.iconOverride, g.icon) IS NULL) ORDER BY e.achievedAt ASC, e.id ASC"
    )
    suspend fun unstyled(): List<EntryWithGroup>

    @Query("UPDATE entries SET colorOverride = :color, iconOverride = :icon WHERE id = :id")
    suspend fun setStyle(id: Long, color: String?, icon: String?)

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<EntryEntity>)

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("UPDATE entries SET deletedAt = :at WHERE id = :id")
    suspend fun setDeleted(id: Long, at: Long?)

    @Query("DELETE FROM entries WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL ORDER BY achievedAt ASC, id ASC")
    suspend fun allForExport(): List<EntryEntity>

    @Query("SELECT achievedAt || '|' || text FROM entries WHERE deletedAt IS NULL")
    suspend fun allKeys(): List<String>

    @Query("SELECT COUNT(*) FROM entries WHERE deletedAt IS NULL")
    fun count(): Flow<Int>

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM action_groups")
    suspend fun deleteAllGroups()

    @Transaction
    suspend fun clearAll() {
        deleteAllEntries()
        deleteAllGroups()
    }
}
