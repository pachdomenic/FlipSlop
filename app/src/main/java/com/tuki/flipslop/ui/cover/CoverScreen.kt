package com.tuki.flipslop.ui.cover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.ui.cover.pages.AppsPage
import com.tuki.flipslop.ui.cover.pages.HomePage
import com.tuki.flipslop.ui.cover.pages.MediaPage
import com.tuki.flipslop.ui.cover.pages.NotificationsPage
import com.tuki.flipslop.ui.cover.pages.TogglesPage
import com.tuki.flipslop.ui.cover.pages.WidgetsPage
import kotlinx.coroutines.launch

/** Height of the floating toolbar plus its breathing room. */
private val ToolbarSpace = 88.dp

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

    // No Surface at the root (the wallpaper shows through), so set the content color here.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Box(
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
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { pages[it].name },
        ) { index ->
            val pageModifier = Modifier
                .fillMaxSize()
                .padding(start = edge, end = edge, top = edge, bottom = bottom + ToolbarSpace)
            when (pages[index]) {
                CoverPage.HOME -> HomePage(
                    state = state,
                    actions = actions,
                    onOpenNotifications = {
                        val i = pages.indexOf(CoverPage.NOTIFICATIONS)
                        if (i >= 0) scope.launch { pager.animateScrollToPage(i) }
                    },
                    modifier = pageModifier,
                )
                CoverPage.NOTIFICATIONS -> NotificationsPage(state, actions, pageModifier)
                CoverPage.MEDIA -> MediaPage(state, actions, pageModifier)
                CoverPage.APPS -> AppsPage(state, actions, pageModifier)
                CoverPage.WIDGETS -> WidgetsPage(state, actions, pageModifier, widget)
                CoverPage.TOGGLES -> TogglesPage(state, actions, pageModifier)
            }
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottom + 12.dp),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        ) {
            pages.forEachIndexed { index, page ->
                val selected = pager.currentPage == index
                val onClick: () -> Unit = { scope.launch { pager.animateScrollToPage(index) } }
                val label = stringResource(page.labelRes)
                val icon: @Composable () -> Unit = {
                    val count = if (page == CoverPage.NOTIFICATIONS) state.notifications.size else 0
                    if (count > 0 && !selected) {
                        BadgedBox(badge = { Badge { Text(count.coerceAtMost(99).toString()) } }) {
                            Icon(page.icon, contentDescription = label)
                        }
                    } else {
                        Icon(page.icon, contentDescription = label)
                    }
                }
                if (selected) {
                    FilledIconButton(
                        onClick = onClick,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        content = icon,
                    )
                } else {
                    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes(), content = icon)
                }
            }
        }
    }
    }
}

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
