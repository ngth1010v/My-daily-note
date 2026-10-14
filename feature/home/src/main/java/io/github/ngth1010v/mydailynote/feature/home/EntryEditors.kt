package io.github.ngth1010v.mydailynote.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.ui.EntryIcons
import io.github.ngth1010v.mydailynote.core.ui.ColorGrid
import io.github.ngth1010v.mydailynote.core.ui.EntryPalette
import io.github.ngth1010v.mydailynote.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Which editor is open for which entry. */
internal sealed interface EntryEditor {
    val entry: Entry

    data class Text(override val entry: Entry) : EntryEditor
    data class Time(override val entry: Entry) : EntryEditor
    data class Style(override val entry: Entry) : EntryEditor
}

@Composable
internal fun EntryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTime: () -> Unit,
    onStyle: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text(stringResource(R.string.menu_edit)) }, onClick = { onDismiss(); onEdit() })
        DropdownMenuItem(text = { Text(stringResource(R.string.menu_change_time)) }, onClick = { onDismiss(); onTime() })
        DropdownMenuItem(text = { Text(stringResource(R.string.menu_color_icon)) }, onClick = { onDismiss(); onStyle() })
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_delete), color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() },
        )
    }
}

@Composable
internal fun EntryEditorHost(
    editor: EntryEditor?,
    use24h: Boolean,
    defaultColor: String,
    onUpdate: (Entry) -> Unit,
    onChangeTime: (Entry, Instant) -> Unit,
    onClose: () -> Unit,
) {
    when (editor) {
        null -> {}
        is EntryEditor.Text -> EditTextSheet(editor.entry, onSave = { onUpdate(it); onClose() }, onDismiss = onClose)
        is EntryEditor.Time -> ChangeTimeDialogs(editor.entry, use24h, onPicked = { onChangeTime(editor.entry, it); onClose() }, onDismiss = onClose)
        is EntryEditor.Style -> StyleSheet(editor.entry, defaultColor, onChange = onUpdate, onDismiss = onClose)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTextSheet(entry: Entry, onSave: (Entry) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(entry.text) }
    var description by rememberSaveable { mutableStateOf(entry.description.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.edit_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.field_text)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                description, { description = it },
                label = { Text(stringResource(R.string.field_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    enabled = text.isNotBlank(),
                    onClick = { onSave(entry.copy(text = text, description = description.ifBlank { null })) },
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

/** Date picker, then time picker. Uses the entry's own offset so the shown time matches the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChangeTimeDialogs(entry: Entry, use24h: Boolean, onPicked: (Instant) -> Unit, onDismiss: () -> Unit) {
    val local = entry.achievedAt.atOffset(entry.zoneOffset)
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val date = pickedDate
    if (date == null) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    pickedDate = state.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: local.toLocalDate()
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state) }
    } else {
        val state = rememberTimePickerState(local.hour, local.minute, is24Hour = use24h)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.time_pick_title)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    // Interpret the picked wall time in the device's current zone (where the user is now).
                    val at = date.atTime(LocalTime.of(state.hour, state.minute)).atZone(ZoneId.systemDefault()).toInstant()
                    onPicked(at)
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleSheet(entry: Entry, defaultColor: String, onChange: (Entry) -> Unit, onDismiss: () -> Unit) {
    var color by remember { mutableStateOf(entry.colorOverride) }
    var icon by remember { mutableStateOf(entry.iconOverride) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.color_icon_title), style = MaterialTheme.typography.titleLarge)
            StyleFields(
                color, icon, defaultColor,
                onColor = { color = it; onChange(entry.copy(colorOverride = color, iconOverride = icon)) },
                onIcon = { icon = it; onChange(entry.copy(colorOverride = color, iconOverride = icon)) },
            )
        }
    }
}

/** Color grid and icon chooser, shared by the style sheet and the entry detail screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StyleFields(
    color: String?,
    icon: String?,
    defaultColor: String,
    onColor: (String?) -> Unit,
    onIcon: (String?) -> Unit,
) {
    val tint = EntryPalette.color(color, defaultColor)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Swatch(
                fill = EntryPalette.color(null, defaultColor),
                selected = color == null,
                label = stringResource(R.string.color_default),
                outlined = true,
            ) { onColor(null) }
            Text(stringResource(R.string.color_default), style = MaterialTheme.typography.bodyMedium)
        }
        ColorGrid(color, onSelect = onColor)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconChoice(selected = icon == null, onClick = { onIcon(null) }) {
                Icon(Icons.Filled.Block, stringResource(R.string.icon_none), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.icon_label), style = MaterialTheme.typography.labelLarge)
        }
        EntryIcons.groups.forEach { group ->
            Text(
                stringResource(group.title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.icons.forEach { (key, vector) ->
                    IconChoice(selected = icon == key, onClick = { onIcon(key) }) {
                        Icon(vector, key, tint = tint)
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    fill: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    label: String,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(36.dp)
            .clip(shape)
            .background(fill)
            .then(if (outlined) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape) else Modifier)
            .semantics { contentDescription = label; this.selected = selected }
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.surface)
    }
}

@Composable
private fun IconChoice(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
