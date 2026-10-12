package io.github.ngth1010v.mydailynote.feature.home

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.dayOf
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.ui.EntryBadge
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import io.github.ngth1010v.mydailynote.core.ui.Formatters
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.languageName
import io.github.ngth1010v.mydailynote.speech.api.SpeechError
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Requests from outside the screen: widget, quick settings tile, app shortcut, share sheet. */
sealed interface HomeLaunch {
    data object Voice : HomeLaunch
    data object Text : HomeLaunch
    data class Share(val text: String) : HomeLaunch
}

@Composable
fun HomeScreen(
    controller: HomeController,
    contentPadding: PaddingValues,
    launch: HomeLaunch?,
    onLaunchHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = controller.vm
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val formatters = remember(configuration, settings.timeFormat) { Formatters(context, settings.timeFormat) }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val today = rememberToday(settings.dayStartHour)
    val view = LocalView.current
    val listState = rememberLazyListState()
    val items = vm.items.collectAsLazyPagingItems()
    var scrollToNewest by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EntryEditor?>(null) }

    val deletedText = stringResource(R.string.entry_deleted)
    val undoText = stringResource(R.string.undo)
    val messages = mapOf(
        SpeechError.NO_MATCH to stringResource(R.string.voice_no_match),
        SpeechError.AUDIO to stringResource(R.string.voice_error_audio),
        SpeechError.NOT_AVAILABLE to stringResource(R.string.voice_error_not_available),
        SpeechError.ENGINE to stringResource(R.string.voice_error_engine),
    )
    val langSwitched = stringResource(R.string.voice_language_switched, "%s")
    val langNames = VoiceLanguage.entries.associateWith { languageName(it) }

    LaunchedEffect(vm) {
        vm.eventFlow.collect { event ->
            when (event) {
                HomeEvent.Saved -> {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                        else HapticFeedbackConstants.KEYBOARD_TAP
                    )
                    scrollToNewest = true
                    listState.animateScrollToItem(0)
                }
                is HomeEvent.Deleted -> {
                    val r = controller.snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) vm.undoDelete(event.id)
                }
                is HomeEvent.VoiceFailed -> messages[event.error]?.let { controller.snackbar.showSnackbar(it) }
                is HomeEvent.VoiceLanguageChanged ->
                    controller.snackbar.showSnackbar(langSwitched.replace("%s", langNames.getValue(event.language)))
            }
        }
    }
    // The new row arrives with the next page refresh; keep the newest visible once it does.
    LaunchedEffect(items.itemCount) {
        if (scrollToNewest) {
            listState.animateScrollToItem(0)
            scrollToNewest = false
        }
    }
    LaunchedEffect(launch) {
        when (launch) {
            null -> return@LaunchedEffect
            HomeLaunch.Voice -> controller.onMic()
            HomeLaunch.Text -> vm.startTyping()
            is HomeLaunch.Share -> vm.startTyping(launch.text)
        }
        onLaunchHandled()
    }
    BackHandler(enabled = vm.mode != InputMode.Idle) { vm.cancelInput() }
    BackHandler(enabled = vm.searchOpen && vm.mode == InputMode.Idle) { vm.closeSearch() }

    val layoutDirection = LocalLayoutDirection.current
    val startPad = contentPadding.calculateStartPadding(layoutDirection)
    val endPad = contentPadding.calculateEndPadding(layoutDirection)

    val onEntryAction: (EntryEditor) -> Unit = { editor = it }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (vm.searchOpen) {
                    SearchContent(vm, formatters, settings.defaultColor, dark, startPad, endPad, onEntryAction)
                } else {
                    val empty = items.loadState.refresh is LoadState.NotLoading && items.itemCount == 0
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(start = 16.dp + startPad, end = 8.dp + endPad, top = 64.dp, bottom = 8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            count = items.itemCount,
                            key = items.itemKey { it.key },
                            contentType = items.itemContentType { if (it is HomeItem.Row) 0 else 1 },
                        ) { index ->
                            when (val item = items[index]) {
                                is HomeItem.Row -> EntryRow(item.entry, formatters, settings.defaultColor, dark, onEntryAction, vm::delete)
                                is HomeItem.Header -> DayHeader(formatters.day(item.day, today))
                                null -> Spacer(Modifier.height(40.dp))
                            }
                        }
                    }
                    if (empty) EmptyState(Modifier.align(Alignment.Center).padding(start = startPad, end = endPad))
                    // Floating "sticky" label: the day of the top-most visible row, shown once that
                    // day's own header has scrolled out of view.
                    val topDay by remember {
                        derivedStateOf {
                            val top = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf null
                            if (top >= items.itemCount) null
                            else (items.peek(top) as? HomeItem.Row)?.day
                        }
                    }
                    val surface = MaterialTheme.colorScheme.surface
                    Row(
                        Modifier
                            .fillMaxWidth()
                            // Rows fade out under the label and search button instead of colliding with them.
                            .background(Brush.verticalGradient(0.7f to surface, 1f to surface.copy(alpha = 0f)))
                            .padding(start = 8.dp + startPad, end = endPad, top = 8.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        topDay?.let { DayPill(formatters.day(it, today)) }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = vm::openSearch) {
                            Icon(Icons.Filled.Search, stringResource(R.string.action_search))
                        }
                    }
                }
            }
            InputPanel(
                vm, settings.defaultColor, dark,
                Modifier.padding(start = startPad, end = endPad),
            )
        }
        SnackbarHost(
            controller.snackbar,
            Modifier.align(Alignment.BottomCenter).padding(start = startPad, end = endPad, bottom = 8.dp),
        )
    }

    EntryEditorHost(
        editor = editor,
        use24h = formatters.use24h,
        defaultColor = settings.defaultColor,
        dark = dark,
        onUpdate = vm::update,
        onChangeTime = vm::changeTime,
        onClose = { editor = null },
    )
    MicRationaleDialog(controller)
    ModelPromptSheet(vm)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: Entry,
    formatters: Formatters,
    defaultColor: String,
    dark: Boolean,
    onEditor: (EntryEditor) -> Unit,
    onDelete: (Entry) -> Unit,
    showDate: Boolean = false,
) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val local = entry.achievedAt.atOffset(entry.zoneOffset)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { expanded = !expanded },
                    onLongClick = { menu = true },
                )
                .padding(vertical = 8.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                if (showDate) formatters.date(local.toLocalDate()) + "\n" + formatters.time(local) else formatters.time(local),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(if (showDate) 100.dp else if (formatters.use24h) 52.dp else 76.dp).padding(top = 2.dp),
            )
            EntryBadge(
                EntryPalette.color(entry.color, defaultColor, dark),
                entry.icon,
                Modifier.padding(top = 1.dp, end = 10.dp),
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (expanded && entry.source == EntrySource.VOICE) {
                        Icon(
                            Icons.Filled.Mic, stringResource(R.string.voice_source),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp).size(14.dp),
                        )
                    }
                }
                if (expanded && !entry.description.isNullOrBlank()) {
                    Text(
                        entry.description.orEmpty(),
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
    dark: Boolean,
    startPad: androidx.compose.ui.unit.Dp,
    endPad: androidx.compose.ui.unit.Dp,
    onEditor: (EntryEditor) -> Unit,
) {
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize().padding(start = startPad, end = endPad)) {
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
                EntryRow(entry, formatters, defaultColor, dark, onEditor, vm::delete, showDate = true)
            }
        }
    }
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
