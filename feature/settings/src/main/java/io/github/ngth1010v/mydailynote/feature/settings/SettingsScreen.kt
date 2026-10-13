package io.github.ngth1010v.mydailynote.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ngth1010v.mydailynote.core.data.backup.BackupFormat
import io.github.ngth1010v.mydailynote.core.data.settings.AppSettings
import io.github.ngth1010v.mydailynote.core.data.settings.RailSide
import io.github.ngth1010v.mydailynote.core.data.settings.ThemeMode
import io.github.ngth1010v.mydailynote.core.data.settings.TimeFormat
import io.github.ngth1010v.mydailynote.core.ui.ColorGrid
import io.github.ngth1010v.mydailynote.core.ui.ModelRow
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.languageName
import io.github.ngth1010v.mydailynote.speech.api.ModelKind
import io.github.ngth1010v.mydailynote.speech.api.ModelRegistry
import io.github.ngth1010v.mydailynote.speech.api.ModelState
import io.github.ngth1010v.mydailynote.speech.api.SpeechEngineIds
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage
import java.time.LocalDate

private enum class Page { MAIN, MODELS, LICENSES }

@Composable
fun SettingsScreen(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(Page.MAIN) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(vm) {
        vm.messageFlow.collect { snackbar.showSnackbar(message(context, it)) }
    }
    BackHandler(enabled = page != Page.MAIN) { page = Page.MAIN }
    Box(modifier.fillMaxSize()) {
        when (page) {
            Page.MAIN -> MainPage(vm, onModels = { page = Page.MODELS }, onLicenses = { page = Page.LICENSES })
            Page.MODELS -> SubPage(stringResource(R.string.pref_models), onBack = { page = Page.MAIN }) { ModelsPage(vm) }
            Page.LICENSES -> SubPage(stringResource(R.string.about_oss), onBack = { page = Page.MAIN }) { LicensesPage() }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MainPage(vm: SettingsViewModel, onModels: () -> Unit, onLicenses: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val count by vm.entryCount.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val today = LocalDate.now()

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.JSON.mimeType)) { uri ->
        if (uri != null) vm.export(BackupFormat.JSON) { context.contentResolver.openOutputStream(uri) }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.CSV.mimeType)) { uri ->
        if (uri != null) vm.export(BackupFormat.CSV) { context.contentResolver.openOutputStream(uri) }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import { context.contentResolver.openInputStream(uri) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(
                stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        general(vm, s)
        voice(vm, s, onModels)

        item { SectionHeader(stringResource(R.string.section_data)) }
        item {
            PrefItem(
                stringResource(R.string.pref_export),
                stringResource(R.string.pref_export_summary) + " · " + pluralStringResource(R.plurals.actions_count, count, count),
                onClick = { dialog = "export" },
            )
        }
        item {
            PrefItem(stringResource(R.string.pref_import), stringResource(R.string.pref_import_summary), onClick = {
                importFile.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream"))
            })
        }
        item {
            SwitchPref(stringResource(R.string.pref_auto_backup), stringResource(R.string.pref_auto_backup_summary), s.autoBackup) { v ->
                vm.update { it.copy(autoBackup = v) }
            }
        }
        item { PrefItem(stringResource(R.string.pref_clear), onClick = { dialog = "clear" }) }

        item { SectionHeader(stringResource(R.string.section_about)) }
        item {
            PrefItem(
                stringResource(R.string.app_name),
                stringResource(R.string.about_version, vm.appInfo.versionName),
            )
        }
        item {
            PrefItem(stringResource(R.string.about_source), vm.appInfo.repoUrl, onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.appInfo.repoUrl)))
            })
        }
        item { PrefItem(stringResource(R.string.about_license), stringResource(R.string.about_license_summary), onClick = { dialog = "license" }) }
        item { PrefItem(stringResource(R.string.about_oss), onClick = onLicenses) }
        item { PrefItem(stringResource(R.string.about_privacy), onClick = { dialog = "privacy" }) }
        item {
            SwitchPref(
                stringResource(R.string.pref_update_check),
                when (val u = update) {
                    UpdateStatus.Idle, UpdateStatus.Checking -> stringResource(R.string.pref_update_check_summary)
                    UpdateStatus.Latest -> stringResource(R.string.update_latest)
                    UpdateStatus.Failed -> stringResource(R.string.update_failed)
                    is UpdateStatus.Available -> stringResource(R.string.update_available, u.version)
                },
                s.updateCheck,
                onChange = vm::setUpdateCheck,
            )
        }
        (update as? UpdateStatus.Available)?.let { u ->
            item {
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))) },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(stringResource(R.string.update_available, u.version)) }
            }
        }
    }

    when (dialog) {
        "export" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.pref_export)) },
            text = {
                Column {
                    listOf(BackupFormat.JSON to R.string.export_json, BackupFormat.CSV to R.string.export_csv).forEach { (format, label) ->
                        Text(
                            stringResource(label),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dialog = null
                                    val name = "my-daily-note-$today.${format.extension}"
                                    if (format == BackupFormat.JSON) exportJson.launch(name) else exportCsv.launch(name)
                                }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
        "clear" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { dialog = null; vm.clearAll() }) {
                    Text(stringResource(R.string.menu_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
        "license" -> TextDialog(stringResource(R.string.about_license), MIT_LICENSE) { dialog = null }
        "privacy" -> TextDialog(stringResource(R.string.about_privacy), stringResource(R.string.privacy_body)) { dialog = null }
    }
}

private fun LazyListScope.general(vm: SettingsViewModel, s: AppSettings) {
    item { SectionHeader(stringResource(R.string.section_general)) }
    item {
        ChoicePref(
            title = stringResource(R.string.pref_language),
            options = listOf(
                "" to stringResource(R.string.lang_system),
                "en" to stringResource(R.string.lang_en),
                "vi" to stringResource(R.string.lang_vi),
            ),
            selected = vm.appLanguage.substringBefore('-').substringBefore(','),
            onSelect = vm::setAppLanguage,
        )
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_theme),
            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark),
            ),
            s.themeMode,
        ) { v -> vm.update { it.copy(themeMode = v) } }
    }
    item {
        SwitchPref(
            stringResource(R.string.pref_dynamic_color),
            stringResource(R.string.pref_dynamic_color_summary),
            s.dynamicColor,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        ) { v -> vm.update { it.copy(dynamicColor = v) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_rail_side),
            listOf(
                RailSide.RIGHT to stringResource(R.string.rail_right),
                RailSide.LEFT to stringResource(R.string.rail_left),
                RailSide.FOLLOW_LAYOUT to stringResource(R.string.rail_follow),
            ),
            s.railSide,
        ) { v -> vm.update { it.copy(railSide = v) } }
    }
    item {
        val hour = { h: Int -> "%02d:00".format(h) }
        ChoicePref(
            title = stringResource(R.string.pref_day_start),
            options = (0..23).map { it to hour(it) },
            selected = s.dayStartHour,
            summary = hour(s.dayStartHour) + " · " + stringResource(R.string.pref_day_start_summary),
        ) { v -> vm.update { it.copy(dayStartHour = v) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_time_format),
            listOf(
                TimeFormat.SYSTEM to stringResource(R.string.time_system),
                TimeFormat.H12 to stringResource(R.string.time_12),
                TimeFormat.H24 to stringResource(R.string.time_24),
            ),
            s.timeFormat,
        ) { v -> vm.update { it.copy(timeFormat = v) } }
    }
    item { DefaultColorPref(s.defaultColor) { v -> vm.update { it.copy(defaultColor = v) } } }
}

private fun LazyListScope.voice(vm: SettingsViewModel, s: AppSettings, onModels: () -> Unit) {
    item { SectionHeader(stringResource(R.string.section_voice)) }
    item {
        ChoicePref(
            stringResource(R.string.pref_voice_language),
            listOf<Pair<String?, String>>(null to stringResource(R.string.voice_lang_follow)) +
                VoiceLanguage.entries.map { it.tag to languageName(it) } +
                (VoiceLanguage.AUTO_TAG to stringResource(R.string.voice_lang_auto)),
            s.voiceLanguage,
        ) { v -> vm.update { it.copy(voiceLanguage = v) } }
    }
    item {
        ChoicePref(
            title = stringResource(R.string.pref_engine),
            options = buildList {
                add(SpeechEngineIds.VOSK to stringResource(R.string.engine_vosk))
                if (vm.androidEngineAvailable) add(SpeechEngineIds.ANDROID to stringResource(R.string.engine_android))
            },
            selected = s.speechEngine,
            summary = if (s.speechEngine == SpeechEngineIds.ANDROID) {
                stringResource(R.string.engine_android) +
                    if (!vm.androidEngineAvailable) " · " + stringResource(R.string.engine_android_unavailable) else ""
            } else stringResource(R.string.engine_vosk),
        ) { v -> vm.update { it.copy(speechEngine = v) } }
    }
    item { PrefItem(stringResource(R.string.pref_models), stringResource(R.string.pref_models_summary), onClick = onModels) }
    item {
        ChoicePref(
            stringResource(R.string.pref_auto_stop),
            AppSettings.AUTO_STOP_CHOICES.map { ms ->
                ms to if (ms == 0) stringResource(R.string.auto_stop_off)
                else stringResource(R.string.auto_stop_seconds, (ms / 1000f).toString().removeSuffix(".0"))
            },
            s.autoStopMs,
        ) { v -> vm.update { it.copy(autoStopMs = v) } }
    }
    item {
        val states by vm.modelStates.collectAsStateWithLifecycle()
        val installed = states[s.whisperModelId] is ModelState.Installed
        SwitchPref(
            stringResource(R.string.pref_high_accuracy),
            stringResource(R.string.pref_high_accuracy_summary),
            s.highAccuracy && installed,
            enabled = installed,
        ) { v -> vm.update { it.copy(highAccuracy = v) } }
    }
    item {
        val options = ModelRegistry.whisper.map { it.id to it.displayName }
        ChoicePref(
            stringResource(R.string.pref_whisper_model),
            options,
            s.whisperModelId,
            summary = listOfNotNull(
                options.firstOrNull { it.first == s.whisperModelId }?.second,
                stringResource(R.string.pref_whisper_model_hint),
            ).joinToString("\n"),
        ) { v -> vm.update { it.copy(whisperModelId = v) } }
    }
}

@Composable
private fun DefaultColorPref(selected: String, onSelect: (String) -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.pref_default_color), style = MaterialTheme.typography.bodyLarge)
        ColorGrid(selected, dark, onSelect, Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ModelsPage(vm: SettingsViewModel) {
    val states by vm.modelStates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var importTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = importTarget
        if (uri != null && id != null) vm.importModel(id) { context.contentResolver.openInputStream(uri) }
        importTarget = null
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        listOf(ModelKind.VOSK to R.string.models_section_vosk, ModelKind.WHISPER to R.string.models_section_whisper).forEach { (kind, title) ->
            Text(
                stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            ModelRegistry.all.filter { it.kind == kind }.forEach { spec ->
                ModelRow(
                    spec = spec,
                    state = states[spec.id] ?: ModelState.NotInstalled,
                    onDownload = { vm.downloadModel(spec.id) },
                    onImport = { importTarget = spec.id; picker.launch(arrayOf("*/*")) },
                    onCancel = { vm.cancelModel(spec.id) },
                    onDelete = { vm.deleteModel(spec.id) },
                )
                HorizontalDivider()
            }
        }
        Text(
            stringResource(R.string.model_import_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun LicensesPage() {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize()) {
        items(thirdParties.size) { i ->
            val t = thirdParties[i]
            PrefItem(t.name, t.license, onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.url)))
            })
        }
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }
        content()
    }
}

@Composable
private fun TextDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) } },
    )
}

private fun message(context: Context, m: SettingsMessage): String = when (m) {
    is SettingsMessage.Exported -> context.getString(R.string.export_done, m.count)
    SettingsMessage.ExportFailed -> context.getString(R.string.export_failed)
    is SettingsMessage.Imported -> context.getString(R.string.import_done, m.imported, m.skipped)
    SettingsMessage.ImportFailed -> context.getString(R.string.import_failed)
    SettingsMessage.Cleared -> context.getString(R.string.clear_done)
}
