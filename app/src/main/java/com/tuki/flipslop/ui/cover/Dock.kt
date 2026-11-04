package com.tuki.flipslop.ui.cover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.ui.components.IconImage

/** Fixed row heights, so the pager knows how much room the dock takes. */
object DockMetrics {
    val MediaHeight = 44.dp
    val NotificationsHeight = 36.dp
    val ToolbarHeight = 48.dp
    val FloatingToolbarHeight = 64.dp
    val Gap = 6.dp
    val BottomMargin = 8.dp
}

/** Page switcher. `compact` shrinks it to fit beside the camera block. */
@Composable
fun PageBar(
    pages: List<CoverPage>,
    current: Int,
    notificationCount: Int,
    compact: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonSize: Dp = if (compact) 40.dp else 48.dp
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides if (compact) 0.dp else 48.dp) {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = modifier.then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            contentPadding = if (compact) PaddingValues(4.dp) else FloatingToolbarDefaults.ContentPadding,
            expandedShadowElevation = if (compact) 0.dp else FloatingToolbarDefaults.ContainerExpandedElevation,
        ) {
            pages.forEachIndexed { index, page ->
                val selected = current == index
                val label = stringResource(page.labelRes)
                val icon: @Composable () -> Unit = {
                    val count = if (page == CoverPage.NOTIFICATIONS) notificationCount else 0
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
                        onClick = { onSelect(index) },
                        modifier = Modifier.size(buttonSize),
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        content = icon,
                    )
                } else {
                    IconButton(
                        onClick = { onSelect(index) },
                        modifier = Modifier.size(buttonSize),
                        shapes = IconButtonDefaults.shapes(),
                        content = icon,
                    )
                }
            }
        }
    }
}

/** App icons of pending notifications, like the stock cover's notification row. */
@Composable
fun DockNotifications(state: CoverUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val apps = state.notifications.distinctBy { it.packageName }
    val shown = apps.take(MAX_ICONS)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        modifier = modifier.height(DockMetrics.NotificationsHeight).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            shown.forEach { n ->
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    IconImage(
                        n.smallIcon,
                        size = 15.dp,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                        fallback = n.appIcon,
                    )
                }
            }
            val more = apps.size - shown.size
            if (more > 0) {
                Text("+$more", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 4.dp))
            }
        }
    }
}

/** Now playing pill: art, title, play/pause. Tapping it opens the media page. */
@Composable
fun DockMedia(state: CoverUiState, actions: CoverActions, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val media = state.media ?: return
    val art = remember(media.art) { media.art?.asImageBitmap() }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        modifier = modifier.height(DockMetrics.MediaHeight).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp))
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                media.title.ifBlank { media.appLabel },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                FilledIconButton(
                    onClick = actions::mediaPlayPause,
                    modifier = Modifier.size(32.dp),
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (media.isPlaying) R.string.pause else R.string.play),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Dark outline of the camera block (flash + two lenses, lenses on the outer
 * side), for the settings preview and screenshots.
 */
@Composable
fun CameraGuide(width: Dp, height: Dp, outerSideIsEnd: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width, height)
            .padding(6.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black),
        contentAlignment = if (outerSideIsEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        val lens = minOf((width.value - 40f) / 2.3f, height.value * 0.72f).coerceAtLeast(8f).dp
        val parts = @Composable {
            Box(Modifier.size(lens / 5).clip(CircleShape).background(Color(0xFF2A2A30)))
            repeat(2) { Box(Modifier.size(lens).clip(CircleShape).background(Color(0xFF1C1C22))) }
        }
        Row(
            Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (outerSideIsEnd) {
                parts()
            } else {
                // Mirror: lenses first, flash towards the middle.
                repeat(2) { Box(Modifier.size(lens).clip(CircleShape).background(Color(0xFF1C1C22))) }
                Box(Modifier.size(lens / 5).clip(CircleShape).background(Color(0xFF2A2A30)))
            }
        }
    }
}

private const val MAX_ICONS = 6

