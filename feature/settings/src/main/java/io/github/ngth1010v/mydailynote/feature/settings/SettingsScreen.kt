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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ngth1010v.mydailynote.core.data.backup.BackupFormat
import io.github.ngth1010v.mydailynote.core.data.model.RecommendOptions
import io.github.ngth1010v.mydailynote.core.data.settings.AppSettings
import io.github.ngth1010v.mydailynote.core.data.settings.RailSide
import io.github.ngth1010v.mydailynote.core.data.settings.TimeFormat
import io.github.ngth1010v.mydailynote.core.ui.R
import java.time.LocalDate

private enum class Page { MAIN, LICENSES }

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
            Page.MAIN -> MainPage(vm, onLicenses = { page = Page.LICENSES })
            Page.LICENSES -> SubPage(stringResource(R.string.about_oss), onBack = { page = Page.MAIN }) { LicensesPage() }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MainPage(vm: SettingsViewModel, onLicenses: () -> Unit) {
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
        suggestions(vm, s)

        item { SectionHeader(stringResource(R.string.section_groups)) }
        item {
            SwitchPref(stringResource(R.string.pref_auto_group), stringResource(R.string.pref_auto_group_summary), s.autoGroupActions) { v ->
                vm.update { it.copy(autoGroupActions = v) }
            }
        }
        item {
            SwitchPref(
                stringResource(R.string.pref_auto_create_groups),
                stringResource(R.string.pref_auto_create_groups_summary),
                s.autoCreateGroups,
                enabled = s.autoGroupActions,
            ) { v -> vm.update { it.copy(autoCreateGroups = v) } }
        }

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
        item { PrefItem(stringResource(R.string.pref_restyle), stringResource(R.string.pref_restyle_summary), onClick = { dialog = "restyle" }) }
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
        "restyle" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.restyle_confirm_title)) },
            text = { Text(stringResource(R.string.restyle_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { dialog = null; vm.restyleAll() }) { Text(stringResource(R.string.restyle_confirm)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
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
}

private fun LazyListScope.suggestions(vm: SettingsViewModel, s: AppSettings) {
    item { SectionHeader(stringResource(R.string.section_suggestions)) }
    item {
        SwitchPref(stringResource(R.string.pref_recommend), stringResource(R.string.pref_recommend_summary), s.recommendActions) { v ->
            vm.update { it.copy(recommendActions = v) }
        }
    }
    val on = s.recommendActions
    val r = s.recommend
    val tune = { f: (RecommendOptions) -> RecommendOptions -> vm.update { it.copy(recommend = f(it.recommend)) } }
    item {
        ChoicePref(
            stringResource(R.string.pref_recommend_confidence),
            (5..95 step 5).map { it to "$it%" },
            r.minConfidencePercent,
            enabled = on,
            summary = "${r.minConfidencePercent}% · " + stringResource(R.string.pref_recommend_confidence_summary),
        ) { v -> tune { it.copy(minConfidencePercent = v) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_recommend_usual),
            (1..10).map { it to "$it" },
            r.usualCount,
            enabled = on,
            summary = "${r.usualCount} · " + stringResource(R.string.pref_recommend_usual_summary),
        ) { v -> tune { it.copy(usualCount = v, maxCount = maxOf(it.maxCount, v)) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_recommend_max),
            (1..20).map { it to "$it" },
            r.maxCount,
            enabled = on,
        ) { v -> tune { it.copy(maxCount = v, usualCount = minOf(it.usualCount, v)) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_recommend_history),
            listOf(7, 14, 28, 56, 90, 180).map { it to pluralStringResource(R.plurals.days_count, it, it) },
            r.historyDays,
            enabled = on,
        ) { v -> tune { it.copy(historyDays = v) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_recommend_min_days),
            (1..7).map { it to pluralStringResource(R.plurals.days_count, it, it) },
            r.minDays,
            enabled = on,
            summary = pluralStringResource(R.plurals.days_count, r.minDays, r.minDays) + " · " +
                stringResource(R.string.pref_recommend_min_days_summary),
        ) { v -> tune { it.copy(minDays = v) } }
    }
    item {
        ChoicePref(
            stringResource(R.string.pref_recommend_weekday),
            listOf(1 to stringResource(R.string.off), 2 to "×2", 4 to "×4", 8 to "×8"),
            r.weekdayWeight,
            enabled = on,
            summary = (if (r.weekdayWeight == 1) stringResource(R.string.off) else "×${r.weekdayWeight}") + " · " +
                stringResource(R.string.pref_recommend_weekday_summary),
        ) { v -> tune { it.copy(weekdayWeight = v) } }
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
    is SettingsMessage.Restyled -> context.resources.getQuantityString(R.plurals.restyle_done, m.count, m.count)
}
