package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ngth1010v.mydailynote.core.data.model.Group

/** The group's name; the built-in group shows the localized "Other" until the user renames it. */
@Composable
fun groupName(group: Group): String =
    group.name.ifBlank { if (group.isDefault) stringResource(R.string.group_default_name) else "" }
