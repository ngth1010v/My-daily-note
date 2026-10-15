package io.github.ngth1010v.mydailynote.core.data

import androidx.room.withTransaction
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoGroup
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoStyle
import io.github.ngth1010v.mydailynote.core.data.autostyle.GroupCandidate
import io.github.ngth1010v.mydailynote.core.data.autostyle.Routing
import io.github.ngth1010v.mydailynote.core.data.db.ActionEntity
import io.github.ngth1010v.mydailynote.core.data.db.AppDatabase
import io.github.ngth1010v.mydailynote.core.data.db.EntryEntity
import io.github.ngth1010v.mydailynote.core.data.db.GroupEntity
import io.github.ngth1010v.mydailynote.core.data.model.Action
import io.github.ngth1010v.mydailynote.core.data.model.Group
import io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock

/** A group with its actions in order. */
data class GroupWithActions(val group: Group, val actions: List<Action>)

/** A new group's name and icon for an icon category (localized by the caller). */
class GroupCategory(val name: String, val icon: String)

/** What a delete removed, to put it back on undo. */
class DeletedGroup internal constructor(internal val group: GroupEntity, internal val actions: List<ActionEntity>)

class DeletedAction internal constructor(internal val action: ActionEntity, internal val entryIds: List<Long>)

sealed interface ActionEdit {
    data object Saved : ActionEdit
    /** The new text is another action's: the entries were merged into it. */
    data class Merged(val into: Action) : ActionEdit
}

class GroupRepository(
    private val db: AppDatabase,
    /** Name and icon for a new group of each icon category, by category index. */
    private val categories: () -> List<GroupCategory>,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = db.groupDao()

    /** All groups in order, each with its actions in order. */
    val groups: Flow<List<GroupWithActions>> = combine(dao.groupsFlow(), dao.actionsFlow()) { groups, actions ->
        val byGroup = actions.map { it.toModel() }.groupBy { it.groupId }
        groups.map { g -> GroupWithActions(g.toModel(), byGroup[g.id].orEmpty()) }
    }

    /** How many logged texts have no action (and so no group) yet; [assignNewActions] fixes them. */
    val pendingCount: Flow<Int> = dao.pendingCount()

    suspend fun action(text: String): Action? = dao.actionWithText(TextNormalizer.normalize(text))?.toModel()

    /** The built-in group, created first time it is needed (last in order). */
    suspend fun defaultGroup(): Group = db.withTransaction {
        dao.defaultGroup()?.toModel() ?: run {
            val position = (dao.groups().maxOfOrNull { it.position } ?: -1) + 1
            val entity = GroupEntity(
                name = "", color = null, icon = null, description = null,
                createdAt = clock.millis(), position = position, isDefault = true,
            )
            entity.copy(id = dao.insertGroup(entity)).toModel()
        }
    }

    /**
     * Gives every logged text without an action one, oldest first, styled like its latest entry.
     * With [route], it goes in the best-fitting group (see [AutoGroup]), and with [create] a group is
     * made when none fits; otherwise, or when nothing fits, it goes in the built-in group.
     */
    suspend fun assignNewActions(
        autoGroup: () -> AutoGroup,
        autoStyle: () -> AutoStyle,
        route: Boolean,
        create: Boolean,
    ) {
        val pending = dao.entriesWithoutAction()
        if (pending.isEmpty()) return
        // Latest entry per text, in order of each text's first appearance.
        val latest = LinkedHashMap<String, EntryEntity>()
        pending.forEach { latest[it.searchText] = it }

        val fallback = defaultGroup().id
        val groups = dao.groups().toMutableList()
        val members = dao.actions().groupBy({ it.groupId }, { it.text })
            .mapValues { it.value.toMutableList() }.toMutableMap()
        val groupIds = groups.mapTo(HashSet()) { it.id }
        for (entry in latest.values) {
            // Entries from older backups may name their group already.
            val groupId = entry.groupId?.takeIf { it in groupIds } ?: if (!route) fallback else {
                val candidates = groups.map { g ->
                    GroupCandidate(g.id, g.name, g.isDefault, g.category, members[g.id].orEmpty())
                }
                when (val r = autoGroup().route(entry.text, entry.iconOverride, candidates, create)) {
                    is Routing.Existing -> r.groupId
                    Routing.Default -> fallback
                    is Routing.Create -> {
                        val category = categories()[r.category]
                        val group = insertGroupBeforeDefault(
                            GroupEntity(
                                name = category.name,
                                color = autoStyle().freshColor(groups.map { it.color }),
                                icon = category.icon,
                                description = null,
                                createdAt = clock.millis(),
                                category = r.category,
                            )
                        )
                        groups.add(group)
                        groupIds.add(group.id)
                        group.id
                    }
                }
            }
            val inserted = dao.insertAction(
                ActionEntity(
                    text = entry.text,
                    searchText = entry.searchText,
                    groupId = groupId,
                    position = dao.nextActionPosition(groupId),
                    color = entry.colorOverride,
                    icon = entry.iconOverride,
                    createdAt = clock.millis(),
                )
            )
            if (inserted > 0) members.getOrPut(groupId) { mutableListOf() }.add(entry.text)
        }
    }

    /**
     * Makes sure [text] has an action, putting a new one in [groupId] (used when an entry's text is
     * edited to a new action: it stays in the group it was in).
     */
    suspend fun ensureAction(text: String, color: String?, icon: String?, groupId: Long) {
        val searchText = TextNormalizer.normalize(text)
        if (dao.actionWithText(searchText) != null) return
        val target = dao.group(groupId)?.id ?: defaultGroup().id
        dao.insertAction(
            ActionEntity(
                text = text.trim(), searchText = searchText, groupId = target,
                position = dao.nextActionPosition(target), color = color, icon = icon, createdAt = clock.millis(),
            )
        )
    }

    // Groups

    /** Adds a group at the end (before the built-in group if that is last). Returns its id. */
    suspend fun createGroup(name: String, color: String?, icon: String?, description: String?): Long =
        insertGroupBeforeDefault(
            Group(name = name, color = color, icon = icon, description = description, createdAt = clock.instant()).toEntity()
        ).id

    suspend fun updateGroup(group: Group) {
        val current = dao.group(group.id) ?: return
        dao.updateGroup(
            group.toEntity().copy(position = current.position, isDefault = current.isDefault, createdAt = current.createdAt)
        )
    }

    /** Puts groups in the order of [ids]. */
    suspend fun reorderGroups(ids: List<Long>) = db.withTransaction {
        ids.forEachIndexed { i, id -> dao.setGroupPosition(id, i) }
    }

    /**
     * Deletes a group, moving its actions to the end of the built-in group. The built-in group can't
     * be deleted (returns null).
     */
    suspend fun deleteGroup(id: Long): DeletedGroup? = db.withTransaction {
        val group = dao.group(id) ?: return@withTransaction null
        if (group.isDefault) return@withTransaction null
        val actions = dao.actionsIn(id)
        val fallback = defaultGroup().id
        var next = dao.nextActionPosition(fallback)
        actions.forEach { dao.placeAction(it.id, fallback, next++) }
        dao.deleteGroup(id)
        DeletedGroup(group, actions)
    }

    suspend fun undoDeleteGroup(deleted: DeletedGroup) = db.withTransaction {
        if (dao.group(deleted.group.id) == null) dao.insertGroup(deleted.group)
        deleted.actions.forEach { a ->
            if (dao.action(a.id) != null) dao.placeAction(a.id, a.groupId, a.position)
        }
    }

    // Actions

    /**
     * Puts the actions of each group in [layout] (group id to action ids in order) there, in that
     * order. Actions not listed keep their place.
     */
    suspend fun placeActions(layout: Map<Long, List<Long>>) = db.withTransaction {
        layout.forEach { (groupId, ids) ->
            ids.forEachIndexed { i, id -> dao.placeAction(id, groupId, i) }
        }
    }

    /** Adds an action with no entries at the end of [groupId]. Returns null if the text already has one. */
    suspend fun createAction(text: String, groupId: Long, color: String?, icon: String?): Long? {
        val searchText = TextNormalizer.normalize(text)
        if (searchText.isEmpty()) return null
        return db.withTransaction {
            if (dao.actionWithText(searchText) != null) return@withTransaction null
            dao.insertAction(
                ActionEntity(
                    text = text.trim(), searchText = searchText, groupId = groupId,
                    position = dao.nextActionPosition(groupId), color = color, icon = icon, createdAt = clock.millis(),
                )
            ).takeIf { it > 0 }
        }
    }

    /**
     * Saves an action's text and look; the look is also given to all its entries, and a new text
     * renames them. If the new text is another action's, this one is merged into that one.
     */
    suspend fun updateAction(id: Long, text: String, color: String?, icon: String?): ActionEdit? = db.withTransaction {
        val current = dao.action(id) ?: return@withTransaction null
        val newSearch = TextNormalizer.normalize(text)
        if (newSearch.isEmpty()) return@withTransaction null
        val other = dao.actionWithText(newSearch)?.takeIf { it.id != id }
        if (other != null) {
            dao.renameEntries(current.searchText, other.text, other.searchText, clock.millis())
            dao.styleEntries(other.searchText, other.color, other.icon)
            dao.deleteAction(id)
            return@withTransaction ActionEdit.Merged(other.toModel())
        }
        if (text.trim() != current.text) dao.renameEntries(current.searchText, text.trim(), newSearch, clock.millis())
        dao.updateAction(current.copy(text = text.trim(), searchText = newSearch, color = color, icon = icon))
        dao.styleEntries(newSearch, color, icon)
        ActionEdit.Saved
    }

    /** Deletes an action and (soft-deletes) all its entries. */
    suspend fun deleteAction(id: Long): DeletedAction? = db.withTransaction {
        val action = dao.action(id) ?: return@withTransaction null
        val ids = dao.liveEntryIds(action.searchText)
        if (ids.isNotEmpty()) dao.setEntriesDeleted(ids, clock.millis())
        dao.deleteAction(id)
        DeletedAction(action, ids)
    }

    suspend fun undoDeleteAction(deleted: DeletedAction) = db.withTransaction {
        val group = dao.group(deleted.action.groupId)?.id ?: defaultGroup().id
        dao.insertAction(deleted.action.copy(groupId = group))
        if (deleted.entryIds.isNotEmpty()) dao.setEntriesDeleted(deleted.entryIds, null)
    }

    private suspend fun insertGroupBeforeDefault(group: GroupEntity): GroupEntity = db.withTransaction {
        val groups = dao.groups()
        val default = groups.lastOrNull()?.takeIf { it.isDefault }
        val position = if (default != null) {
            dao.setGroupPosition(default.id, default.position + 1)
            default.position
        } else (groups.maxOfOrNull { it.position } ?: -1) + 1
        val entity = group.copy(id = 0, position = position)
        entity.copy(id = dao.insertGroup(entity))
    }
}
