package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The color dot (or colored icon) shown in front of an entry. */
@Composable
fun EntryBadge(color: Color, iconKey: String?, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    val icon = EntryIcons.icon(iconKey)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size))
        } else {
            Box(Modifier.size(size * 0.55f).background(color, CircleShape))
        }
    }
}
