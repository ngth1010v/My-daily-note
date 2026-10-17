package io.github.ngth1010v.mydailynote.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.flatMap
import androidx.paging.insertSeparators
import androidx.paging.map
import io.github.ngth1010v.mydailynote.core.data.EntryRepository
import io.github.ngth1010v.mydailynote.core.data.GroupRepository
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoGroup
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoStyle
import io.github.ngth1010v.mydailynote.core.data.autostyle.Style
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.Recommendation
import io.github.ngth1010v.mydailynote.core.data.model.recommend
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.data.model.Suggestion
import io.github.ngth1010v.mydailynote.core.data.model.day
import io.github.ngth1010v.mydailynote.core.data.settings.AppSettings
import io.github.ngth1010v.mydailynote.core.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One row of the Home list: an entry, or the label above a day's entries. */
sealed interface HomeItem {
    val key: String

    data class Row(val entry: Entry, val day: LocalDate) : HomeItem {
        override val key get() = "e${entry.id}"
    }

    data class Header(val day: LocalDate) : HomeItem {
        override val key get() = "d$day"
    }

    /** A guessed action for [day] (today), not logged yet. */
    data class Recommend(val rec: Recommendation, val day: LocalDate) : HomeItem {
        override val key get() = "r${rec.key}"
    }
}

sealed interface InputMode {
    data object Idle : InputMode
    data object Typing : InputMode
}

sealed interface HomeEvent {
    /** Entry [id] was saved: scroll to it and confirm with a haptic tick. */
    data class Saved(val id: Long) : HomeEvent
    data class Deleted(val id: Long) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val entries: EntryRepository,
    private val groups: GroupRepository,
    private val settingsRepo: SettingsRepository,
    /** Slow the first time (loads the model): call off the main thread. */
    private val autoStyle: () -> AutoStyle,
    /** Slow the first time (loads the model): call off the main thread. */
    private val autoGroup: () -> AutoGroup,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val rows: Flow<PagingData<HomeItem.Row>> = settingsRepo.settings
        .map { it.dayStartHour }
        .distinctUntilChanged()
        .flatMapLatest { dayStart ->
            entries.pagedNewestFirst().map { page -> page.map { HomeItem.Row(it, it.day(dayStart)) } }
        }
        .cachedIn(viewModelScope)

    /** Today's recommendations, oldest first; recomputed when entries change or the day rolls over. */
    private val recommendations: Flow<List<HomeItem.Recommend>> = settingsRepo.settings
        .map { if (it.recommendActions) it.dayStartHour to it.recommend else null }
        .distinctUntilChanged()
        .flatMapLatest { tuning ->
            val (dayStart, options) = tuning ?: return@flatMapLatest flowOf(emptyList())
            flow { while (true) { emit(computeToday(dayStart)); delay(60_000) } }
                .distinctUntilChanged()
                .flatMapLatest { today ->
                    val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())
                    val from = today.minusDays(options.historyDays + 1L).atStartOfDay().toInstant(offset)
                    combine(entries.since(from), settingsRepo.dismissedRecommendations(today)) { list, dismissed ->
                        recommend(list, today, dayStart, offset, dismissed, options).map { HomeItem.Recommend(it, today) }
                    }
                }
        }
        .flowOn(Dispatchers.Default)

    val items: Flow<PagingData<HomeItem>> = combine(rows, recommendations) { page, recs -> page.withLabels(recs) }

    var mode by mutableStateOf<InputMode>(InputMode.Idle)
        private set

    /** What the user types. */
    var text by mutableStateOf("")
        private set

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    val suggestions: StateFlow<List<Suggestion>> = snapshotFlow { mode to text }
        .debounce(80)
        .flatMapLatest { (m, t) ->
            if (m != InputMode.Typing) flowOf(emptyList())
            else entries.suggestions(t).map { list -> list.filterNot { it.text == t.trim() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Entry open in the full-screen edit sub screen, or null. */
    var detail by mutableStateOf<Entry?>(null)

    var searchOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set

    val searchResults: StateFlow<List<Entry>> = snapshotFlow { query }
        .debounce(150)
        .flatMapLatest { q -> entries.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) { entries.purgeDeleted() }
        // Every action needs a color, an icon and a group: style and group old, new and imported ones
        // once they appear (grouping goes by the icon, so it comes second).
        viewModelScope.launch(Dispatchers.Default) {
            combine(entries.count, groups.pendingCount) { count, pending -> count to pending }
                .debounce(500)
                .collect { (_, pending) ->
                    entries.fillMissingStyles(autoStyle())
                    if (pending > 0) {
                        val s = settingsRepo.current()
                        groups.assignNewActions(autoGroup, autoStyle, s.autoGroupActions, s.autoCreateGroups)
                    }
                }
        }
    }

    fun onTextChange(value: String) {
        text = value
    }

    fun startTyping(prefill: String = "") {
        if (prefill.isNotEmpty()) text = prefill
        mode = InputMode.Typing
    }

    fun cancelInput() {
        text = ""
        mode = InputMode.Idle
    }

    fun save() {
        val value = text.trim()
        if (value.isEmpty()) return
        text = ""
        mode = InputMode.Idle
        viewModelScope.launch {
            // The same text as a past action (ignoring case and accents) logs that action again. One
            // made on the Group screen but never logged keeps its look; a new one gets a color and icon
            // picked from how it relates to past ones.
            val id = entries.addAsExisting(value, EntrySource.TEXT) ?: run {
                val known = groups.action(value)?.takeIf { it.color != null && it.icon != null }
                val style = known?.let { Style(it.color!!, it.icon!!) }
                    ?: withContext(Dispatchers.Default) { autoStyle().pick(value, entries.styleHistory()) }
                entries.add(known?.text ?: value, EntrySource.TEXT, style.color, style.icon)
            }
            events.send(HomeEvent.Saved(id))
        }
    }

    /** One-tap re-log of a previous action, copying its latest entry. */
    fun relog(s: Suggestion) {
        text = ""
        mode = InputMode.Idle
        viewModelScope.launch {
            val id = entries.addAsExisting(s.text, EntrySource.TEXT) ?: entries.add(s.text, EntrySource.TEXT, s.color, s.icon)
            events.send(HomeEvent.Saved(id))
        }
    }

    /** Logs a recommendation at its time, or now if that is still ahead. */
    fun confirm(item: HomeItem.Recommend) {
        val r = item.rec
        viewModelScope.launch {
            // Logged now, a later habit wouldn't count as done: hide it for today either way.
            settingsRepo.dismissRecommendation(item.day, r.key)
            val at = minOf(r.at, Instant.now())
            val id = entries.addAsExisting(r.text, EntrySource.TEXT, at) ?: entries.add(r.text, EntrySource.TEXT, r.color, r.icon, at)
            events.send(HomeEvent.Saved(id))
        }
    }

    fun dismiss(item: HomeItem.Recommend) {
        viewModelScope.launch { settingsRepo.dismissRecommendation(item.day, item.rec.key) }
    }

    // Entry actions

    fun delete(entry: Entry) {
        viewModelScope.launch {
            entries.delete(entry.id)
            events.send(HomeEvent.Deleted(entry.id))
        }
    }

    fun undoDelete(id: Long) {
        viewModelScope.launch { entries.undoDelete(id) }
    }

    fun update(entry: Entry) {
        if (entry.text.isBlank()) return
        viewModelScope.launch {
            // Edited to a new action's text: that action starts in the group the entry was in.
            val old = entries.get(entry.id)
            val oldGroup = old?.let { groups.action(it.text)?.groupId }
            if (oldGroup != null) groups.ensureAction(entry.text, entry.colorOverride, entry.iconOverride, oldGroup)
            entries.update(entry)
        }
    }

    fun changeTime(entry: Entry, at: Instant) {
        viewModelScope.launch { entries.changeTime(entry, at) }
    }

    // Search

    fun openSearch() {
        searchOpen = true
    }

    fun closeSearch() {
        searchOpen = false
        query = ""
    }

    fun onQueryChange(value: String) {
        query = value
    }
}

/**
 * Adds day labels and today's [recs] to rows listed newest first (rendered bottom-up): each
 * recommendation goes among today's rows by time, and a day's label after its oldest row.
 */
private fun PagingData<HomeItem.Row>.withLabels(recs: List<HomeItem.Recommend>): PagingData<HomeItem> =
    map { listOf<HomeItem>(it) }
        .insertSeparators { newer: List<HomeItem>?, older: List<HomeItem>? ->
            gap(newer?.first() as HomeItem.Row?, older?.first() as HomeItem.Row?, recs).ifEmpty { null }
        }
        .flatMap { it }

private fun gap(newer: HomeItem.Row?, older: HomeItem.Row?, recs: List<HomeItem.Recommend>): List<HomeItem> {
    val today = recs.firstOrNull()?.day
    val out = ArrayList<HomeItem>()
    if (today != null && (newer == null || newer.day == today)) {
        val before = newer?.entry?.achievedAt
        val after = older?.takeIf { it.day == today }?.entry?.achievedAt
        recs.asReversed().filterTo(out) { (before == null || it.rec.at < before) && (after == null || it.rec.at >= after) }
    }
    if (newer != null && (older == null || older.day != newer.day)) out += HomeItem.Header(newer.day)
    // No row today yet: today's label goes above its recommendations.
    else if (newer == null && out.isNotEmpty() && older?.day != today) out += HomeItem.Header(today!!)
    return out
}
