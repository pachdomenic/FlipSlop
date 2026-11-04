package com.tuki.flipslop.ui.cover.pages

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.data.IconShapeStyle
import com.tuki.flipslop.ui.components.ShapedAppIcon
import com.tuki.flipslop.ui.components.rememberIconShape
import com.tuki.flipslop.ui.cover.CoverActions
import com.tuki.flipslop.ui.cover.CoverUiState

@Composable
fun AppsPage(state: CoverUiState, actions: CoverActions, modifier: Modifier = Modifier) {
    var showAll by rememberSaveable { mutableStateOf(state.pinnedApps.isEmpty()) }
    val pinnedKeys = state.pinnedApps.map { it.key }.toSet()
    val apps = if (showAll) state.allApps else state.pinnedApps

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ToggleButton(
                checked = !showAll,
                onCheckedChange = { showAll = false },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
            ) { Text(stringResource(R.string.apps_pinned)) }
            ToggleButton(
                checked = showAll,
                onCheckedChange = { showAll = true },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Rounded.Apps, contentDescription = null) },
            ) { Text(stringResource(R.string.apps_all)) }
        }
        Spacer(Modifier.height(12.dp))
        if (apps.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.PushPin,
                text = stringResource(R.string.apps_pinned_empty),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                    AppCell(
                        app = app,
                        index = index,
                        pinned = app.key in pinnedKeys,
                        showPinMark = showAll,
                        iconShape = state.iconShape,
                        themed = state.themedIcons,
                        onClick = { actions.launchApp(app) },
                        onLongClick = { actions.togglePin(app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCell(
    app: AppEntry,
    index: Int,
    pinned: Boolean,
    showPinMark: Boolean,
    iconShape: IconShapeStyle,
    themed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onLongClickLabel = stringResource(if (pinned) R.string.unpin else R.string.pin),
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            ShapedAppIcon(app.icon, rememberIconShape(index, iconShape), size = 64.dp, themed = themed)
            if (showPinMark && pinned) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
