package com.tuki.flipslop.ui.cover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.data.LockLayout
import com.tuki.flipslop.data.LockNotifications
import com.tuki.flipslop.data.UnlockGesture
import com.tuki.flipslop.notifications.LockLine
import com.tuki.flipslop.notifications.forLockScreen
import com.tuki.flipslop.ui.cover.pages.EmptyState
import com.tuki.flipslop.ui.cover.pages.MediaPage
import com.tuki.flipslop.ui.cover.pages.TogglesPage
import kotlinx.coroutines.launch

/**
 * The cover while the device is locked, as set up under Lock screen: the
 * clock (cards or big), music, notifications within Android's lock screen
 * rules, and optionally a few pages to swipe to without unlocking. The
 * unlock gesture on the main page brings up the bouncer (or the fingerprint
 * prompt); once the device unlocks the full cover takes over.
 *
 * [pages] / [pager] are the locked pages (LockConfig.pages); [contentModifier]
 * places a page above the camera strip, [stripModifier] the page dots and the
 * unlock hint beside the cameras.
 */
@Composable
fun LockScreen(
    state: CoverUiState,
    actions: CoverActions,
    pages: List<CoverPage>,
    pager: PagerState,
    modifier: Modifier = Modifier,
    contentModifier: Modifier,
    stripModifier: Modifier,
    /** The music chip and the player card open the music slide. */
    onOpenMedia: () -> Unit = actions::mediaOpen,
) {
    val scope = rememberCoroutineScope()
    val hidden = stringResource(R.string.lock_contents_hidden)
    // Everything that may show while locked, already redacted.
    val lines = remember(state.notifications, state.lockPolicy, hidden) {
        state.notifications.mapNotNull { it.forLockScreen(state.lockPolicy, hidden) }
    }
    val home = pages.indexOf(CoverPage.HOME).coerceAtLeast(0)
    val goTo: (CoverPage) -> Boolean = { page ->
        val i = pages.indexOf(page)
        if (i >= 0) scope.launch { pager.animateScrollToPage(i) }
        i >= 0
    }

    Box(modifier.fillMaxSize()) {
        if (pages.size > 1) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { pages[it].name },
            ) { index ->
                when (pages[index]) {
                    CoverPage.HOME -> LockHome(state, actions, lines, contentModifier, goTo, onOpenMedia)
                    CoverPage.NOTIFICATIONS -> LockNotificationsPage(state, actions, lines, contentModifier)
                    CoverPage.MEDIA -> MediaPage(state, actions, contentModifier)
                    CoverPage.TOGGLES -> TogglesPage(state, actions, contentModifier)
                    // LockConfig never lets apps or widgets through.
                    else -> Unit
                }
            }
        } else {
            LockHome(state, actions, lines, contentModifier, goTo, onOpenMedia)
        }

        Box(stripModifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.lock.hint) UnlockHint(state, onClick = actions::unlock)
                if (pages.size > 1) {
                    PageDots(
                        pages = pages.size,
                        current = pager.currentPage,
                        onClick = { scope.launch { pager.animateScrollToPage(home) } },
                    )
                }
            }
        }
    }
}

/** The main lock page: clock, notifications and music as configured. */
@Composable
private fun LockHome(
    state: CoverUiState,
    actions: CoverActions,
    lines: List<LockLine>,
    modifier: Modifier,
    onOpenNotifications: (CoverPage) -> Boolean,
    onOpenMedia: () -> Unit,
) {
    val lock = state.lock
    val shown = if (lock.media) state else state.copy(media = null)
    val cards = if (lock.notifications == LockNotifications.CARDS) lines else emptyList()
    val icons: (@Composable () -> Unit)? = if (lock.notifications == LockNotifications.ICONS && lines.isNotEmpty()) {
        {
            DockNotifications(
                state.copy(notifications = lines.map { it.item }),
                onClick = { if (!onOpenNotifications(CoverPage.NOTIFICATIONS)) actions.unlock() },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    } else {
        null
    }

    Box(Modifier.fillMaxSize().unlockGestures(lock.gesture, actions::unlock)) {
        when (lock.layout) {
            LockLayout.CARDS -> CardsLayout(shown, actions, cards, modifier, belowClock = icons, onOpenMedia = onOpenMedia)
            LockLayout.BIG_CLOCK -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                BigClock(shown, Modifier.weight(1f).fillMaxWidth())
                icons?.invoke()
                // The big clock keeps most of the page: fewer cards, and the player as a pill.
                cards.take(if (shown.media != null) 1 else 2).forEach { line ->
                    Spacer(Modifier.height(6.dp))
                    CompactNotificationCard(line, onClick = { actions.openNotification(line.item) })
                }
                if (shown.media != null) {
                    Spacer(Modifier.height(8.dp))
                    DockMedia(
                        shown,
                        actions,
                        onClick = onOpenMedia,
                    )
                }
            }
        }
    }
}

/** Notifications page while locked: the redacted cards, nothing to reply to or expand. */
@Composable
private fun LockNotificationsPage(state: CoverUiState, actions: CoverActions, lines: List<LockLine>, modifier: Modifier) {
    when {
        !state.lockPolicy.showNotifications -> EmptyState(
            icon = Icons.Rounded.NotificationsOff,
            text = stringResource(R.string.lock_notifications_off_by_system),
            modifier = modifier,
        )
        lines.isEmpty() -> EmptyState(
            icon = Icons.Rounded.NotificationsNone,
            text = stringResource(R.string.notifications_empty),
            modifier = modifier,
        )
        else -> Column(modifier) {
            Text(stringResource(R.string.page_notifications), style = MaterialTheme.typography.titleLargeEmphasized)
            Spacer(Modifier.size(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lines, key = { it.item.key }) { line ->
                    CompactNotificationCard(line, onClick = { actions.openNotification(line.item) })
                }
            }
        }
    }
}

/** Lock glyph and how to get in; tapping it always unlocks, whatever the gesture. */
@Composable
private fun UnlockHint(state: CoverUiState, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClickLabel = stringResource(R.string.unlock), onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            if (state.fingerprintUnlock) Icons.Rounded.Fingerprint else Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryFixed,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(state.lock.gesture.hintRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private val UnlockGesture.hintRes: Int
    get() = when (this) {
        UnlockGesture.TAP_OR_SWIPE -> R.string.lock_hint
        UnlockGesture.SWIPE -> R.string.lock_hint_swipe
        UnlockGesture.DOUBLE_TAP -> R.string.lock_hint_double_tap
    }

/** Swipe up always unlocks; a tap or a double tap too, depending on [gesture]. */
@Composable
private fun Modifier.unlockGestures(gesture: UnlockGesture, unlock: () -> Unit): Modifier {
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    return this
        .pointerInput(gesture) {
            when (gesture) {
                UnlockGesture.TAP_OR_SWIPE -> detectTapGestures(onTap = { unlock() })
                UnlockGesture.DOUBLE_TAP -> detectTapGestures(onDoubleTap = { unlock() })
                UnlockGesture.SWIPE -> Unit
            }
        }
        .pointerInput(Unit) {
            var dragged = 0f
            detectVerticalDragGestures(
                onDragStart = { dragged = 0f },
                onDragEnd = { if (dragged < -swipeThreshold) unlock() },
            ) { _, dy -> dragged += dy }
        }
}
