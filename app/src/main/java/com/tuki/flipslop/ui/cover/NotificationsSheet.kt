package com.tuki.flipslop.ui.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.notifications.forLockScreen

/**
 * Notifications on top of the locked cover, for when Notifications is not
 * one of the lock screen's pages: a tap on the notification icons (beside
 * the cameras or under the clock) shows them without unlocking. Same cards
 * and same Android lock screen rules as the locked Notifications page:
 * secret ones never show, private ones are redacted unless the system allows
 * private content. A tap on a card still unlocks and opens it. Swipe down,
 * Back or the button beside the cameras closes it.
 */
@Composable
fun LockNotificationsSheet(
    state: CoverUiState,
    actions: CoverActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier,
    stripModifier: Modifier,
) {
    val hidden = stringResource(R.string.lock_contents_hidden)
    val lines = remember(state.notifications, state.lockPolicy, hidden) {
        state.notifications.mapNotNull { it.forLockScreen(state.lockPolicy, hidden) }
    }
    val closeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            // Keeps taps and drags from reaching the lock screen underneath.
            .pointerInput(Unit) { detectTapGestures() }
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged > closeThreshold) onClose() },
                ) { _, dy -> dragged += dy }
            },
    ) {
        LockNotificationsPage(state, actions, lines, contentModifier)
        Box(stripModifier, contentAlignment = Alignment.Center) {
            FilledTonalButton(onClick = onClose, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.media_sheet_close))
            }
        }
    }
}
