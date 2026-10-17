package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Size of a navigation bar button, and the gap between stacked buttons. */
val RailButtonSize = 34.dp
val RailButtonGap = 8.dp

/** Home's floating add / save / cancel buttons: 1.2x a navigation button. */
val ActionButtonSize = RailButtonSize * 1.2f

/** Horizontal space Home's floating buttons take (button + margins); bottom panels pad by this. */
val RailReservedWidth = ActionButtonSize + 16.dp

/**
 * A round rail button. [emphasized] = the filled primary button (voice / save); [level] (0..1) pulses it
 * while recording. [selected] only tints the icon: the caller draws the (sliding) selection indicator.
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
    level: (() -> Float)? = null,
    onLongClick: (() -> Unit)? = null,
    size: Dp = RailButtonSize,
) {
    val colors = MaterialTheme.colorScheme
    val (bgTarget, fgTarget) = when {
        emphasized -> colors.primary to colors.onPrimary
        selected -> colors.secondaryContainer.copy(alpha = 0f) to colors.onSecondaryContainer
        else -> colors.surfaceContainerHigh.copy(alpha = 0f) to colors.onSurfaceVariant
    }
    val bg by animateColorAsState(if (enabled) bgTarget else bgTarget.copy(alpha = bgTarget.alpha * 0.4f), label = "railBg")
    val fg by animateColorAsState(if (enabled) fgTarget else fgTarget.copy(alpha = 0.38f), label = "railFg")
    Box(
        modifier
            .size(size)
            // Read in the draw phase: the level changes 10x a second and must not recompose the rail.
            .graphicsLayer {
                val scale = 1f + (level?.invoke() ?: 0f).coerceIn(0f, 1f) * 0.15f
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bg)
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
        Icon(
            icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
