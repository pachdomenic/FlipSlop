package com.tuki.flipslop.ui.cover

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.notifications.forLockScreen

/**
 * The cover while the device is locked: the clock and the music, plus up
 * to three notifications when the user allows it. A tap on any empty spot or a swipe up brings up the bouncer; once
 * the device unlocks the full cover takes over.
 *
 * [contentModifier] places the clock and music above the camera strip,
 * [stripModifier] the unlock hint beside the cameras.
 */
@Composable
fun LockScreen(
    state: CoverUiState,
    actions: CoverActions,
    contentModifier: Modifier,
    stripModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { actions.unlock() }) }
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -swipeThreshold) actions.unlock() },
                ) { _, dy -> dragged += dy }
            },
    ) {
        val hidden = stringResource(R.string.lock_contents_hidden)
        val lines = remember(state.notifications, state.lockPolicy, state.lockNotifications, hidden) {
            if (!state.lockNotifications) {
                emptyList()
            } else {
                state.notifications.mapNotNull { it.forLockScreen(state.lockPolicy, hidden) }.take(MAX_CARD_NOTIFICATIONS)
            }
        }
        CardsLayout(state, actions, lines, contentModifier)
        Box(stripModifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primaryFixed,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.lock_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}
