package com.tuki.flipslop.ui.cover.pages

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuki.flipslop.R
import com.tuki.flipslop.data.ClockStyle
import com.tuki.flipslop.data.HomeStyle
import com.tuki.flipslop.notifications.LockLine
import com.tuki.flipslop.ui.cover.BigClock
import com.tuki.flipslop.ui.cover.date
import com.tuki.flipslop.ui.cover.hours
import com.tuki.flipslop.ui.cover.minutes
import com.tuki.flipslop.ui.cover.style
import com.tuki.flipslop.ui.cover.textColor
import com.tuki.flipslop.ui.cover.CardsLayout
import com.tuki.flipslop.ui.cover.MAX_CARD_NOTIFICATIONS
import com.tuki.flipslop.ui.components.IconImage
import com.tuki.flipslop.ui.cover.CoverActions
import com.tuki.flipslop.ui.cover.CoverUiState
import com.tuki.flipslop.ui.theme.ClockType
import java.util.Date
import java.util.Locale

@Composable
fun HomePage(
    state: CoverUiState,
    actions: CoverActions,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    /** Music chips and the player card open the music slide. */
    onOpenMedia: () -> Unit = actions::mediaOpen,
) {
    if (state.homeStyle == HomeStyle.BIG_CLOCK) {
        BigClock(state, modifier)
        return
    }
    if (state.isIdle && state.idleAospClock) {
        IdleHome(state, modifier)
        return
    }
    if (state.homeStyle == HomeStyle.CARDS) {
        // Same cards as the lock screen, unredacted: the device is unlocked here.
        val lines = state.notifications.take(MAX_CARD_NOTIFICATIONS).map { LockLine(it, it.title, it.text) }
        CardsLayout(
            state = state,
            actions = actions,
            lines = lines,
            modifier = modifier,
            moreCount = state.notifications.size - lines.size,
            onMore = onOpenNotifications,
            onOpenMedia = onOpenMedia,
        )
        return
    }
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        StatusRow(state)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Size the clock to whatever height is left above the camera block.
            val scale = (maxHeight.value / 300f).coerceIn(0.5f, 1.15f)
            ExpressiveClock(state, scale)
        }
        // Whatever lives in the camera dock is not repeated here.
        val media = state.media
        if (media != null && !state.dock.media) {
            MiniMedia(state, actions, onOpenMedia)
        } else if (state.notifications.isNotEmpty() && !state.dock.notifications) {
            NotificationSummary(state, onOpenNotifications)
        }
    }
}

/**
 * Nothing pending: a calm clock in the style of the AOSP lock screen. Small
 * date top left, big stacked digits centered, a lock glyph while locked.
 */
@Composable
private fun IdleHome(state: CoverUiState, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val clock = state.clock
    val date = clock.date(state.now, locale).orEmpty()
    val hours = clock.hours(state.now, state.is24h)
    val minutes = minutes(state.now)
    val accent = clock.textColor()

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                date,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            state.battery?.takeUnless { state.systemStatusBar }?.let { b ->
                Text(
                    "${b.level}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            // Two lines of digits, the lock glyph and some air must fit above the dock.
            val needed = if (state.locked) 340f else 280f
            val scale = (maxHeight.value / needed).coerceIn(0.5f, 1f)
            Column(
                Modifier.padding(top = (24 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val style = clock.style(
                    ClockType.idle.fontSize * scale * clock.scale / 100f,
                    ClockType.idle.lineHeight * scale * clock.scale / 100f,
                )
                Digits(hours, style, accent)
                Digits(minutes, style, accent)
                if (state.locked) {
                    Spacer(Modifier.height((20 * scale).dp))
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusRow(state: CoverUiState) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(state.now / 60_000) {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMM")
        DateFormat.format(pattern, Date(state.now)).toString()
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            date.replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.locked) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        state.battery?.takeUnless { state.systemStatusBar }?.let { b ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (b.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${b.level}%", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ExpressiveClock(state: CoverUiState, scale: Float) {
    val date = Date(state.now)
    val hours = state.clock.hours(state.now, state.is24h)
    val minutes = DateFormat.format("mm", date).toString()
    val colors = MaterialTheme.colorScheme

    // A slowly turning cookie behind the digits, nudged every minute.
    val turn by animateFloatAsState(
        // Wraps once every 40 hours, so the spring never has to rewind.
        targetValue = (state.now / 60_000 % 2400).toFloat() * 15f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "clockTurn",
    )
    val cookie = MaterialShapes.Cookie12Sided.toShape()

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((240 * scale).dp)
                .rotate(turn)
                .clip(cookie)
                .background(colors.primaryContainer.copy(alpha = 0.4f)),
        )
        when (state.clock.layout) {
            ClockStyle.STACKED -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Digits(hours, ClockType.hours.copy(fontSize = ClockType.hours.fontSize * scale, lineHeight = ClockType.hours.lineHeight * scale), colors.primary)
                Digits(minutes, ClockType.minutes.copy(fontSize = ClockType.minutes.fontSize * scale, lineHeight = ClockType.minutes.lineHeight * scale), colors.tertiary)
            }
            ClockStyle.INLINE -> Row(verticalAlignment = Alignment.CenterVertically) {
                val style = ClockType.inline.copy(fontSize = ClockType.inline.fontSize * scale, lineHeight = ClockType.inline.lineHeight * scale)
                Digits(hours, style, colors.primary)
                Text(":", style = style, color = colors.onSurfaceVariant)
                Digits(minutes, style, colors.tertiary)
            }
        }
    }
}

@Composable
internal fun Digits(value: String, style: androidx.compose.ui.text.TextStyle, color: androidx.compose.ui.graphics.Color) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
        },
        label = "digits",
    ) { text ->
        Text(text, style = style, color = color, maxLines = 1)
    }
}

@Composable
private fun MiniMedia(state: CoverUiState, actions: CoverActions, onOpen: () -> Unit) {
    val media = state.media ?: return
    val art = remember(media.art) { media.art?.asImageBitmap() }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.open_media_controls), onClick = onOpen),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (art != null) {
                Image(
                    art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(MaterialShapes.Cookie9Sided.toShape()),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(media.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    media.artist.ifBlank { media.appLabel },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledIconButton(onClick = actions::mediaPlayPause, shapes = IconButtonDefaults.shapes()) {
                Icon(if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
            }
        }
    }
}

@Composable
private fun NotificationSummary(state: CoverUiState, onOpen: () -> Unit) {
    val apps = state.notifications.distinctBy { it.packageName }.take(5)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            apps.forEach { n ->
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    IconImage(
                        n.smallIcon,
                        size = 16.dp,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                        fallback = n.appIcon,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.notification_count, state.notifications.size, state.notifications.size),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp,
            )
        }
    }
}
