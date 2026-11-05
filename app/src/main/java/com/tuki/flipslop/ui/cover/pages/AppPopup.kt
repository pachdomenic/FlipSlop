package com.tuki.flipslop.ui.cover.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tuki.flipslop.R
import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.apps.AppMenu
import com.tuki.flipslop.apps.AppShortcut
import com.tuki.flipslop.ui.components.ShapedAppIcon
import com.tuki.flipslop.ui.cover.CoverActions

// Launcher3's popup metrics (bg_popup_item_*, deep_shortcut_icon_size).
private val PopupWidth = 234.dp
private val ItemHeight = 56.dp
private val ShortcutIconSize = 32.dp
private val ItemPadding = 16.dp
private val IconTextGap = 14.dp
private val Corner = 24.dp

/** Room around the cards so their shadow is not cut off by the popup window. */
private val ShadowRoom = 6.dp

/**
 * The home screen's long-press popup for an app: its shortcuts as a list,
 * then the system actions (pin, app info, uninstall) as a row of icons, in a
 * card of their own. With no shortcuts the actions are a labelled list, as
 * Launcher3 does. Opens above the icon when there is room, else below.
 */
@Composable
internal fun AppPopup(
    app: AppEntry,
    menu: AppMenu?,
    pinned: Boolean,
    themed: Boolean,
    actions: CoverActions,
    onDismiss: () -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = menu != null
    // Keep the last contents while the popup animates out.
    var shown by remember { mutableStateOf(AppMenu()) }
    if (menu != null) shown = menu
    if (!visible.currentState && !visible.targetState) return

    val density = LocalDensity.current
    var above by remember { mutableStateOf(true) }
    val position = remember(density) {
        with(density) {
            AnchoredPosition(margin = 8.dp.roundToPx(), gap = (2.dp - ShadowRoom).roundToPx()) { above = it }
        }
    }
    val origin = TransformOrigin(0.5f, if (above) 1f else 0f)
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(150)) + scaleIn(tween(200), initialScale = 0.8f, transformOrigin = origin),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f, transformOrigin = origin),
        ) {
            PopupContent(app, shown, pinned, themed, actions, onDismiss)
        }
    }
}

@Composable
private fun PopupContent(
    app: AppEntry,
    menu: AppMenu,
    pinned: Boolean,
    themed: Boolean,
    actions: CoverActions,
    onDismiss: () -> Unit,
) {
    val system = buildList {
        add(
            SystemAction(
                icon = if (pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin,
                label = stringResource(if (pinned) R.string.unpin else R.string.pin),
            ) { actions.togglePin(app) },
        )
        add(SystemAction(Icons.Rounded.Info, stringResource(R.string.app_info)) { actions.openAppInfo(app) })
        if (menu.canUninstall) {
            add(SystemAction(Icons.Rounded.Delete, stringResource(R.string.app_uninstall)) { actions.uninstallApp(app) })
        }
    }
    Column(
        Modifier
            .padding(ShadowRoom)
            .widthIn(max = PopupWidth)
            .width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (menu.shortcuts.isNotEmpty()) {
            PopupCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    menu.shortcuts.forEach { shortcut ->
                        ShortcutRow(shortcut, fallbackIcon = app, themed = themed) {
                            onDismiss()
                            actions.launchShortcut(shortcut)
                        }
                    }
                }
            }
            PopupCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    system.forEach { action ->
                        IconButton(onClick = {
                            onDismiss()
                            action.run()
                        }) {
                            Icon(
                                action.icon,
                                contentDescription = action.label,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            PopupCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    system.forEach { action ->
                        PopupRow(
                            onClick = {
                                onDismiss()
                                action.run()
                            },
                            leading = {
                                Icon(
                                    action.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            text = action.label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Corner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

/** Long label when it fits, else the short one (Launcher3's DeepShortcutView). */
@Composable
private fun ShortcutRow(shortcut: AppShortcut, fallbackIcon: AppEntry, themed: Boolean, onClick: () -> Unit) {
    val style = MaterialTheme.typography.bodyLarge
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val label = remember(shortcut, style, density) {
        val long = shortcut.longLabel ?: return@remember shortcut.label
        val room = with(density) { (PopupWidth - ItemPadding * 2 - ShortcutIconSize - IconTextGap).roundToPx() }
        val width = measurer.measure(long, style, maxLines = 1, softWrap = false).size.width
        if (width <= room) long else shortcut.label
    }
    PopupRow(
        onClick = onClick,
        leading = {
            ShapedAppIcon(shortcut.icon ?: fallbackIcon.icon, CircleShape, size = ShortcutIconSize, themed = themed)
        },
        text = label,
    )
}

@Composable
private fun PopupRow(onClick: () -> Unit, leading: @Composable () -> Unit, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = ItemPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(IconTextGap))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private class SystemAction(val icon: ImageVector, val label: String, val run: () -> Unit)

/** Centred over the anchor, above it when it fits, kept [margin] inside the window. */
private class AnchoredPosition(
    private val margin: Int,
    private val gap: Int,
    private val onSide: (above: Boolean) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(margin, maxX)
        val aboveY = anchorBounds.top - popupContentSize.height - gap
        val above = aboveY >= margin
        onSide(above)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val y = if (above) aboveY else (anchorBounds.bottom + gap).coerceAtMost(maxY)
        return IntOffset(x, y)
    }
}
