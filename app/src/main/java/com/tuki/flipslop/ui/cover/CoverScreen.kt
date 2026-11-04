package com.tuki.flipslop.ui.cover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.ui.cover.pages.AppsPage
import com.tuki.flipslop.ui.cover.pages.HomePage
import com.tuki.flipslop.ui.cover.pages.MediaPage
import com.tuki.flipslop.ui.cover.pages.NotificationsPage
import com.tuki.flipslop.ui.cover.pages.TogglesPage
import com.tuki.flipslop.ui.cover.pages.WidgetsPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CoverScreen(
    state: CoverUiState,
    actions: CoverActions,
    modifier: Modifier = Modifier,
    initialPage: CoverPage = CoverPage.HOME,
    widget: @Composable (id: Int, modifier: Modifier) -> Unit = { _, _ -> },
) {
    val pages = state.pages.ifEmpty { listOf(CoverPage.HOME) }
    val pager = rememberPagerState(initialPage = pages.indexOf(initialPage).coerceAtLeast(0)) { pages.size }
    val scope = rememberCoroutineScope()
    val home = pages.indexOf(CoverPage.HOME).coerceAtLeast(0)

    // Back on the cover returns to the home pane instead of leaving the launcher.
    BackHandler(enabled = pager.currentPage != home) {
        scope.launch { pager.animateScrollToPage(home) }
    }

    val edge = state.edgePaddingDp.dp
    val bottom = state.bottomInsetDp.dp
    val dock = state.dock
    val dockMedia = dock.media && state.media != null
    val dockNotifications = dock.notifications && state.notifications.isNotEmpty()

    val goTo: (CoverPage) -> Boolean = { page ->
        val i = pages.indexOf(page)
        if (i >= 0) scope.launch { pager.animateScrollToPage(i) }
        i >= 0
    }

    // Height of the dock column; the pager keeps clear of it and of the camera block.
    val dockRows = listOfNotNull(
        DockMetrics.MediaHeight.takeIf { dockMedia },
        DockMetrics.NotificationsHeight.takeIf { dockNotifications },
        DockMetrics.DotsHeight.takeIf { !dock.pageBarAlways },
    )
    val dockHeight = if (dockRows.isEmpty()) {
        0.dp
    } else {
        dockRows.fold(0.dp) { acc, h -> acc + h } + DockMetrics.Gap * (dockRows.size - 1) + DockMetrics.BottomMargin
    }
    val reserved = maxOf(bottom, dockHeight)
    val contentBottom = if (dock.pageBarAlways) reserved + DockMetrics.FloatingToolbarHeight + 20.dp else reserved + 8.dp

    // The page bar hides behind the page dots and comes out on tap for a few seconds.
    var barShown by remember { mutableStateOf(false) }
    val barVisible = dock.pageBarAlways || barShown
    LaunchedEffect(barShown, pager.currentPage) {
        if (barShown && !dock.pageBarAlways) {
            delay(PAGE_BAR_TIMEOUT_MS)
            barShown = false
        }
    }

    // No Surface at the root (the wallpaper shows through), so set the content color here.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.5f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        ) {
            val cameraWidth = dock.cameraWidthDp.dp.coerceIn(0.dp, maxOf(0.dp, maxWidth - MinDockWidth))
            val cameraLeft = dock.cameraSide == CameraSide.LEFT

            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { pages[it].name },
            ) { index ->
                val pageModifier = Modifier
                    .fillMaxSize()
                    .padding(start = edge, end = edge, top = edge, bottom = contentBottom)
                when (pages[index]) {
                    CoverPage.HOME -> HomePage(
                        state = state,
                        actions = actions,
                        onOpenNotifications = { goTo(CoverPage.NOTIFICATIONS) },
                        modifier = pageModifier,
                    )
                    CoverPage.NOTIFICATIONS -> NotificationsPage(state, actions, pageModifier)
                    CoverPage.MEDIA -> MediaPage(state, actions, pageModifier)
                    CoverPage.APPS -> AppsPage(state, actions, pageModifier)
                    CoverPage.WIDGETS -> WidgetsPage(state, actions, pageModifier, widget)
                    CoverPage.TOGGLES -> TogglesPage(state, actions, pageModifier)
                }
            }

            if (state.showCameraGuide && cameraWidth > 0.dp && bottom > 0.dp) {
                CameraGuide(
                    width = cameraWidth,
                    height = bottom,
                    outerSideIsEnd = !cameraLeft,
                    modifier = Modifier.align(if (cameraLeft) Alignment.BottomStart else Alignment.BottomEnd),
                )
            }

            // The strip beside the camera block; the dock sits centred in it.
            if (dockRows.isNotEmpty()) {
                Box(
                    Modifier
                        .align(if (cameraLeft) Alignment.BottomEnd else Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(reserved)
                        .padding(
                            start = if (cameraLeft) cameraWidth else edge,
                            end = if (cameraLeft) edge else cameraWidth,
                            bottom = DockMetrics.BottomMargin,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(DockMetrics.Gap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (dockMedia) {
                        DockMedia(
                            state = state,
                            actions = actions,
                            onClick = { if (!goTo(CoverPage.MEDIA)) actions.mediaOpen() },
                        )
                    }
                    if (dockNotifications) {
                        DockNotifications(state, onClick = { goTo(CoverPage.NOTIFICATIONS) })
                    }
                    if (!dock.pageBarAlways) {
                        PageDots(
                            pages = pages.size,
                            current = pager.currentPage,
                            onClick = { barShown = !barShown },
                        )
                    }
                }
                }
            }

            AnimatedVisibility(
                visible = barVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = reserved + 12.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.85f) + slideInVertically { it / 2 },
                exit = fadeOut() + scaleOut(targetScale = 0.85f) + slideOutVertically { it / 2 },
            ) {
                PageBar(
                    pages = pages,
                    current = pager.currentPage,
                    notificationCount = state.notifications.size,
                    onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                )
            }
        }
    }
}

private const val PAGE_BAR_TIMEOUT_MS = 4_000L

/** The dock never gets narrower than this, whatever the camera width setting says. */
private val MinDockWidth = 160.dp

val CoverPage.icon: ImageVector
    get() = when (this) {
        CoverPage.HOME -> Icons.Rounded.Home
        CoverPage.NOTIFICATIONS -> Icons.Rounded.Notifications
        CoverPage.MEDIA -> Icons.Rounded.MusicNote
        CoverPage.APPS -> Icons.Rounded.Apps
        CoverPage.WIDGETS -> Icons.Rounded.Widgets
        CoverPage.TOGGLES -> Icons.Rounded.ToggleOn
    }

val CoverPage.labelRes: Int
    get() = when (this) {
        CoverPage.HOME -> R.string.page_home
        CoverPage.NOTIFICATIONS -> R.string.page_notifications
        CoverPage.MEDIA -> R.string.page_media
        CoverPage.APPS -> R.string.page_apps
        CoverPage.WIDGETS -> R.string.page_widgets
        CoverPage.TOGGLES -> R.string.page_toggles
    }
