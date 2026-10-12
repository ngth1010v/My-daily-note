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
import io.github.ngth1010v.mydailynote.speech.api.ModelManager
import io.github.ngth1010v.mydailynote.speech.api.ModelRegistry
import io.github.ngth1010v.mydailynote.speech.api.ModelSpec
import io.github.ngth1010v.mydailynote.speech.api.ModelState
import io.github.ngth1010v.mydailynote.speech.api.SpeechEngineIds
import io.github.ngth1010v.mydailynote.speech.api.SpeechError
import io.github.ngth1010v.mydailynote.speech.api.SpeechEvent
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import io.github.ngth1010v.mydailynote.speech.api.VoicePipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

enum class VoicePhase { LISTENING, REFINING, DONE }

sealed interface InputMode {
    data object Idle : InputMode
    data object Typing : InputMode
    data class Voice(val phase: VoicePhase) : InputMode
}

sealed interface HomeEvent {
    /** An entry was saved: scroll to the newest and confirm with a haptic tick. */
    data object Saved : HomeEvent
    data class Deleted(val id: Long) : HomeEvent
    data class VoiceFailed(val error: SpeechError) : HomeEvent
    data class VoiceLanguageChanged(val language: VoiceLanguage) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val entries: EntryRepository,
    private val settingsRepo: SettingsRepository,
    private val pipeline: VoicePipeline,
    private val models: ModelManager,
    /** Current app UI language tag (per-app locale or system). */
    private val appLanguage: () -> String,
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

    /** Editable text: what the user types, or the recognized sentence after voice. */
    var text by mutableStateOf("")
        private set

    /** Live words while listening (shown in a lighter style). */
    var partial by mutableStateOf("")
        private set

    var level by mutableStateOf(0f)
        private set

    /** Non-null while asking to download the model for this language. */
    var modelPrompt by mutableStateOf<ModelSpec?>(null)
        private set

    val modelStates: StateFlow<Map<String, ModelState>> = models.states

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    val suggestions: StateFlow<List<Suggestion>> = snapshotFlow { mode to text }
        .debounce(80)
        .flatMapLatest { (m, t) ->
            if (m != InputMode.Typing) flowOf(emptyList())
            else entries.suggestions(t).map { list -> list.filterNot { it.text == t.trim() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var searchOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set

    val searchResults: StateFlow<List<Entry>> = snapshotFlow { query }
        .debounce(150)
        .flatMapLatest { q -> entries.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var voiceJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) { entries.purgeDeleted() }
        preloadVoice()
    }

    /** Loads the speech model in the background so the first mic tap starts instantly. */
    private fun preloadVoice() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(800) // let the first frames render first
            combine(settingsRepo.settings, models.states) { s, _ -> s }
                .map { s -> Triple(s.speechEngine, voiceLanguage(s), refineModel(s)) }
                .distinctUntilChanged()
                .collect { (engine, lang, refine) ->
                    if (pipeline.engine(engine).isAvailable(lang)) {
                        runCatching { pipeline.prepare(engine, lang, refine) }
                    }
                }
        }
    }

    fun voiceLanguage(s: AppSettings = settings.value): VoiceLanguage =
        VoiceLanguage.fromTag(s.voiceLanguage) ?: VoiceLanguage.fromTag(appLanguage()) ?: VoiceLanguage.EN

    private fun refineModel(s: AppSettings): String? =
        s.whisperModelId.takeIf { s.highAccuracy && pipeline.refinerAvailable(it) }

    fun onTextChange(value: String) {
        text = value
    }

    fun startTyping(prefill: String = "") {
        cancelVoice()
        if (prefill.isNotEmpty()) text = prefill
        mode = InputMode.Typing
    }

    /** Call only with RECORD_AUDIO granted. */
    fun startVoice() {
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            val lang = voiceLanguage(s)
            val engine = pipeline.engine(s.speechEngine)
            if (engine.id == SpeechEngineIds.VOSK && !engine.isAvailable(lang)) {
                modelPrompt = ModelRegistry.vosk(lang)
                return@launch
            }
            listen(s, lang)
        }
    }

    private fun listen(s: AppSettings, lang: VoiceLanguage) {
        voiceJob?.cancel()
        text = ""
        partial = ""
        level = 0f
        mode = InputMode.Voice(VoicePhase.LISTENING)
        voiceJob = viewModelScope.launch {
            var finished = false
            pipeline.start(s.speechEngine, lang, s.autoStopMs, refineModel(s)).collect { e ->
                when (e) {
                    SpeechEvent.Listening -> {}
                    is SpeechEvent.Level -> level = e.level
                    is SpeechEvent.Partial -> partial = e.text
                    is SpeechEvent.Final -> {
                        finished = true
                        text = e.text
                        partial = ""
                        level = 0f
                        mode = InputMode.Voice(VoicePhase.DONE)
                    }
                    SpeechEvent.Refining -> mode = InputMode.Voice(VoicePhase.REFINING)
                    is SpeechEvent.Refined -> text = e.text
                    is SpeechEvent.Error -> {
                        finished = true
                        level = 0f
                        if (e.error == SpeechError.NO_MODEL) modelPrompt = ModelRegistry.vosk(lang)
                        else events.send(HomeEvent.VoiceFailed(e.error))
                        // Keep what was heard so far, editable; otherwise leave voice mode.
                        if (partial.isNotBlank()) {
                            text = partial
                            partial = ""
                            mode = InputMode.Voice(VoicePhase.DONE)
                        } else mode = InputMode.Idle
                    }
                }
            }
            if (mode == InputMode.Voice(VoicePhase.REFINING)) mode = InputMode.Voice(VoicePhase.DONE)
            if (!finished && mode is InputMode.Voice) mode = InputMode.Idle
        }
    }

    /** Ends recording; the final result still arrives. */
    fun stopVoice() = pipeline.stop()

    private fun cancelVoice() {
        voiceJob?.cancel()
        voiceJob = null
        partial = ""
        level = 0f
    }

    fun cancelInput() {
        cancelVoice()
        text = ""
        mode = InputMode.Idle
    }

    fun save() {
        val value = text.trim()
        if (value.isEmpty()) return
        val source = if (mode is InputMode.Voice) EntrySource.VOICE else EntrySource.TEXT
        cancelVoice()
        text = ""
        mode = InputMode.Idle
        viewModelScope.launch {
            entries.add(value, source)
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

    /** Long-press on the mic: switch between the voice languages. */
    fun cycleVoiceLanguage() {
        viewModelScope.launch {
            val next = VoiceLanguage.entries.let { all -> all[(all.indexOf(voiceLanguage()) + 1) % all.size] }
            settingsRepo.update { it.copy(voiceLanguage = next.tag) }
            events.send(HomeEvent.VoiceLanguageChanged(next))
        }
    }

    // Model prompt

    fun downloadModel(spec: ModelSpec) = models.download(spec.id)

    fun cancelModelDownload(spec: ModelSpec) = models.cancel(spec.id)

    fun importModel(spec: ModelSpec, open: () -> java.io.InputStream?) {
        viewModelScope.launch {
            open()?.use { models.import(spec.id, it) }
        }
    }

    fun dismissModelPrompt() {
        modelPrompt = null
    }

    /** Called when the prompted model finished installing: start listening right away. */
    fun onModelReady() {
        modelPrompt = null
        startVoice()
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

    override fun onCleared() {
        cancelVoice()
    }
}
