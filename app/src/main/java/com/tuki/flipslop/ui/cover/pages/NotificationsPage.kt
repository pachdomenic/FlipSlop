package com.tuki.flipslop.ui.cover.pages

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.data.NotificationStyle
import com.tuki.flipslop.notifications.CoverNotification
import com.tuki.flipslop.notifications.LockLine
import androidx.compose.animation.animateContentSize
import com.tuki.flipslop.ui.cover.CompactNotificationCard
import com.tuki.flipslop.ui.cover.ExpandButton
import com.tuki.flipslop.ui.cover.NotificationDetails
import com.tuki.flipslop.ui.components.IconImage
import com.tuki.flipslop.ui.cover.CoverActions
import com.tuki.flipslop.ui.cover.CoverUiState

@Composable
fun NotificationsPage(state: CoverUiState, actions: CoverActions, modifier: Modifier = Modifier) {
    when {
        !state.notificationAccess -> EmptyState(
            icon = Icons.Rounded.NotificationsActive,
            text = stringResource(R.string.notifications_need_access),
            action = stringResource(R.string.grant),
            onAction = actions::grantNotificationAccess,
            modifier = modifier,
        )
        state.notifications.isEmpty() -> EmptyState(
            icon = Icons.Rounded.NotificationsNone,
            text = stringResource(R.string.notifications_empty),
            modifier = modifier,
        )
        else -> Column(modifier) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.page_notifications),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.weight(1f),
                )
                if (state.notifications.any { it.isClearable }) {
                    FilledTonalIconButton(
                        onClick = actions::dismissAllNotifications,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Rounded.ClearAll, contentDescription = stringResource(R.string.clear_all))
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.notifications, key = { it.key }) { item ->
                    if (state.notificationStyle == NotificationStyle.COMPACT) {
                        DismissibleNotification(item, actions, Modifier.animateItem()) {
                            CompactNotificationCard(
                                LockLine(item, item.title, item.text),
                                onClick = { actions.openNotification(item) },
                            )
                        }
                    } else {
                        NotificationCard(item, state.now, actions, Modifier.animateItem())
                    }
                }
            }
        }
    }
}

/** Swipe either way to dismiss (when the notification can be cleared). */
@Composable
private fun DismissibleNotification(
    item: CoverNotification,
    actions: CoverActions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismiss = rememberSwipeToDismissBoxState()
    if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) {
        // Fired once the swipe settles past the threshold.
        LaunchedEffect(item.key) { actions.dismissNotification(item.key) }
    }
    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier,
        enableDismissFromStartToEnd = item.isClearable,
        enableDismissFromEndToStart = item.isClearable,
        backgroundContent = { Box(Modifier.fillMaxSize()) },
    ) { content() }
}

@Composable
private fun NotificationCard(
    item: CoverNotification,
    now: Long,
    actions: CoverActions,
    modifier: Modifier = Modifier,
) {
    DismissibleNotification(item, actions, modifier) {
        var replying by rememberSaveable(item.key) { mutableStateOf<String?>(null) }
        var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
        var textCut by remember(item.key) { mutableStateOf(false) }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
                .clickable { actions.openNotification(item) },
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accent(item.color)),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconImage(item.smallIcon, 14.dp, colorFilter = ColorFilter.tint(Color.White), fallback = item.appIcon)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.appLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        DateUtils.getRelativeTimeSpanString(item.postTime, now, DateUtils.MINUTE_IN_MILLIS).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.hasDetails || textCut || expanded) {
                        Spacer(Modifier.width(8.dp))
                        ExpandButton(expanded, onClick = { expanded = !expanded })
                    }
                }
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        if (item.title.isNotBlank()) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = if (expanded) 3 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (expanded) {
                            NotificationDetails(item, Modifier.padding(top = 4.dp))
                        } else if (item.text.isNotBlank()) {
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { if (it.hasVisualOverflow) textCut = true },
                            )
                        }
                    }
                    if (item.largeIcon != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(40.dp).clip(MaterialShapes.Cookie9Sided.toShape())) {
                            IconImage(item.largeIcon, 40.dp)
                        }
                    }
                }
                if (item.actions.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.actions.take(MAX_ACTIONS).forEach { action ->
                            TextButton(onClick = {
                                if (action.isReply) replying = "" else actions.runAction(action)
                            }) { Text(action.title, maxLines = 1) }
                        }
                    }
                }
                val replyAction = item.actions.firstOrNull { it.isReply }
                AnimatedVisibility(visible = replying != null && replyAction != null) {
                    ReplyField(
                        value = replying.orEmpty(),
                        onValueChange = { replying = it },
                        onSend = {
                            replyAction?.let { actions.reply(it, replying.orEmpty()) }
                            replying = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyField(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            placeholder = { Text(stringResource(R.string.reply_hint)) },
        )
        Spacer(Modifier.width(6.dp))
        FilledTonalIconButton(onClick = onSend, enabled = value.isNotBlank(), shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = stringResource(R.string.send))
        }
    }
}

@Composable
private fun accent(color: Int): Color =
    if (color == 0 || Color(color).alpha == 0f) MaterialTheme.colorScheme.primary else Color(color).copy(alpha = 1f)

@Composable
internal fun EmptyState(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.size(16.dp))
            Button(onClick = onAction, shapes = androidx.compose.material3.ButtonDefaults.shapes()) { Text(action) }
        }
    }
}


private const val MAX_ACTIONS = 3
