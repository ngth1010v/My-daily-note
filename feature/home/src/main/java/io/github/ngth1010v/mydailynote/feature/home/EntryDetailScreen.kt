package io.github.ngth1010v.mydailynote.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.ui.Formatters
import io.github.ngth1010v.mydailynote.core.ui.R
import java.time.Instant
import java.time.ZoneId

/** Full-screen editor for one entry: text, description, time, color and icon. Back discards. */
@Composable
internal fun EntryDetailScreen(
    entry: Entry,
    formatters: Formatters,
    defaultColor: String,
    dark: Boolean,
    onSave: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable(entry.id) { mutableStateOf(entry.text) }
    var description by rememberSaveable(entry.id) { mutableStateOf(entry.description.orEmpty()) }
    var color by rememberSaveable(entry.id) { mutableStateOf(entry.colorOverride) }
    var icon by rememberSaveable(entry.id) { mutableStateOf(entry.iconOverride) }
    var achievedAt by rememberSaveable(entry.id) { mutableStateOf(entry.achievedAt) }
    var pickingTime by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    // Unchanged time keeps the entry's own offset; a picked time uses the device's current zone.
    val offset = if (achievedAt == entry.achievedAt) entry.zoneOffset else zone.rules.getOffset(achievedAt)
    val local = achievedAt.atOffset(offset)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_cancel))
                }
                Text(
                    stringResource(R.string.edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDelete(entry); onClose() }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.menu_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.field_text)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    description, { description = it },
                    label = { Text(stringResource(R.string.field_description)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(formatters.date(local.toLocalDate()) + " · " + formatters.time24(local))
                }
                StyleFields(color, icon, defaultColor, dark, onColor = { color = it }, onIcon = { icon = it })
            }
            Button(
                enabled = text.isNotBlank(),
                onClick = {
                    onSave(
                        entry.copy(
                            text = text,
                            description = description.ifBlank { null },
                            colorOverride = color,
                            iconOverride = icon,
                            achievedAt = achievedAt,
                            zoneOffset = offset,
                        )
                    )
                    onClose()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }

    if (pickingTime) {
        ChangeTimeDialogs(
            entry.copy(achievedAt = achievedAt, zoneOffset = offset),
            use24h = formatters.use24h,
            onPicked = { at: Instant -> achievedAt = at; pickingTime = false },
            onDismiss = { pickingTime = false },
        )
    }
}
