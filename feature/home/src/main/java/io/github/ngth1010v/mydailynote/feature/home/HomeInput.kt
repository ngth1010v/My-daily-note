package io.github.ngth1010v.mydailynote.feature.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ngth1010v.mydailynote.core.ui.EntryBadge
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import io.github.ngth1010v.mydailynote.core.ui.ModelRow
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.RailButton
import io.github.ngth1010v.mydailynote.core.ui.formatBytes
import io.github.ngth1010v.mydailynote.core.ui.languageName
import io.github.ngth1010v.mydailynote.speech.api.ModelSpec
import io.github.ngth1010v.mydailynote.speech.api.ModelState
import kotlinx.coroutines.launch

/** Shared by the Home screen and its rail buttons: mic permission flow and snackbars. */
@Stable
class HomeController internal constructor(
    val vm: HomeViewModel,
    val snackbar: SnackbarHostState,
    private val requestPermission: () -> Unit,
    private val hasPermission: () -> Boolean,
    private val shouldExplain: () -> Boolean,
) {
    var showRationale by mutableStateOf(false)
        internal set

    fun onMic() {
        when {
            hasPermission() -> vm.startVoice()
            shouldExplain() -> showRationale = true
            else -> requestPermission()
        }
    }

    internal fun confirmRationale() {
        showRationale = false
        requestPermission()
    }
}

@Composable
fun rememberHomeController(vm: HomeViewModel): HomeController {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.voice_permission_denied)
    val openSettings = stringResource(R.string.open_settings)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startVoice()
        else scope.launch {
            val result = snackbar.showSnackbar(deniedText, openSettings, withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
    return remember(vm) {
        HomeController(
            vm = vm,
            snackbar = snackbar,
            requestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            hasPermission = {
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            },
            shouldExplain = {
                activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true
            },
        )
    }
}

/** Home's contextual rail buttons, shown above the route buttons. */
@Composable
fun ColumnScope.HomeRailActions(controller: HomeController) {
    val vm = controller.vm
    when (val mode = vm.mode) {
        InputMode.Idle -> {
            RailButton(Icons.Filled.Edit, stringResource(R.string.action_add), onClick = { vm.startTyping() })
            RailButton(
                Icons.Filled.Mic,
                stringResource(R.string.action_add_voice),
                onClick = controller::onMic,
                onLongClick = vm::cycleVoiceLanguage,
                emphasized = true,
            )
        }
        InputMode.Typing -> {
            RailButton(Icons.Filled.Check, stringResource(R.string.action_save), onClick = vm::save, emphasized = true, enabled = vm.text.isNotBlank())
            RailButton(Icons.Filled.Close, stringResource(R.string.action_cancel), onClick = vm::cancelInput)
        }
        is InputMode.Voice -> when (mode.phase) {
            VoicePhase.LISTENING -> {
                RailButton(Icons.Filled.Close, stringResource(R.string.action_cancel), onClick = vm::cancelInput)
                RailButton(Icons.Filled.Stop, stringResource(R.string.action_stop), onClick = vm::stopVoice, emphasized = true, level = { vm.level })
            }
            VoicePhase.REFINING, VoicePhase.DONE -> {
                RailButton(Icons.Filled.Check, stringResource(R.string.action_save), onClick = vm::save, emphasized = true, enabled = vm.text.isNotBlank())
                RailButton(Icons.Filled.Close, stringResource(R.string.action_cancel), onClick = vm::cancelInput)
                RailButton(Icons.Filled.Mic, stringResource(R.string.action_record_again), onClick = controller::onMic, onLongClick = vm::cycleVoiceLanguage)
            }
        }
    }
}

/** True while Home is taking input; the app hides the route buttons then. */
val HomeController.isCapturing: Boolean get() = vm.mode != InputMode.Idle

/** True while the entry edit sub screen is open; the app hides the whole rail then. */
val HomeController.isEditing: Boolean get() = vm.detail != null

/** Bottom panel: text field with suggestions, or the live voice transcript. */
@Composable
internal fun InputPanel(vm: HomeViewModel, defaultColor: String, dark: Boolean, modifier: Modifier = Modifier) {
    val mode = vm.mode
    AnimatedVisibility(mode != InputMode.Idle, modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                if (mode is InputMode.Typing) {
                    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
                    if (suggestions.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            suggestions.forEach { s ->
                                SuggestionChip(
                                    onClick = { vm.relog(s) },
                                    label = { Text(s.text, maxLines = 1) },
                                    icon = { EntryBadge(EntryPalette.color(s.color, defaultColor, dark), s.icon, size = 16.dp) },
                                )
                            }
                        }
                    }
                }
                if (mode is InputMode.Voice) {
                    VoiceStatus(vm, mode.phase)
                }
                if (mode is InputMode.Voice && mode.phase == VoicePhase.LISTENING) {
                    Text(
                        vm.partial.ifEmpty { "…" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp),
                    )
                } else {
                    val focus = remember { FocusRequester() }
                    OutlinedTextField(
                        value = vm.text,
                        onValueChange = { value ->
                            // Actions are one short sentence: Enter (hardware or IME newline) saves.
                            if ('\n' in value) {
                                vm.onTextChange(value.replace('\n', ' ').trim())
                                vm.save()
                            } else vm.onTextChange(value)
                        },
                        placeholder = { Text(stringResource(R.string.input_hint)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { vm.save() }),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                    if (mode == InputMode.Typing) LaunchedEffect(Unit) { focus.requestFocus() }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatus(vm: HomeViewModel, phase: VoicePhase) {
    val label = when (phase) {
        VoicePhase.LISTENING -> stringResource(R.string.voice_listening) + " · " + languageName(vm.voiceLanguage())
        VoicePhase.REFINING -> stringResource(R.string.voice_refining)
        VoicePhase.DONE -> stringResource(R.string.voice_edit_hint)
    }
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    when (phase) {
        VoicePhase.LISTENING -> LinearProgressIndicator(
            progress = { vm.level },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        VoicePhase.REFINING -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
        VoicePhase.DONE -> {}
    }
}

@Composable
internal fun MicRationaleDialog(controller: HomeController) {
    if (!controller.showRationale) return
    AlertDialog(
        onDismissRequest = { controller.showRationale = false },
        title = { Text(stringResource(R.string.voice_permission_title)) },
        text = { Text(stringResource(R.string.voice_permission_rationale)) },
        confirmButton = { TextButton(onClick = controller::confirmRationale) { Text(stringResource(R.string.dialog_ok)) } },
        dismissButton = { TextButton(onClick = { controller.showRationale = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Asks to download (or import) the voice model for the current language; starts listening once installed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelPromptSheet(vm: HomeViewModel) {
    val spec: ModelSpec = vm.modelPrompt ?: return
    val states by vm.modelStates.collectAsStateWithLifecycle()
    val state = states[spec.id] ?: ModelState.NotInstalled
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(spec) { context.contentResolver.openInputStream(uri) }
    }
    LaunchedEffect(state) {
        if (state is ModelState.Installed) vm.onModelReady()
    }
    ModalBottomSheet(onDismissRequest = vm::dismissModelPrompt) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            val lang = spec.language?.let { languageName(it) } ?: ""
            Text(stringResource(R.string.model_needed_title, lang), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.model_needed_body, formatBytes(spec.sizeBytes)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            ModelRow(
                spec = spec,
                state = state,
                onDownload = { vm.downloadModel(spec) },
                onImport = { picker.launch(arrayOf("*/*")) },
                onCancel = { vm.cancelModelDownload(spec) },
                onDelete = {},
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
