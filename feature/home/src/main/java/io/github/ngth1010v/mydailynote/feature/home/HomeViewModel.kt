package io.github.ngth1010v.mydailynote.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import io.github.ngth1010v.mydailynote.core.data.EntryRepository
import io.github.ngth1010v.mydailynote.core.data.model.Entry
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** One row of the Home list: an entry, or the label above a day's entries. */
sealed interface HomeItem {
    val key: String

    data class Row(val entry: Entry, val day: LocalDate) : HomeItem {
        override val key get() = "e${entry.id}"
    }

    data class Header(val day: LocalDate) : HomeItem {
        override val key get() = "d$day"
    }
}

sealed interface InputMode {
    data object Idle : InputMode
    data object Typing : InputMode
}

sealed interface HomeEvent {
    /** An entry was saved: scroll to the newest and confirm with a haptic tick. */
    data object Saved : HomeEvent
    data class Deleted(val id: Long) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val entries: EntryRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val items: Flow<PagingData<HomeItem>> = settingsRepo.settings
        .map { it.dayStartHour }
        .distinctUntilChanged()
        .flatMapLatest { dayStart ->
            entries.pagedNewestFirst().map { page ->
                page.map { HomeItem.Row(it, it.day(dayStart)) }
                    // Newest first, rendered bottom-up: a day's label goes after its oldest entry.
                    .insertSeparators { newer: HomeItem.Row?, older: HomeItem.Row? ->
                        if (newer != null && (older == null || older.day != newer.day)) HomeItem.Header(newer.day)
                        else null
                    }
            }
        }
        .cachedIn(viewModelScope)

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
            entries.add(value, EntrySource.TEXT)
            events.send(HomeEvent.Saved)
        }
    }

    /** One-tap re-log of a previous entry, keeping its color and icon. */
    fun relog(s: Suggestion) {
        text = ""
        mode = InputMode.Idle
        viewModelScope.launch {
            entries.add(s.text, EntrySource.TEXT, s.color, s.icon)
            events.send(HomeEvent.Saved)
        }
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
        viewModelScope.launch { entries.update(entry) }
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
