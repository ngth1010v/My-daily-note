package io.github.ngth1010v.mydailynote

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ngth1010v.mydailynote.core.data.settings.AppSettings
import io.github.ngth1010v.mydailynote.core.data.settings.RailSide
import io.github.ngth1010v.mydailynote.core.ui.R
import io.github.ngth1010v.mydailynote.core.ui.RailButton
import io.github.ngth1010v.mydailynote.core.ui.RailButtonGap
import io.github.ngth1010v.mydailynote.core.ui.RailButtonSize
import io.github.ngth1010v.mydailynote.core.ui.RailReservedWidth
import io.github.ngth1010v.mydailynote.feature.home.HomeLaunch
import io.github.ngth1010v.mydailynote.feature.home.HomeRailActions
import io.github.ngth1010v.mydailynote.feature.home.HomeScreen
import io.github.ngth1010v.mydailynote.feature.home.HomeViewModel
import io.github.ngth1010v.mydailynote.feature.home.isCapturing
import io.github.ngth1010v.mydailynote.feature.home.isEditing
import io.github.ngth1010v.mydailynote.feature.home.rememberHomeController
import io.github.ngth1010v.mydailynote.feature.settings.AppInfo
import io.github.ngth1010v.mydailynote.feature.settings.SettingsScreen
import io.github.ngth1010v.mydailynote.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    // One pager page per route, in rail order; swiping left/right switches screens.
    val pager = rememberPagerState { Route.entries.size }
    val scope = rememberCoroutineScope()
    val route = Route.entries[pager.currentPage]
    val go: (Route) -> Unit = { r -> scope.launch { pager.animateScrollToPage(r.ordinal) } }
    LaunchedEffect(launch) { if (launch != null) pager.scrollToPage(Route.HOME.ordinal) }
    BackHandler(enabled = route != Route.HOME) { go(Route.HOME) }

    val homeVm: HomeViewModel = viewModel(factory = viewModelFactory {
        initializer {
            HomeViewModel(container.entries, container.settings)
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
    // Screens are full width; only Home's bottom input panel and snackbar keep clear of the rail.
    val railPadding = if (railAtEnd) PaddingValues(end = RailReservedWidth) else PaddingValues(start = RailReservedWidth)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Includes the keyboard, so the rail and input panel move above it.
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        HorizontalPager(
            state = pager,
            // While typing, horizontal drags belong to the input, not the pager.
            userScrollEnabled = !home.isCapturing && !home.isEditing,
            key = { it },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (Route.entries[page]) {
                Route.HOME -> HomeScreen(home, railPadding, launch, onLaunchHandled)
                Route.GROUP -> ComingSoon(Route.GROUP, R.string.group_placeholder_body)
                Route.ANALYSIS -> ComingSoon(Route.ANALYSIS, R.string.analysis_placeholder_body)
                Route.SETTINGS -> {
                    val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                        initializer {
                            SettingsViewModel(
                                settingsRepo = container.settings,
                                entries = container.entries,
                                backup = container.backup,
                                appInfo = AppInfo(BuildConfig.VERSION_NAME, "https://github.com/$REPO_SLUG", REPO_SLUG),
                            )
                        }
                    })
                    SettingsScreen(vm)
                }
            }
        }

        AnimatedVisibility(
            visible = !(route == Route.HOME && home.isEditing),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(if (railAtEnd) Alignment.BottomEnd else Alignment.BottomStart),
        ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(8.dp),
        ) {
            Column(
                Modifier.padding(8.dp).animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val capturing = route == Route.HOME && home.isCapturing
                AnimatedVisibility(
                    visible = route == Route.HOME,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        Modifier.padding(bottom = if (capturing) 0.dp else RailButtonGap),
                        verticalArrangement = Arrangement.spacedBy(RailButtonGap),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HomeRailActions(home)
                        if (!capturing) HorizontalDivider(Modifier.width(24.dp))
                    }
                }
                AnimatedVisibility(
                    visible = !capturing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Box {
                        // Selection pill: follows the pager, so it slides along with a swipe.
                        val indicator = MaterialTheme.colorScheme.secondaryContainer
                        Box(
                            Modifier
                                .offset {
                                    val position = (pager.currentPage + pager.currentPageOffsetFraction)
                                        .coerceIn(0f, Route.entries.lastIndex.toFloat())
                                    IntOffset(0, (position * (RailButtonSize + RailButtonGap).toPx()).roundToInt())
                                }
                                .size(RailButtonSize)
                                .background(indicator, CircleShape)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(RailButtonGap)) {
                            Route.entries.forEach { r ->
                                RailButton(
                                    icon = r.icon,
                                    label = stringResource(r.label),
                                    selected = r.ordinal == pager.targetPage,
                                    onClick = { go(r) },
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ComingSoon(route: Route, body: Int) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
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
