package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val CellShape = RoundedCornerShape(6.dp)

/** The full [EntryPalette] as a hue-by-shade table of rounded rectangles; cells stretch to fill the width. */
@Composable
fun ColorGrid(selected: String?, dark: Boolean, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EntryPalette.swatches.chunked(EntryPalette.COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { sw ->
                    val fill = if (dark) sw.dark else sw.light
                    val isSelected = sw.key == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CellShape)
                            .background(fill)
                            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CellShape) else Modifier)
                            .semantics { contentDescription = sw.key; this.selected = isSelected }
                            .clickable(onClickLabel = sw.key) { onSelect(sw.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check, null,
                                // Readable on both light and dark cells.
                                tint = if (fill.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
