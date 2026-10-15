package io.github.ngth1010v.mydailynote.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM action_groups ORDER BY position, id")
    fun groupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM action_groups ORDER BY position, id")
    suspend fun groups(): List<GroupEntity>

    @Query("SELECT * FROM action_groups WHERE id = :id")
    suspend fun group(id: Long): GroupEntity?

    @Query("SELECT * FROM action_groups WHERE isDefault = 1 ORDER BY id LIMIT 1")
    suspend fun defaultGroup(): GroupEntity?

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("UPDATE action_groups SET position = :position WHERE id = :id")
    suspend fun setGroupPosition(id: Long, position: Int)

    @Query("DELETE FROM action_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    /** Every action with how often and when it was last logged, by group then position. */
    @Query(
        "SELECT a.*, COUNT(e.id) AS count, MAX(e.achievedAt) AS lastAt FROM actions a " +
            "LEFT JOIN entries e ON e.searchText = a.searchText AND e.deletedAt IS NULL " +
            "GROUP BY a.id ORDER BY a.groupId, a.position, a.id"
    )
    fun actionsFlow(): Flow<List<ActionWithStats>>

    @Query("SELECT * FROM actions ORDER BY groupId, position, id")
    suspend fun actions(): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE id = :id")
    suspend fun action(id: Long): ActionEntity?

    @Query("SELECT * FROM actions WHERE searchText = :searchText")
    suspend fun actionWithText(searchText: String): ActionEntity?

    @Query("SELECT * FROM actions WHERE groupId = :groupId ORDER BY position, id")
    suspend fun actionsIn(groupId: Long): List<ActionEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM actions WHERE groupId = :groupId")
    suspend fun nextActionPosition(groupId: Long): Int

    /** Ignored when an action with the same text already exists. Returns -1 then. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAction(action: ActionEntity): Long

    @Update
    suspend fun updateAction(action: ActionEntity)

    @Query("UPDATE actions SET groupId = :groupId, position = :position WHERE id = :id")
    suspend fun placeAction(id: Long, groupId: Long, position: Int)

    @Query("DELETE FROM actions WHERE id = :id")
    suspend fun deleteAction(id: Long)

    /** How many distinct live entry texts have no action yet. */
    @Query(
        "SELECT COUNT(DISTINCT searchText) FROM entries WHERE deletedAt IS NULL " +
            "AND searchText NOT IN (SELECT searchText FROM actions)"
    )
    fun pendingCount(): Flow<Int>

    /** Live entries whose text has no action yet, oldest first. */
    @Query(
        "SELECT * FROM entries WHERE deletedAt IS NULL AND searchText NOT IN (SELECT searchText FROM actions) " +
            "ORDER BY achievedAt ASC, id ASC"
    )
    suspend fun entriesWithoutAction(): List<EntryEntity>

    @Query("UPDATE entries SET colorOverride = :color, iconOverride = :icon WHERE searchText = :searchText")
    suspend fun styleEntries(searchText: String, color: String?, icon: String?)

    @Query("UPDATE entries SET text = :text, searchText = :newSearchText, updatedAt = :at WHERE searchText = :searchText")
    suspend fun renameEntries(searchText: String, text: String, newSearchText: String, at: Long)

    @Query("SELECT id FROM entries WHERE searchText = :searchText AND deletedAt IS NULL")
    suspend fun liveEntryIds(searchText: String): List<Long>

    @Query("UPDATE entries SET deletedAt = :at WHERE id IN (:ids)")
    suspend fun setEntriesDeleted(ids: List<Long>, at: Long?)

    /** Gives each action the look of its latest entry (after entries were restyled). */
    @Query(
        "UPDATE actions SET " +
            "color = (SELECT e.colorOverride FROM entries e WHERE e.searchText = actions.searchText " +
            "AND e.deletedAt IS NULL ORDER BY e.achievedAt DESC LIMIT 1), " +
            "icon = (SELECT e.iconOverride FROM entries e WHERE e.searchText = actions.searchText " +
            "AND e.deletedAt IS NULL ORDER BY e.achievedAt DESC LIMIT 1) " +
            "WHERE EXISTS (SELECT 1 FROM entries e WHERE e.searchText = actions.searchText AND e.deletedAt IS NULL)"
    )
    suspend fun copyStylesFromEntries()
}
