package io.github.ngth1010v.mydailynote.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ngth1010v.mydailynote.core.data.EntryRepository
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoStyle
import io.github.ngth1010v.mydailynote.core.data.backup.BackupFormat
import io.github.ngth1010v.mydailynote.core.data.backup.BackupRepository
import io.github.ngth1010v.mydailynote.core.data.settings.AppSettings
import io.github.ngth1010v.mydailynote.core.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/** Static facts about this build, provided by the app module. */
data class AppInfo(
    val versionName: String,
    val repoUrl: String,
    /** "owner/repo" for the GitHub releases API. */
    val repoSlug: String,
)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object Latest : UpdateStatus
    data class Available(val version: String, val url: String) : UpdateStatus
    data object Failed : UpdateStatus
}

sealed interface SettingsMessage {
    data class Exported(val count: Int) : SettingsMessage
    data object ExportFailed : SettingsMessage
    data class Imported(val imported: Int, val skipped: Int) : SettingsMessage
    data object ImportFailed : SettingsMessage
    data object Cleared : SettingsMessage
    data class Restyled(val count: Int) : SettingsMessage
}

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val entries: EntryRepository,
    private val backup: BackupRepository,
    /** Slow the first time (loads the model): call off the main thread. */
    private val autoStyle: () -> AutoStyle,
    val appInfo: AppInfo,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val entryCount: StateFlow<Int> = entries.count.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _update = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val update: StateFlow<UpdateStatus> = _update.asStateFlow()

    private val messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messageFlow: Flow<SettingsMessage> = messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (settingsRepo.settings.first().updateCheck) checkForUpdate()
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    /** "" = follow the system. */
    val appLanguage: String get() = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    fun setAppLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun setUpdateCheck(enabled: Boolean) {
        update { it.copy(updateCheck = enabled) }
        if (enabled) checkForUpdate() else _update.value = UpdateStatus.Idle
    }

    fun checkForUpdate() {
        if (_update.value == UpdateStatus.Checking) return
        _update.value = UpdateStatus.Checking
        viewModelScope.launch {
            _update.value = runCatching { UpdateChecker.latest(appInfo.repoSlug) }.fold(
                onSuccess = { release ->
                    if (release != null && UpdateChecker.isNewer(release.version, appInfo.versionName)) {
                        UpdateStatus.Available(release.version, release.url)
                    } else UpdateStatus.Latest
                },
                onFailure = { UpdateStatus.Failed },
            )
        }
    }

    // Data

    fun export(format: BackupFormat, open: () -> OutputStream?) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { open()?.use { backup.export(format, it) } ?: error("no stream") }
            }
            messages.send(result.fold({ SettingsMessage.Exported(it) }, { SettingsMessage.ExportFailed }))
        }
    }

    fun import(open: () -> InputStream?) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { open()?.use { backup.import(it) } ?: error("no stream") }
            }
            messages.send(
                result.fold({ SettingsMessage.Imported(it.imported, it.skippedDuplicates) }, { SettingsMessage.ImportFailed })
            )
        }
    }

    fun restyleAll() {
        viewModelScope.launch {
            val count = withContext(Dispatchers.Default) { entries.restyleAll(autoStyle()) }
            messages.send(SettingsMessage.Restyled(count))
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            entries.clearAll()
            messages.send(SettingsMessage.Cleared)
        }
    }
}
