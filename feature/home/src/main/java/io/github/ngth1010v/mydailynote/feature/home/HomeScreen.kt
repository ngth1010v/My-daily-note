package io.github.ngth1010v.mydailynote.feature.home

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.dayOf
import io.github.ngth1010v.mydailynote.core.ui.EntryBadge
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import io.github.ngth1010v.mydailynote.core.ui.Formatters
import io.github.ngth1010v.mydailynote.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Requests from outside the screen: widget, app shortcut, share sheet. */
sealed interface HomeLaunch {
    data object Text : HomeLaunch
    data class Share(val text: String) : HomeLaunch
}

@Composable
fun HomeScreen(
    controller: HomeController,
    /** Keeps the bottom input panel and snackbar clear of the floating rail; the list stays full width. */
    railPadding: PaddingValues,
    launch: HomeLaunch?,
    onLaunchHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = controller.vm
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val formatters = remember(configuration, settings.timeFormat) { Formatters(context, settings.timeFormat) }
    val today = rememberToday(settings.dayStartHour)
    val view = LocalView.current
    // List index 0 is the scroll-room spacer under the newest row; entries start at index 1.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    val items = vm.items.collectAsLazyPagingItems()
    var scrollToNewest by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EntryEditor?>(null) }

    val deletedText = stringResource(R.string.entry_deleted)
    val undoText = stringResource(R.string.undo)

    LaunchedEffect(vm) {
        vm.eventFlow.collect { event ->
            when (event) {
                HomeEvent.Saved -> {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                        else HapticFeedbackConstants.KEYBOARD_TAP
                    )
                    scrollToNewest = true
                    listState.animateScrollToItem(1)
                }
                is HomeEvent.Deleted -> {
                    val r = controller.snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) vm.undoDelete(event.id)
                }
            }
        }
    }
    // The new row arrives with the next page refresh; keep the newest visible once it does.
    LaunchedEffect(items.itemCount) {
        if (scrollToNewest) {
            listState.animateScrollToItem(1)
            scrollToNewest = false
        }
    }
    // Height of the spacer under the newest row: just enough to scroll today's header up to the top edge.
    val bottomPadPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    // Saveable: the pager disposes Home when swiped away, and the restored scroll position relies on it.
    var todayRoomPx by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(listState, items, today) {
        snapshotFlow { todayRoom(listState, items, today, bottomPadPx) }
            .filterNotNull()
            .collect { room ->
                val atRest = todayRoomPx == 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                todayRoomPx = room
                // Keep the newest row at the bottom when the spacer first gets a height. Requested (not
                // scrolled) so it lands in the same measure pass as the new spacer height.
                if (atRest && room > 0) listState.requestScrollToItem(1)
            }
    }
    LaunchedEffect(launch) {
        when (launch) {
            null -> return@LaunchedEffect
            HomeLaunch.Text -> vm.startTyping()
            is HomeLaunch.Share -> vm.startTyping(launch.text)
        }
        onLaunchHandled()
    }
    BackHandler(enabled = vm.detail != null) { vm.detail = null }
    BackHandler(enabled = vm.mode != InputMode.Idle) { vm.cancelInput() }
    BackHandler(enabled = vm.searchOpen && vm.mode == InputMode.Idle) { vm.closeSearch() }

    val layoutDirection = LocalLayoutDirection.current
    val startPad = railPadding.calculateStartPadding(layoutDirection)
    val endPad = railPadding.calculateEndPadding(layoutDirection)

    val onEntryAction: (EntryEditor) -> Unit = { editor = it }
    val onOpen: (Entry) -> Unit = { vm.detail = it }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (vm.searchOpen) {
                    SearchContent(vm, formatters, settings.defaultColor, onEntryAction, onOpen)
                } else {
                    val empty = items.loadState.refresh is LoadState.NotLoading && items.itemCount == 0
                    // Only lay the list out once rows exist: measuring it empty would clamp the saved
                    // scroll position (index 1) onto the spacer and open with today at the top.
                    if (items.itemCount > 0) LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "today-room", contentType = 2) {
                            Spacer(Modifier.height(with(LocalDensity.current) { todayRoomPx.toDp() }))
                        }
                        items(
                            count = items.itemCount,
                            key = items.itemKey { it.key },
                            contentType = items.itemContentType { if (it is HomeItem.Row) 0 else 1 },
                        ) { index ->
                            when (val item = items[index]) {
                                is HomeItem.Row -> EntryRow(item.entry, formatters, settings.defaultColor, onEntryAction, onOpen, vm::delete)
                                is HomeItem.Header -> DayHeader(formatters.day(item.day, today))
                                null -> Spacer(Modifier.height(40.dp))
                            }
                        }
                    }
                    if (empty) EmptyState(Modifier.align(Alignment.Center))
                    // Floating "sticky" label: the day of the top-most visible row, shown once that
                    // day's own header has scrolled out of view.
                    val topDay by remember {
                        derivedStateOf {
                            val top = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf null) - 1
                            if (top !in 0 until items.itemCount) null
                            else (items.peek(top) as? HomeItem.Row)?.day
                        }
                    }
                    // No backdrop: rows scroll all the way to the top edge, under the pill and search button.
                    Row(
                        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        topDay?.let { DayPill(formatters.day(it, today)) }
                        Spacer(Modifier.weight(1f))
                        Surface(
                            onClick = vm::openSearch,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Search, stringResource(R.string.action_search), Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            InputPanel(
                vm, settings.defaultColor,
                Modifier.padding(start = startPad, end = endPad),
            )
        }
        SnackbarHost(
            controller.snackbar,
            Modifier.align(Alignment.BottomCenter).padding(start = startPad, end = endPad, bottom = 8.dp),
        )
        vm.detail?.let { entry ->
            EntryDetailScreen(
                entry, formatters, settings.defaultColor,
                onSave = vm::update,
                onDelete = vm::delete,
                onClose = { vm.detail = null },
            )
        }
    }

    EntryEditorHost(
        editor = editor,
        use24h = formatters.use24h,
        defaultColor = settings.defaultColor,
        onUpdate = vm::update,
        onChangeTime = vm::changeTime,
        onClose = { editor = null },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: Entry,
    formatters: Formatters,
    defaultColor: String,
    onEditor: (EntryEditor) -> Unit,
    onOpen: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
    showDate: Boolean = false,
) {
    var menu by remember { mutableStateOf(false) }
    val local = entry.achievedAt.atOffset(entry.zoneOffset)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onOpen(entry) },
                    onLongClick = { menu = true },
                )
                .padding(vertical = 8.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = EntryPalette.color(entry.color, defaultColor)
            Box(
                Modifier.padding(end = 12.dp).size(40.dp).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                EntryBadge(color, entry.icon, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(entry.text, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                Text(
                    if (showDate) formatters.date(local.toLocalDate()) + " · " + formatters.time24(local) else formatters.time24(local),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!entry.description.isNullOrBlank()) {
                    Text(
                        entry.description.orEmpty(),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        EntryMenu(
            expanded = menu,
            onDismiss = { menu = false },
            onEdit = { onEditor(EntryEditor.Text(entry)) },
            onTime = { onEditor(EntryEditor.Time(entry)) },
            onStyle = { onEditor(EntryEditor.Style(entry)) },
            onDelete = { onDelete(entry) },
        )
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun DayPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SearchContent(
    vm: HomeViewModel,
    formatters: Formatters,
    defaultColor: String,
    onEditor: (EntryEditor) -> Unit,
    onOpen: (Entry) -> Unit,
) {
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize()) {
        TextField(
            value = vm.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                IconButton(onClick = vm::closeSearch) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_close_search))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(8.dp).focusRequester(focus),
        )
        if (vm.query.isNotBlank() && results.isEmpty()) {
            Text(
                stringResource(R.string.search_no_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(results, key = { it.id }) { entry ->
                EntryRow(entry, formatters, defaultColor, onEditor, onOpen, vm::delete, showDate = true)
            }
        }
    }
}

/**
 * Spacer height (px) that lets today's header scroll up to the top edge: the viewport minus today's
 * section. 0 when there is no today section or it already fills the screen; null when it can't be
 * measured right now (scrolled out of view), so the last value is kept.
 */
private fun todayRoom(state: LazyListState, items: LazyPagingItems<HomeItem>, today: LocalDate, bottomPadPx: Int): Int? {
    if (items.itemCount == 0) return 0
    val header = (0 until items.itemCount).firstOrNull { items.peek(it) is HomeItem.Header } ?: return null
    if ((items.peek(header) as HomeItem.Header).day != today) return 0
    val info = state.layoutInfo
    // List indices are paging indices + 1 (index 0 is the spacer).
    val section = info.visibleItemsInfo.filter { it.index in 1..header + 1 }
    if (section.size != header + 1) return if (section.any { it.index == 1 }) 0 else null
    return (info.viewportSize.height - bottomPadPx - section.sumOf { it.size }).coerceAtLeast(0)
}

/** Today's date under the "day starts at" rule; updates when the day rolls over. */
@Composable
private fun rememberToday(dayStartHour: Int): LocalDate {
    val today by produceState(computeToday(dayStartHour), dayStartHour) {
        while (true) {
            value = computeToday(dayStartHour)
            delay(60_000)
        }
    }
    return today
}

private fun computeToday(dayStartHour: Int): LocalDate {
    val now = Instant.now()
    return dayOf(now, ZoneId.systemDefault().rules.getOffset(now), dayStartHour)
}
