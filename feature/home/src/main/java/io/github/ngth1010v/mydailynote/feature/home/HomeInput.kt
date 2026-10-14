package io.github.ngth1010v.mydailynote.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ngth1010v.mydailynote.core.ui.EntryBadge
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.RailButton

/** Shared by the Home screen and its rail buttons. */
@Stable
class HomeController internal constructor(
    val vm: HomeViewModel,
    val snackbar: SnackbarHostState,
)

@Composable
fun rememberHomeController(vm: HomeViewModel): HomeController {
    val snackbar = remember { SnackbarHostState() }
    return remember(vm) { HomeController(vm, snackbar) }
}

/** Home's contextual rail buttons, shown above the route buttons. */
@Composable
fun ColumnScope.HomeRailActions(controller: HomeController) {
    val vm = controller.vm
    when (vm.mode) {
        InputMode.Idle -> {
            RailButton(Icons.Filled.Edit, stringResource(R.string.action_add), onClick = { vm.startTyping() }, emphasized = true)
        }
        InputMode.Typing -> {
            RailButton(Icons.Filled.Check, stringResource(R.string.action_save), onClick = vm::save, emphasized = true, enabled = vm.text.isNotBlank())
            RailButton(Icons.Filled.Close, stringResource(R.string.action_cancel), onClick = vm::cancelInput)
        }
    }
}

/** True while Home is taking input; the app hides the route buttons then. */
val HomeController.isCapturing: Boolean get() = vm.mode != InputMode.Idle

/** True while the entry edit sub screen is open; the app hides the whole rail then. */
val HomeController.isEditing: Boolean get() = vm.detail != null

/** Bottom panel: text field with suggestions. */
@Composable
internal fun InputPanel(vm: HomeViewModel, defaultColor: String, modifier: Modifier = Modifier) {
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
                                    icon = { EntryBadge(EntryPalette.color(s.color, defaultColor), s.icon, size = 16.dp) },
                                )
                            }
                        }
                    }
                }
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
