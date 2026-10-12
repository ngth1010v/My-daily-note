package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Total horizontal space the floating rail takes (pill + margin); screens pad their content by this. */
val RailReservedWidth = 80.dp

/**
 * A round rail button. [emphasized] = the large primary voice button; [level] (0..1) pulses it
 * while recording.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RailButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    level: Float = 0f,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        emphasized -> colors.primary to colors.onPrimary
        selected -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.surfaceContainerHigh.copy(alpha = 0f) to colors.onSurfaceVariant
    }
    val size = if (emphasized) 56.dp else 48.dp
    Box(
        modifier
            .size(size)
            .scale(1f + level.coerceIn(0f, 1f) * 0.15f)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.4f))
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                indication = ripple(),
                interactionSource = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) fg else fg.copy(alpha = 0.38f))
    }
}
