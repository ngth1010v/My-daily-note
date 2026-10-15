package io.github.ngth1010v.mydailynote.feature.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ngth1010v.mydailynote.core.data.model.Action
import io.github.ngth1010v.mydailynote.core.data.model.Group
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.StyleFields

@Composable
internal fun GroupEditorHost(vm: GroupViewModel) {
    val close = { vm.editor = null }
    when (val e = vm.editor) {
        null -> {}
        GroupEditor.NewGroup -> GroupSheet(
            group = null,
            onSave = { name, color, icon, description -> vm.saveGroup(null, name, color, icon, description); close() },
            onDelete = null,
            onDismiss = close,
        )
        is GroupEditor.EditGroup -> GroupSheet(
            group = e.group,
            onSave = { name, color, icon, description -> vm.saveGroup(e.group.id, name, color, icon, description); close() },
            onDelete = if (e.group.isDefault) null else ({ vm.deleteGroup(e.group.id); close() }),
            onDismiss = close,
        )
        is GroupEditor.NewAction -> ActionSheet(
            action = null,
            onSave = { text, color, icon -> vm.saveAction(null, e.groupId, text, color, icon); close() },
            onDelete = null,
            onDismiss = close,
        )
        is GroupEditor.EditAction -> ActionSheet(
            action = e.action,
            onSave = { text, color, icon -> vm.saveAction(e.action.id, e.action.groupId, text, color, icon); close() },
            onDelete = { vm.deleteAction(e.action.id); close() },
            onDismiss = close,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSheet(
    group: Group?,
    onSave: (name: String, color: String?, icon: String?, description: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(group?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(group?.description.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(group?.color) }
    var icon by rememberSaveable { mutableStateOf(group?.icon) }
    val isDefault = group?.isDefault == true
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (group == null) R.string.group_new_title else R.string.group_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                name, { name = it },
                label = { Text(stringResource(R.string.group_name)) },
                // The built-in group may keep no name of its own: it then shows the localized one.
                placeholder = if (isDefault) ({ Text(stringResource(R.string.group_default_name)) }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                description, { description = it },
                label = { Text(stringResource(R.string.field_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            StyleFields(color, icon, onColor = { color = it }, onIcon = { icon = it })
            if (isDefault) {
                Text(
                    stringResource(R.string.group_default_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SheetButtons(
                canSave = name.isNotBlank() || isDefault,
                onSave = { onSave(name, color, icon, description.ifBlank { null }) },
                onDelete = onDelete,
                onDismiss = onDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSheet(
    action: Action?,
    onSave: (text: String, color: String?, icon: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(action?.text.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(action?.color) }
    var icon by rememberSaveable { mutableStateOf(action?.icon) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (action == null) R.string.action_new_title else R.string.edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                text, { text = it },
                label = { Text(stringResource(R.string.field_text)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            StyleFields(color, icon, onColor = { color = it }, onIcon = { icon = it })
            if (action != null && action.count > 0) {
                Text(
                    pluralStringResource(R.plurals.action_delete_entries, action.count, action.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SheetButtons(
                canSave = text.isNotBlank(),
                onSave = { onSave(text, color, icon) },
                onDelete = onDelete,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun SheetButtons(canSave: Boolean, onSave: () -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.menu_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        Button(enabled = canSave, onClick = onSave) { Text(stringResource(R.string.action_save)) }
    }
}
