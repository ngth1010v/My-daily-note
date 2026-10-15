package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Color grid and icon chooser, shared by the entry and group/action editors. */
@Composable
fun StyleFields(
    color: String?,
    icon: String?,
    onColor: (String?) -> Unit,
    onIcon: (String?) -> Unit,
) {
    val tint = EntryPalette.color(color)
    var pickingIcon by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelLarge)
        ColorGrid(color, onSelect = onColor)
        Text(stringResource(R.string.icon_label), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Same look as the row on Home.
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f))
                    .clickable { pickingIcon = true },
                contentAlignment = Alignment.Center,
            ) {
                EntryBadge(tint, icon, size = 26.dp)
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { pickingIcon = true }) { Text(stringResource(R.string.icon_select)) }
        }
    }
    if (pickingIcon) {
        IconPickerSheet(icon, tint, onPick = onIcon, onDismiss = { pickingIcon = false })
    }
}

/** All icons in one grid under group headers, with a tab per group to jump to it. Picking closes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPickerSheet(selected: String?, tint: Color, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val groups = EntryIcons.groups
    // Grid index of each group's header (each header is followed by its icons).
    val headerIndex = remember { groups.runningFold(0) { i, g -> i + 1 + g.icons.size }.dropLast(1) }
    val selectedGroup = remember { groups.indexOfFirst { g -> g.icons.any { it.key == selected } }.coerceAtLeast(0) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = headerIndex[selectedGroup])
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // The tab follows the scroll; a tapped tab stays selected until the next drag (the last groups
    // can't scroll up to the top, so the scroll position alone would point at an earlier one).
    var tapped by remember { mutableStateOf<Int?>(null) }
    val scrolledTo by remember { derivedStateOf { headerIndex.indexOfLast { it <= gridState.firstVisibleItemIndex } } }
    LaunchedEffect(gridState) {
        gridState.interactionSource.interactions.collect { if (it is DragInteraction.Start) tapped = null }
    }
    val current = tapped ?: scrolledTo

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            stringResource(R.string.icon_select),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
        )
        PrimaryScrollableTabRow(selectedTabIndex = current, edgePadding = 16.dp, containerColor = Color.Transparent) {
            groups.forEachIndexed { i, group ->
                Tab(
                    selected = i == current,
                    onClick = {
                        tapped = i
                        scope.launch { gridState.animateScrollToItem(headerIndex[i]) }
                    },
                    text = { Text(stringResource(group.title)) },
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            state = gridState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        ) {
            groups.forEach { group ->
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    Text(
                        stringResource(group.title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(group.icons, key = { it.key }, contentType = { "icon" }) { (key, vector) ->
                    Box(contentAlignment = Alignment.Center) {
                        IconChoice(
                            selected = selected == key,
                            onClick = {
                                onPick(key)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            },
                        ) {
                            Icon(vector, key, tint = tint)
                        }
                    }
                }
            }
        }
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
