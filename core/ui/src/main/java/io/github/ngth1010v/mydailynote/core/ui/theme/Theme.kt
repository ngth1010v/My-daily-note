package io.github.ngth1010v.mydailynote.core.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2E4),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    secondaryContainer = Color(0xFFCCE8E2),
    surface = Color(0xFFF8FAF8),
    surfaceContainer = Color(0xFFECEFED),
)

@Composable
fun MyDailyNoteTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Light only.
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(LocalContext.current)
    } else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
