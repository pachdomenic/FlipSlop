package com.tuki.flipslop.ui.cover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.data.HomeStyle
import com.tuki.flipslop.data.LockHintPosition
import com.tuki.flipslop.ui.cover.pages.AppsPage
import com.tuki.flipslop.ui.cover.pages.HomePage
import com.tuki.flipslop.ui.cover.pages.MediaPage
import com.tuki.flipslop.ui.cover.pages.NotificationsPage
import com.tuki.flipslop.ui.cover.pages.TogglesPage
import com.tuki.flipslop.ui.cover.pages.WidgetsPage
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CoverScreen(
    state: CoverUiState,
    actions: CoverActions,
    modifier: Modifier = Modifier,
    initialPage: CoverPage = CoverPage.HOME,
    /** Height of a visible status bar; content starts below it. */
    topInset: Dp = 0.dp,
    widget: @Composable (id: Int, modifier: Modifier) -> Unit = { _, _ -> },
) {
    val pages = state.pages.ifEmpty { listOf(CoverPage.HOME) }
    val pager = rememberPagerState(initialPage = pages.indexOf(initialPage).coerceAtLeast(0)) { pages.size }
    val scope = rememberCoroutineScope()
    val home = pages.indexOf(CoverPage.HOME).coerceAtLeast(0)

    val lockView = state.locked && state.lock.enabled
    val lockPages = remember(state.lock, pages) { state.lock.pages(pages) }
    val lockPager = rememberPagerState(initialPage = lockPages.indexOf(CoverPage.HOME).coerceAtLeast(0)) { lockPages.size }

    // Back on the cover returns to the home pane instead of leaving the launcher.
    BackHandler(enabled = !lockView && pager.currentPage != home) {
        scope.launch { pager.animateScrollToPage(home) }
    }
    val lockHome = lockPages.indexOf(CoverPage.HOME).coerceAtLeast(0)
    BackHandler(enabled = lockView && lockPager.currentPage != lockHome) {
        scope.launch { lockPager.animateScrollToPage(lockHome) }
    }
    // Every time the device locks, start again from the clock.
    LaunchedEffect(lockView) {
        if (lockView) lockPager.scrollToPage(lockHome)
    }

    // Music chips and the player card go to the Music page (or the lock screen's Music page)
    // when there is one, and open the music slide over everything when there is not.
    var mediaSheet by remember { mutableStateOf(false) }
    val openMedia: () -> Unit = {
        val (shown, shownPager) = if (lockView) lockPages to lockPager else pages to pager
        val i = shown.indexOf(CoverPage.MEDIA)
        if (i >= 0) scope.launch { shownPager.animateScrollToPage(i) } else mediaSheet = true
    }
    BackHandler(enabled = mediaSheet) { mediaSheet = false }
    LaunchedEffect(state.media == null) {
        if (state.media == null) mediaSheet = false
    }

    // Notification icons on the lock screen: the lock screen's Notifications page when there is
    // one, otherwise the notifications slide, all without unlocking. Unlocking closes it,
    // and so does the screen going off: the next wake starts from the clock.
    var notificationsSheet by remember { mutableStateOf(false) }
    BackHandler(enabled = notificationsSheet) { notificationsSheet = false }
    LaunchedEffect(lockView) {
        if (!lockView) notificationsSheet = false
    }
    LaunchedEffect(state.hiddenCount) { notificationsSheet = false }

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
    // Counted with every pill, even one hidden on its own page, so pages don't jump.
    val showDots = !dock.pageBarAlways && dock.showPageDots
    val dockRows = listOfNotNull(
        DockMetrics.MediaHeight.takeIf { dockMedia },
        DockMetrics.NotificationsHeight.takeIf { dockNotifications },
        DockMetrics.DotsHeight.takeIf { showDots },
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
        BoxWithConstraints(modifier.fillMaxSize()) {
            // Album art behind everything, fading in as the media page comes into view.
            val art = state.media?.art
            // Locked, the pages to swipe through are the lock screen's own.
            val shownPages = if (lockView) lockPages else pages
            val shownPager = if (lockView) lockPager else pager
            val mediaIndex = shownPages.indexOf(CoverPage.MEDIA)
            val artVisibility by remember(mediaIndex, shownPager) {
                derivedStateOf {
                    if (mediaIndex < 0) 0f
                    else 1f - min(1f, abs(shownPager.currentPage + shownPager.currentPageOffsetFraction - mediaIndex))
                }
            }
            if (state.mediaArtBackground && art != null && mediaIndex >= 0) {
                val bitmap = remember(art) { art.asImageBitmap() }
                Image(
                    bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = artVisibility }
                        .then(if (state.artBlurDp > 0) Modifier.blur(state.artBlurDp.dp) else Modifier),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.5f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
            )

            val cameraWidth = dock.cameraWidthDp.dp.coerceIn(0.dp, maxOf(0.dp, maxWidth - MinDockWidth))
            val cameraLeft = dock.cameraSide == CameraSide.LEFT

            // The lock screen and the music slide: one page above the strip beside the cameras.
            val stripAlign = if (cameraLeft) Alignment.BottomEnd else Alignment.BottomStart
            // Locked, the strip may hold the hint, the chips and the page dots: make room for them.
            val lockStripRows = if (!lockView) {
                emptyList()
            } else {
                listOfNotNull(
                    LockHintHeight.takeIf { state.lock.hintPosition == LockHintPosition.STRIP },
                    DockMetrics.MediaHeight.takeIf { state.lock.mediaInStrip(state.media != null) },
                    DockMetrics.NotificationsHeight.takeIf { state.lock.notificationsInStrip(state.notifications.isNotEmpty()) },
                    DockMetrics.DotsHeight.takeIf { lockPages.size > 1 },
                )
            }
            val lockStripNeeded = if (lockStripRows.isEmpty()) {
                0.dp
            } else {
                lockStripRows.fold(0.dp) { acc, h -> acc + h } + DockMetrics.Gap * (lockStripRows.size - 1) + DockMetrics.BottomMargin
            }
            val stripHeight = maxOf(bottom, 56.dp, lockStripNeeded)
            val lockContent = Modifier
                .fillMaxSize()
                .padding(start = edge, end = edge, top = maxOf(edge, topInset + 8.dp), bottom = stripHeight + 8.dp)

            if (lockView) {
                LockScreen(
                    state = state,
                    actions = actions,
                    pages = lockPages,
                    pager = lockPager,
                    onOpenMedia = openMedia,
                    onOpenNotifications = { notificationsSheet = true },
                    contentModifier = lockContent,
                    stripModifier = Modifier
                        .align(stripAlign)
                        .fillMaxWidth()
                        .height(stripHeight)
                        .padding(
                            start = if (cameraLeft) cameraWidth else edge,
                            end = if (cameraLeft) edge else cameraWidth,
                            bottom = DockMetrics.BottomMargin,
                        ),
                )
            } else {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { pages[it].name },
                ) { index ->
                    val pageModifier = Modifier
                        .fillMaxSize()
                        .padding(start = edge, end = edge, top = maxOf(edge, topInset + 8.dp), bottom = contentBottom)
                    when (pages[index]) {
                        CoverPage.HOME -> HomePage(
                            state = state,
                            actions = actions,
                            onOpenNotifications = { goTo(CoverPage.NOTIFICATIONS) },
                            onOpenMedia = openMedia,
                            modifier = pageModifier,
                        )
                        CoverPage.NOTIFICATIONS -> NotificationsPage(state, actions, pageModifier)
                        CoverPage.MEDIA -> MediaPage(state, actions, pageModifier)
                        CoverPage.APPS -> AppsPage(state, actions, pageModifier)
                        CoverPage.WIDGETS -> WidgetsPage(state, actions, pageModifier, widget)
                        CoverPage.TOGGLES -> TogglesPage(state, actions, pageModifier)
                    }
                }
            }

            if (!lockView) {
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
                            // A pill steps aside on its own page: the page already shows the same thing.
                            // targetPage, so it starts leaving while the swipe is still going.
                            val target = pages.getOrNull(pager.targetPage)
                            // Home in cards mode already shows the player and notifications.
                            val cardsHome = state.homeStyle == HomeStyle.CARDS && !(state.isIdle && state.idleAospClock)
                            AnimatedVisibility(
                                visible = dockMedia && target != CoverPage.MEDIA && !(target == CoverPage.HOME && cardsHome),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                DockMedia(
                                    state = state,
                                    actions = actions,
                                    onClick = openMedia,
                                )
                            }
                            AnimatedVisibility(
                                visible = dockNotifications && target != CoverPage.NOTIFICATIONS && !(target == CoverPage.HOME && cardsHome),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                DockNotifications(state, onClick = { goTo(CoverPage.NOTIFICATIONS) })
                            }
                            if (showDots) {
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

            AnimatedVisibility(
                visible = notificationsSheet && lockView,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                LockNotificationsSheet(
                    state = state,
                    actions = actions,
                    onClose = { notificationsSheet = false },
                    contentModifier = lockContent,
                    stripModifier = Modifier
                        .align(stripAlign)
                        .fillMaxWidth()
                        .height(stripHeight)
                        .padding(
                            start = if (cameraLeft) cameraWidth else edge,
                            end = if (cameraLeft) edge else cameraWidth,
                            bottom = DockMetrics.BottomMargin,
                        ),
                )
            }

            AnimatedVisibility(
                visible = mediaSheet && state.media != null,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                MediaSheet(
                    state = state,
                    actions = actions,
                    onClose = { mediaSheet = false },
                    contentModifier = lockContent,
                    stripModifier = Modifier
                        .align(stripAlign)
                        .fillMaxWidth()
                        .height(stripHeight)
                        .padding(
                            start = if (cameraLeft) cameraWidth else edge,
                            end = if (cameraLeft) edge else cameraWidth,
                            bottom = DockMetrics.BottomMargin,
                        ),
                )
            }

            if (state.showCameraGuide && cameraWidth > 0.dp && bottom > 0.dp) {
                CameraGuide(
                    width = cameraWidth,
                    height = bottom,
                    outerSideIsEnd = !cameraLeft,
                    modifier = Modifier.align(if (cameraLeft) Alignment.BottomStart else Alignment.BottomEnd),
                )
            }
        }
    }
}

private const val PAGE_BAR_TIMEOUT_MS = 4_000L

/** The lock glyph over two lines of hint text, when it sits beside the cameras. */
private val LockHintHeight = 60.dp

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
