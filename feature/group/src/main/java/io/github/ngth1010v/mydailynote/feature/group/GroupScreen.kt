package io.github.ngth1010v.mydailynote.feature.group

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ngth1010v.mydailynote.core.data.model.Action
import io.github.ngth1010v.mydailynote.core.ui.EntryBadge
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.groupName
import kotlinx.coroutines.delay

/**
 * Groups and their actions. Each group opens to show its actions and a "New action" row; any row can
 * be long-pressed and dragged: groups to reorder them, actions to reorder them or move them to another
 * group. [railPadding] keeps the snackbar clear of the rail; [bottomClearance] lets the last row
 * scroll above it. [onDragging] reports a running drag (horizontal paging should pause).
 */
@Composable
fun GroupScreen(
    vm: GroupViewModel,
    railPadding: PaddingValues,
    bottomClearance: Dp,
    onDragging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections by vm.sections.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val drag = remember(listState) { GroupDragState(listState) }
    var expanded by rememberSaveable(saver = idSetSaver) { mutableStateOf(emptySet<Long>()) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val edge = with(LocalDensity.current) { 64.dp.toPx() }

    val data = sections.orEmpty()
    val rows = drag.rows ?: buildRows(data, expanded)
    val defaultName = data.firstOrNull { it.group.isDefault }?.group?.let { groupName(it) }.orEmpty()

    LaunchedEffect(sections) { if (drag.awaitingSave) drag.onSaved() }
    // A save that changes nothing visible emits nothing: don't wait for it forever.
    LaunchedEffect(drag.awaitingSave) {
        if (drag.awaitingSave) {
            delay(1_500)
            drag.onSaved()
        }
    }
    LaunchedEffect(drag.isDragging) { onDragging(drag.isDragging) }
    // Scroll while the dragged row is held near the top or bottom edge.
    LaunchedEffect(drag.isDragging) {
        while (drag.isDragging) {
            val speed = drag.edgeScroll(edge)
            if (speed != 0f) {
                listState.scrollBy(speed)
                drag.afterScroll()
            }
            withFrameNanos {}
        }
    }
    // Holding an action over a closed group opens it.
    LaunchedEffect(drag.hoverGroup) {
        val id = drag.hoverGroup ?: return@LaunchedEffect
        delay(500)
        drag.open(id, data.firstOrNull { it.group.id == id }?.actions.orEmpty())
        expanded = expanded + id
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
    LaunchedEffect(vm) {
        vm.eventFlow.collect { e ->
            when (e) {
                is GroupEvent.GroupCreated -> {
                    expanded = expanded + e.id
                    // The new group is added near the end of the list.
                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                }
                is GroupEvent.GroupDeleted -> {
                    val r = snackbar.showSnackbar(
                        context.getString(R.string.group_deleted, defaultName),
                        context.getString(R.string.undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (r == SnackbarResult.ActionPerformed) vm.undoDeleteGroup(e.undo)
                }
                is GroupEvent.ActionDeleted -> {
                    val r = snackbar.showSnackbar(
                        context.getString(R.string.entry_deleted),
                        context.getString(R.string.undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (r == SnackbarResult.ActionPerformed) vm.undoDeleteAction(e.undo)
                }
                is GroupEvent.Merged -> snackbar.showSnackbar(context.getString(R.string.action_merged, e.into))
                GroupEvent.ActionExists -> snackbar.showSnackbar(context.getString(R.string.action_exists))
                is GroupEvent.Logged -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbar.showSnackbar(context.getString(R.string.action_logged, e.text))
                }
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomClearance),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drag) {
                    detectRowDrag(
                        onStart = { y ->
                            val current = drag.rows ?: buildRows(vm.sections.value.orEmpty(), expanded)
                            drag.start(y, current).also { ok ->
                                if (ok) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = drag::drag,
                        onEnd = {
                            when (val drop = drag.end(vm.sections.value.orEmpty(), expanded)) {
                                is GroupDragState.Drop.Groups -> vm.reorderGroups(drop.ids)
                                is GroupDragState.Drop.Actions -> {
                                    drop.openGroup?.let { expanded = expanded + it }
                                    vm.placeActions(drop.layout)
                                }
                                null -> {}
                            }
                        },
                        onCancel = drag::cancel,
                    )
                },
        ) {
            item(key = "title", contentType = "title") {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.nav_group), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.group_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(rows, key = { _, row -> row.key }, contentType = { _, row -> row::class }) { index, row ->
                val dragging = row.key == drag.draggingKey
                // The dragged row's card follows the finger; its place in the list (and the panel
                // behind it) stays put.
                val lift = if (dragging) {
                    Modifier.graphicsLayer {
                        translationY = drag.offsetOf(row.key)
                        shadowElevation = 3.dp.toPx()
                        shape = RoundedCornerShape(RowRadius)
                        clip = true
                    }
                } else Modifier
                val itemModifier = if (dragging) Modifier.zIndex(1f) else Modifier.animateItem()
                val first = rows.getOrNull(index - 1) is GroupRow.Header
                when (row) {
                    is GroupRow.Header -> GroupHeaderRow(
                        row,
                        hovered = drag.hoverGroup == row.group.id,
                        lifted = dragging,
                        onToggle = { expanded = if (row.expanded) expanded - row.group.id else expanded + row.group.id },
                        onEdit = { vm.editor = GroupEditor.EditGroup(row.group) },
                        cardModifier = lift,
                        modifier = itemModifier,
                    )
                    is GroupRow.Item -> PanelSlot(first = first, last = false, modifier = itemModifier) {
                        ActionRow(
                            row.action,
                            lifted = dragging,
                            onEdit = { vm.editor = GroupEditor.EditAction(row.action) },
                            onLog = { vm.log(row.action) },
                            modifier = lift,
                        )
                    }
                    is GroupRow.AddAction -> PanelSlot(first = first, last = true, modifier = itemModifier) {
                        AddActionRow(onClick = { vm.editor = GroupEditor.NewAction(row.groupId) })
                    }
                }
            }
            item(key = "addGroup", contentType = "addGroup") {
                AddGroupRow(onClick = { vm.editor = GroupEditor.NewGroup }, modifier = Modifier.animateItem())
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(railPadding).padding(bottom = 8.dp))
    }

    GroupEditorHost(vm)
}

private val idSetSaver = Saver<MutableState<Set<Long>>, LongArray>(
    save = { it.value.toLongArray() },
    restore = { mutableStateOf(it.toSet()) },
)

private val RowRadius = 16.dp
private val PanelRadius = 20.dp
private val GroupRowHeight = 72.dp
private val ActionRowHeight = 64.dp
private val ActionIndent = 20.dp

/** A row's own rounded background; every row (groups, actions, the add rows) is one. */
@Composable
private fun RowCard(
    height: Dp,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(RowRadius))
            .background(color)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Title and subtitle between a row's badge and its trailing button. */
@Composable
private fun RowScope.RowTexts(title: String, subtitle: String?, titleStyle: TextStyle, color: Color = Color.Unspecified) {
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Text(title, style = titleStyle, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The open group's panel behind its action rows: one slice per row, rounded at the panel's top and
 * bottom. The rows sit on it as their own cards.
 */
@Composable
private fun PanelSlot(first: Boolean, last: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(
        topStart = if (first) PanelRadius else 0.dp,
        topEnd = if (first) PanelRadius else 0.dp,
        bottomStart = if (last) PanelRadius else 0.dp,
        bottomEnd = if (last) PanelRadius else 0.dp,
    )
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = if (first) 4.dp else 0.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .padding(start = 8.dp + ActionIndent, end = 8.dp, top = if (first) 8.dp else 4.dp, bottom = if (last) 8.dp else 4.dp)
    ) { content() }
}

@Composable
private fun GroupHeaderRow(
    row: GroupRow.Header,
    hovered: Boolean,
    lifted: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    cardModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val group = row.group
    val colors = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(if (row.expanded) 180f else 0f, label = "chevron")
    Box(modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp)) {
        RowCard(
            GroupRowHeight,
            color = when {
                hovered -> colors.primaryContainer
                lifted -> colors.surfaceContainerHighest
                else -> colors.surfaceContainer
            },
            onClick = onEdit,
            modifier = cardModifier,
        ) {
            Badge(group.color, group.icon, 40.dp, 22.dp)
            RowTexts(
                groupName(group),
                group.description ?: pluralStringResource(R.plurals.actions_count, row.count, row.count),
                MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onToggle) {
                Icon(
                    Icons.Filled.ExpandMore,
                    stringResource(if (row.expanded) R.string.group_collapse else R.string.group_expand),
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    action: Action,
    lifted: Boolean,
    onEdit: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    RowCard(
        ActionRowHeight,
        color = if (lifted) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
        onClick = onEdit,
        modifier = modifier,
    ) {
        Badge(action.color, action.icon, 36.dp, 20.dp)
        RowTexts(action.text, stats(context, action), MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onLog) {
            Icon(Icons.Filled.AddCircleOutline, stringResource(R.string.action_log_now), tint = colors.primary)
        }
    }
}

/** Same size and layout as an action row: a "+" badge and the label. */
@Composable
private fun AddActionRow(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    RowCard(ActionRowHeight, color = colors.surfaceContainerHigh, onClick = onClick) {
        AddBadge(36.dp, 20.dp)
        RowTexts(stringResource(R.string.action_new), null, MaterialTheme.typography.bodyLarge, color = colors.primary)
    }
}

/** Same size and layout as a group row, after the last group. */
@Composable
private fun AddGroupRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 16.dp)) {
        RowCard(GroupRowHeight, color = colors.surfaceContainer, onClick = onClick) {
            AddBadge(40.dp, 22.dp)
            RowTexts(stringResource(R.string.group_add), null, MaterialTheme.typography.titleMedium, color = colors.primary)
        }
    }
}

@Composable
private fun AddBadge(size: Dp, iconSize: Dp) {
    val tint = MaterialTheme.colorScheme.primary
    Box(Modifier.size(size).background(tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Add, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
internal fun Badge(color: String?, icon: String?, size: Dp, iconSize: Dp) {
    val tint = EntryPalette.color(color)
    Box(Modifier.size(size).background(tint.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
        EntryBadge(tint, icon, size = iconSize)
    }
}

private fun stats(context: Context, action: Action): String {
    val last = action.lastAt ?: return context.getString(R.string.action_never)
    val times = context.resources.getQuantityString(R.plurals.times_count, action.count, action.count)
    val ago = DateUtils.getRelativeTimeSpanString(last.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
    return context.getString(R.string.action_last, times, ago)
}
