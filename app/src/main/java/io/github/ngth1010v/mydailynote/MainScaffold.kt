package io.github.ngth1010v.mydailynote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ngth1010v.mydailynote.core.data.settings.AppSettings
import io.github.ngth1010v.mydailynote.core.data.settings.RailSide
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.RailButton
import io.github.ngth1010v.mydailynote.core.ui.RailReservedWidth
import io.github.ngth1010v.mydailynote.feature.home.HomeLaunch
import io.github.ngth1010v.mydailynote.feature.home.HomeRailActions
import io.github.ngth1010v.mydailynote.feature.home.HomeScreen
import io.github.ngth1010v.mydailynote.feature.home.HomeViewModel
import io.github.ngth1010v.mydailynote.feature.home.isCapturing
import io.github.ngth1010v.mydailynote.feature.home.rememberHomeController
import io.github.ngth1010v.mydailynote.feature.settings.AppInfo
import io.github.ngth1010v.mydailynote.feature.settings.SettingsScreen
import io.github.ngth1010v.mydailynote.feature.settings.SettingsViewModel
import io.github.ngth1010v.mydailynote.speech.api.PlatformSpeechEngine

enum class Route(val label: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Filled.Home),
    GROUP(R.string.nav_group, Icons.Filled.Workspaces),
    ANALYSIS(R.string.nav_analysis, Icons.Filled.Insights),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings),
}

private const val REPO_SLUG = "ngth1010v/My-daily-note"

/** The 4 screens plus the always-visible floating navigation rail. */
@Composable
fun MainScaffold(
    container: AppContainer,
    settings: AppSettings,
    launch: HomeLaunch?,
    onLaunchHandled: () -> Unit,
) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    LaunchedEffect(launch) { if (launch != null) route = Route.HOME }
    BackHandler(enabled = route != Route.HOME) { route = Route.HOME }

    val homeVm: HomeViewModel = viewModel(factory = viewModelFactory {
        initializer {
            HomeViewModel(container.entries, container.settings, container.voice, container.models, container::appLanguage)
        }
    })
    val home = rememberHomeController(homeVm)

    // Which physical side the rail sits on, expressed as layout start/end.
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val railAtEnd = when (settings.railSide) {
        RailSide.RIGHT -> ltr
        RailSide.LEFT -> !ltr
        RailSide.FOLLOW_LAYOUT -> true
    }
    val contentPadding = if (railAtEnd) PaddingValues(end = RailReservedWidth) else PaddingValues(start = RailReservedWidth)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Includes the keyboard, so the rail and input panel move above it.
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (route) {
            Route.HOME -> HomeScreen(home, contentPadding, launch, onLaunchHandled)
            Route.GROUP -> ComingSoon(Route.GROUP, R.string.group_placeholder_body, contentPadding)
            Route.ANALYSIS -> ComingSoon(Route.ANALYSIS, R.string.analysis_placeholder_body, contentPadding)
            Route.SETTINGS -> {
                val context = LocalContext.current
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            settingsRepo = container.settings,
                            entries = container.entries,
                            backup = container.backup,
                            models = container.models,
                            appInfo = AppInfo(BuildConfig.VERSION_NAME, "https://github.com/$REPO_SLUG", REPO_SLUG),
                            androidEngineAvailable = PlatformSpeechEngine.isSupported(context),
                        )
                    }
                })
                SettingsScreen(vm, contentPadding)
            }
        }

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.align(if (railAtEnd) Alignment.BottomEnd else Alignment.BottomStart).padding(8.dp),
        ) {
            Column(
                Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val capturing = route == Route.HOME && home.isCapturing
                if (route == Route.HOME) {
                    HomeRailActions(home)
                    if (!capturing) HorizontalDivider(Modifier.width(32.dp))
                }
                if (!capturing) {
                    Route.entries.forEach { r ->
                        RailButton(
                            icon = r.icon,
                            label = stringResource(r.label),
                            selected = r == route,
                            onClick = { route = r },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoon(route: Route, body: Int, contentPadding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(route.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(
            stringResource(route.label),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.coming_soon),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
