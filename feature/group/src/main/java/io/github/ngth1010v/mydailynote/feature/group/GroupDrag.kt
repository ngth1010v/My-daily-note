package io.github.ngth1010v.mydailynote.feature.group

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlinx.coroutines.CancellationException
import io.github.ngth1010v.mydailynote.core.data.GroupWithActions
import io.github.ngth1010v.mydailynote.core.data.model.Action
import io.github.ngth1010v.mydailynote.core.data.model.Group

/** One draggable-list row of the Group screen. */
internal sealed interface GroupRow {
    val key: String

    data class Header(val group: Group, val count: Int, val expanded: Boolean) : GroupRow {
        override val key get() = "g${group.id}"
    }

    data class Item(val action: Action) : GroupRow {
        override val key get() = "a${action.id}"
    }

    /** "New action" at the bottom of an open group. */
    data class AddAction(val groupId: Long) : GroupRow {
        override val key get() = "n$groupId"
    }
}

/**
 * Long press, then drag. Unlike detectDragGesturesAfterLongPress, the moves are taken in the Initial
 * pass, before the list's own scrolling sees (and consumes) them, which would cancel the drag.
 * [onStart] gets the y of the press and returns whether a drag started.
 */
internal suspend fun PointerInputScope.detectRowDrag(
    onStart: (y: Float) -> Boolean,
    onDrag: (dy: Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
    if (!onStart(press.position.y)) return@awaitEachGesture
    try {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == press.id }
            if (change == null) {
                onCancel()
                break
            }
            val dy = change.positionChangeIgnoreConsumed().y
            change.consume()
            if (change.changedToUpIgnoreConsumed()) {
                onEnd()
                break
            }
            if (dy != 0f) onDrag(dy)
        }
    } catch (e: CancellationException) {
        onCancel()
        throw e
    }
}

/** Lazy list items before the rows (the title). */
internal const val ROWS_START = 1

internal fun buildRows(sections: List<GroupWithActions>, expanded: Set<Long>): List<GroupRow> = buildList {
    sections.forEach { (group, actions) ->
        val open = group.id in expanded
        add(GroupRow.Header(group, actions.size, open))
        if (open) {
            actions.forEach { add(GroupRow.Item(it)) }
            add(GroupRow.AddAction(group.id))
        }
    }
}

/**
 * Long-press drag of the Group screen's rows. While a drag runs (and until its result is saved) the
 * list shows [rows], a local copy that is rearranged as the dragged row passes others:
 * - a group: every group is shown closed and the group moves among them;
 * - an action: it moves among the actions of all open groups. Held over a closed group, that group
 *   is [hoverGroup] (the screen opens it after a moment); dropped there, the action joins its end.
 */
internal class GroupDragState(private val listState: LazyListState) {
    var rows by mutableStateOf<List<GroupRow>?>(null)
        private set
    var draggingKey by mutableStateOf<String?>(null)
        private set
    var hoverGroup by mutableStateOf<Long?>(null)
        private set

    /** Set after a drop that changed something: [rows] stay until the saved data comes back. */
    var awaitingSave by mutableStateOf(false)
        private set

    val isDragging get() = draggingKey != null

    private var startOffset = 0
    private var delta by mutableFloatStateOf(0f)
    private var draggingGroup = false

    /** How far the dragged row is drawn from where the list lays it out. */
    fun offsetOf(key: String): Float {
        if (key != draggingKey) return 0f
        val item = visible(key) ?: return 0f
        return startOffset + delta - item.offset
    }

    /** Starts dragging the row at [y] (list coordinates). Returns false when there is none. */
    fun start(y: Float, current: List<GroupRow>): Boolean {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { y.toInt() in it.offset until it.offset + it.size }
            ?: return false
        val row = current.firstOrNull { it.key == item.key } ?: return false
        if (row is GroupRow.AddAction) return false
        draggingKey = row.key
        startOffset = item.offset
        delta = 0f
        hoverGroup = null
        draggingGroup = row is GroupRow.Header
        if (draggingGroup) {
            val closed = current.filterIsInstance<GroupRow.Header>().map { it.copy(expanded = false) }
            rows = closed
            // Keep the dragged group under the finger while the open groups fold away.
            listState.requestScrollToItem(ROWS_START + closed.indexOfFirst { it.key == row.key }, -startOffset)
        } else {
            rows = current
        }
        return true
    }

    fun drag(dy: Float) {
        delta += dy
        rearrange()
    }

    /** Scroll speed (px per frame) when the dragged row is near the top or bottom edge. */
    fun edgeScroll(edge: Float): Float {
        val key = draggingKey ?: return 0f
        val size = visible(key)?.size ?: return 0f
        val info = listState.layoutInfo
        val top = startOffset + delta
        val bottom = top + size
        return when {
            top < info.viewportStartOffset + edge -> -((info.viewportStartOffset + edge - top) / edge).coerceAtMost(1f) * MAX_SCROLL
            bottom > info.viewportEndOffset - edge -> ((bottom - info.viewportEndOffset + edge) / edge).coerceAtMost(1f) * MAX_SCROLL
            else -> 0f
        }
    }

    /** Re-checks the position after the list scrolled under the finger. */
    fun afterScroll() = rearrange()

    /** Opens closed group [groupId] inside the drag with [actions] (the dragged one left out). */
    fun open(groupId: Long, actions: List<Action>) {
        val list = rows ?: return
        val i = list.indexOfFirst { it is GroupRow.Header && it.group.id == groupId }
        val header = list.getOrNull(i) as? GroupRow.Header ?: return
        if (header.expanded) return
        val inserted = actions.filter { "a${it.id}" != draggingKey }.map { GroupRow.Item(it) } + GroupRow.AddAction(groupId)
        rows = list.subList(0, i) + header.copy(expanded = true) + inserted + list.subList(i + 1, list.size)
        hoverGroup = null
    }

    /** What the drop changed, or null when nothing did. */
    sealed interface Drop {
        data class Groups(val ids: List<Long>) : Drop
        /** Group id to its action ids in order, for every group that changed. */
        data class Actions(val layout: Map<Long, List<Long>>, val openGroup: Long?) : Drop
    }

    /** Ends the drag; [open] = the groups open outside a drag. */
    fun end(sections: List<GroupWithActions>, open: Set<Long>): Drop? {
        val list = rows
        val key = draggingKey
        val hover = hoverGroup
        draggingKey = null
        hoverGroup = null
        if (list == null || key == null) return finish(null)
        if (draggingGroup) {
            val ids = list.filterIsInstance<GroupRow.Header>().map { it.group.id }
            val changed = ids != sections.map { it.group.id }
            // Reopen the groups around the dropped one without it jumping away from the finger.
            val reopened = buildRows(sections.sortedBy { ids.indexOf(it.group.id) }, open)
            val index = reopened.indexOfFirst { it.key == key }
            if (index >= 0) listState.requestScrollToItem(ROWS_START + index, -(startOffset + delta).toInt())
            rows = reopened
            return finish(if (changed) Drop.Groups(ids) else null)
        }

        val draggedId = key.drop(1).toLong()
        val layout = LinkedHashMap<Long, MutableList<Long>>()
        var group: Long? = null
        for (row in list) when (row) {
            is GroupRow.Header -> {
                group = row.group.id
                if (row.expanded) layout[group] = mutableListOf()
            }
            is GroupRow.Item -> group?.let { g -> layout[g]?.add(row.action.id) }
            is GroupRow.AddAction -> {}
        }
        if (hover != null) {
            layout.values.forEach { it.remove(draggedId) }
            layout[hover] = (sections.firstOrNull { it.group.id == hover }?.actions.orEmpty().map { it.id } - draggedId + draggedId)
                .toMutableList()
        }
        val before = sections.associate { (g, actions) -> g.id to actions.map { it.id } }
        val changed = layout.filter { (g, ids) -> before[g] != ids }
        return finish(if (changed.isEmpty()) null else Drop.Actions(changed, hover))
    }

    fun cancel() {
        draggingKey = null
        hoverGroup = null
        finish(null)
    }

    /** New data arrived: drop the local copy once the dropped result is in it. */
    fun onSaved() {
        if (draggingKey == null) {
            awaitingSave = false
            rows = null
        }
    }

    private fun finish(drop: Drop?): Drop? {
        awaitingSave = drop != null
        if (drop == null) rows = null
        return drop
    }

    private fun rearrange() {
        val key = draggingKey ?: return
        val list = rows ?: return
        val dragged = visible(key) ?: return
        val middle = (startOffset + delta + dragged.size / 2f).toInt()
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != key && middle in it.offset until it.offset + it.size
        }
        val targetRow = target?.let { t -> list.firstOrNull { it.key == t.key } }
        if (targetRow == null) {
            hoverGroup = null
            return
        }
        if (draggingGroup) {
            hoverGroup = null
            if (targetRow is GroupRow.Header) {
                keepScroll(key, targetRow.key)
                rows = list.move(key, targetRow.key)
            }
            return
        }
        if (targetRow is GroupRow.Header && !targetRow.expanded) {
            hoverGroup = targetRow.group.id
            return
        }
        hoverGroup = null
        val moved = list.move(key, targetRow.key)
        val i = moved.indexOfFirst { it.key == key }
        val prev = moved.getOrNull(i - 1)
        val next = moved.getOrNull(i + 1)
        val valid = (prev is GroupRow.Item || (prev is GroupRow.Header && prev.expanded)) &&
            (next is GroupRow.Item || next is GroupRow.AddAction)
        if (valid) {
            keepScroll(key, targetRow.key)
            rows = moved
        }
    }

    /**
     * The list keeps its first visible row in place by key, so moving that row would scroll the list
     * along with it: hold the position by index instead.
     */
    private fun keepScroll(vararg keys: String) {
        val first = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return
        if (first.key in keys) listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }

    private fun visible(key: String): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    private fun List<GroupRow>.move(from: String, to: String): List<GroupRow> {
        val i = indexOfFirst { it.key == from }
        val j = indexOfFirst { it.key == to }
        if (i < 0 || j < 0 || i == j) return this
        return toMutableList().apply { add(j, removeAt(i)) }
    }

    private companion object {
        const val MAX_SCROLL = 24f
    }
}
