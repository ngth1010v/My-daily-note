package io.github.ngth1010v.mydailynote.feature.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ngth1010v.mydailynote.core.data.ActionEdit
import io.github.ngth1010v.mydailynote.core.data.DeletedAction
import io.github.ngth1010v.mydailynote.core.data.DeletedGroup
import io.github.ngth1010v.mydailynote.core.data.EntryRepository
import io.github.ngth1010v.mydailynote.core.data.GroupRepository
import io.github.ngth1010v.mydailynote.core.data.GroupWithActions
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoStyle
import io.github.ngth1010v.mydailynote.core.data.model.Action
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.data.model.Group
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which editor sheet is open. */
sealed interface GroupEditor {
    data object NewGroup : GroupEditor
    data class EditGroup(val group: Group) : GroupEditor
    data class NewAction(val groupId: Long) : GroupEditor
    data class EditAction(val action: Action) : GroupEditor
}

sealed interface GroupEvent {
    data class GroupCreated(val id: Long) : GroupEvent
    data class GroupDeleted(val undo: DeletedGroup) : GroupEvent
    data class ActionDeleted(val undo: DeletedAction) : GroupEvent
    data class Merged(val into: String) : GroupEvent
    data object ActionExists : GroupEvent
    data class Logged(val text: String) : GroupEvent
}

class GroupViewModel(
    private val groups: GroupRepository,
    private val entries: EntryRepository,
    /** Slow the first time (loads the model): call off the main thread. */
    private val autoStyle: () -> AutoStyle,
) : ViewModel() {

    /** Null until loaded. */
    val sections: StateFlow<List<GroupWithActions>?> =
        groups.groups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var editor by mutableStateOf<GroupEditor?>(null)

    private val events = Channel<GroupEvent>(Channel.BUFFERED)
    val eventFlow: Flow<GroupEvent> = events.receiveAsFlow()

    init {
        // The built-in group is always there, even before anything is logged.
        viewModelScope.launch { groups.defaultGroup() }
    }

    fun reorderGroups(ids: List<Long>) {
        viewModelScope.launch { groups.reorderGroups(ids) }
    }

    fun placeActions(layout: Map<Long, List<Long>>) {
        viewModelScope.launch { groups.placeActions(layout) }
    }

    // Groups

    fun saveGroup(id: Long?, name: String, color: String?, icon: String?, description: String?) {
        viewModelScope.launch {
            if (id == null) {
                val picked = color ?: withContext(Dispatchers.Default) {
                    autoStyle().freshColor(sections.value.orEmpty().map { it.group.color })
                }
                events.send(GroupEvent.GroupCreated(groups.createGroup(name, picked, icon, description)))
            } else {
                val current = sections.value?.firstOrNull { it.group.id == id }?.group ?: return@launch
                groups.updateGroup(current.copy(name = name, color = color, icon = icon, description = description))
            }
        }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            groups.deleteGroup(id)?.let { events.send(GroupEvent.GroupDeleted(it)) }
        }
    }

    fun undoDeleteGroup(undo: DeletedGroup) {
        viewModelScope.launch { groups.undoDeleteGroup(undo) }
    }

    // Actions

    /** A new action without a chosen look gets one picked like a typed one. */
    fun saveAction(id: Long?, groupId: Long, text: String, color: String?, icon: String?) {
        viewModelScope.launch {
            if (id == null) {
                val (c, i) = if (color != null && icon != null) color to icon else {
                    val style = withContext(Dispatchers.Default) { autoStyle().pick(text, entries.styleHistory()) }
                    (color ?: style.color) to (icon ?: style.icon)
                }
                if (groups.createAction(text, groupId, c, i) == null) events.send(GroupEvent.ActionExists)
            } else {
                val edit = groups.updateAction(id, text, color, icon)
                if (edit is ActionEdit.Merged) events.send(GroupEvent.Merged(edit.into.text))
            }
        }
    }

    fun deleteAction(id: Long) {
        viewModelScope.launch {
            groups.deleteAction(id)?.let { events.send(GroupEvent.ActionDeleted(it)) }
        }
    }

    fun undoDeleteAction(undo: DeletedAction) {
        viewModelScope.launch { groups.undoDeleteAction(undo) }
    }

    /** Logs [action] again, now. */
    fun log(action: Action) {
        viewModelScope.launch {
            entries.addAsExisting(action.text, EntrySource.TEXT)
                ?: entries.add(action.text, EntrySource.TEXT, action.color, action.icon)
            events.send(GroupEvent.Logged(action.text))
        }
    }
}
