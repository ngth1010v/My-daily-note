package io.github.ngth1010v.mydailynote.core.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ngth1010v.mydailynote.speech.api.ModelSpec
import io.github.ngth1010v.mydailynote.speech.api.ModelState
import io.github.ngth1010v.mydailynote.speech.api.VoiceLanguage

/** Downloads above this size ask for confirmation first. */
private const val LARGE_MODEL_BYTES = 100L * 1024 * 1024

@Composable
fun formatBytes(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

@Composable
fun languageName(language: VoiceLanguage): String = stringResource(
    when (language) {
        VoiceLanguage.EN -> R.string.lang_en
        VoiceLanguage.VI -> R.string.lang_vi
    }
)

@Composable
fun modelStatusText(spec: ModelSpec, state: ModelState): String = when (state) {
    ModelState.NotInstalled -> stringResource(R.string.model_not_installed, formatBytes(spec.sizeBytes))
    is ModelState.Downloading -> stringResource(R.string.model_downloading, (state.progress * 100).toInt())
    ModelState.Installing -> stringResource(R.string.model_installing)
    is ModelState.Installed -> stringResource(R.string.model_installed, formatBytes(state.bytes))
    is ModelState.Failed -> stringResource(
        when (state.reason) {
            ModelState.Reason.NETWORK -> R.string.model_failed_network
            ModelState.Reason.CHECKSUM -> R.string.model_failed_checksum
            ModelState.Reason.STORAGE -> R.string.model_failed_storage
            ModelState.Reason.INVALID_FILE -> R.string.model_failed_invalid
        }
    )
}

/** Name, status/progress and Download / Import / Cancel / Delete actions for one model. */
@Composable
fun ModelRow(
    spec: ModelSpec,
    state: ModelState,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmLarge by rememberSaveable { mutableStateOf(false) }
    if (confirmLarge) {
        AlertDialog(
            onDismissRequest = { confirmLarge = false },
            title = { Text(stringResource(R.string.model_large_title, spec.displayName)) },
            text = { Text(stringResource(R.string.model_large_body, formatBytes(spec.sizeBytes))) },
            confirmButton = {
                TextButton(onClick = { confirmLarge = false; onDownload() }) {
                    Text(stringResource(R.string.model_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLarge = false }) { Text(stringResource(R.string.model_large_not_now)) }
            },
        )
    }
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(spec.displayName, style = MaterialTheme.typography.bodyLarge)
        Text(
            modelStatusText(spec, state),
            style = MaterialTheme.typography.bodySmall,
            color = if (state is ModelState.Failed) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (state) {
            is ModelState.Downloading -> LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            ModelState.Installing -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
            else -> {}
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            when (state) {
                is ModelState.Downloading -> TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                ModelState.Installing -> {}
                is ModelState.Installed -> TextButton(onClick = onDelete) { Text(stringResource(R.string.model_delete)) }
                else -> {
                    TextButton(onClick = onImport) { Text(stringResource(R.string.model_import)) }
                    OutlinedButton(onClick = {
                        if (spec.sizeBytes > LARGE_MODEL_BYTES) confirmLarge = true else onDownload()
                    }) { Text(stringResource(R.string.model_download)) }
                }
            }
        }
    }
}
