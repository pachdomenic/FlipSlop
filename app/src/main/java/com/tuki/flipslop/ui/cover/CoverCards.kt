package com.tuki.flipslop.ui.cover

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuki.flipslop.R
import com.tuki.flipslop.notifications.LockLine

import com.tuki.flipslop.ui.components.IconImage
import com.tuki.flipslop.ui.cover.pages.Digits
import com.tuki.flipslop.ui.cover.pages.MediaTimes
import com.tuki.flipslop.ui.theme.ClockType
import java.util.Date
import kotlinx.coroutines.delay

/**
 * The card layout shared by the lock screen and the Home page in cards mode:
 * date, clock, notification cards (up to three, one while music plays) and
 * the player card. The clock takes whatever height is left and goes to one
 * line when short.
 */
@Composable
internal fun CardsLayout(
    state: CoverUiState,
    actions: CoverActions,
    lines: List<LockLine>,
    modifier: Modifier = Modifier,
    moreCount: Int = 0,
    onMore: (() -> Unit)? = null,
) {
    // The cover is short: with the player out, one notification card leaves the clock room.
    val room = if (state.media != null) 1 else MAX_CARD_NOTIFICATIONS
    val shown = lines.take(room)
    val more = moreCount + (lines.size - shown.size)
    Column(modifier) {
        CardsDate(state)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CardsClock(state, stacked = maxHeight >= 200.dp, available = maxHeight.value)
        }
        shown.forEach { line ->
            Spacer(Modifier.height(6.dp))
            CompactNotificationCard(line, onClick = { actions.openNotification(line.item) })
        }
        if (more > 0 && onMore != null) {
            TextButton(onClick = onMore, modifier = Modifier.align(Alignment.End)) {
                Text(pluralStringResource(R.plurals.more_notifications, more, more))
            }
        }
        if (state.media != null) {
            Spacer(Modifier.height(8.dp))
            MediaCard(state, actions)
        }
    }
}

@Composable
internal fun CardsDate(state: CoverUiState) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(state.now / 60_000, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
        DateFormat.format(pattern, Date(state.now)).toString()
    }
    Text(
        date.replaceFirstChar { it.titlecase(locale) },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
    )
}

/** Stacked AOSP-style digits when there is room, one line when music takes it. */
@Composable
internal fun CardsClock(state: CoverUiState, stacked: Boolean, available: Float) {
    val time = Date(state.now)
    val hours = DateFormat.format(if (state.is24h) "HH" else "hh", time).toString()
    val minutes = DateFormat.format("mm", time).toString()
    val accent = MaterialTheme.colorScheme.primaryFixed
    if (stacked) {
        val scale = (available / 250f).coerceIn(0.5f, 1f)
        val style = ClockType.idle.copy(
            fontSize = ClockType.idle.fontSize * scale,
            lineHeight = ClockType.idle.lineHeight * scale,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Digits(hours, style, accent)
            Digits(minutes, style, accent)
        }
    } else {
        val size = (available * 0.7f).coerceIn(40f, 96f)
        val style = ClockType.idle.copy(fontSize = size.sp, lineHeight = size.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Digits(hours, style, accent)
            Text(":", style = style, color = accent)
            Digits(minutes, style, accent)
        }
    }
}

/** Compact player: art, title, progress with times, transport controls. */
@Composable
internal fun MediaCard(state: CoverUiState, actions: CoverActions) {
    val media = state.media ?: return
    val art = remember(media.art) { media.art?.asImageBitmap() }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(media.isPlaying) {
        while (media.isPlaying) {
            now = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val position = media.positionAt(now)
    val fraction = if (media.durationMs > 0) position.toFloat() / media.durationMs else 0f

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClickLabel = stringResource(R.string.open_player), onClick = actions::mediaOpen),
                    contentAlignment = Alignment.Center,
                ) {
                    if (art != null) {
                        Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp))
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(12.dp))
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
            }
            if (media.durationMs > 0) {
                Spacer(Modifier.height(8.dp))
                LinearWavyProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    amplitude = { if (media.isPlaying) 1f else 0f },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                MediaTimes(position, media.durationMs, Modifier.fillMaxWidth().padding(top = 2.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = actions::mediaPrevious,
                    enabled = media.canSkipPrevious,
                    shapes = IconButtonDefaults.shapes(),
                ) { Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.previous)) }
                FilledIconButton(onClick = actions::mediaPlayPause, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (media.isPlaying) R.string.pause else R.string.play),
                    )
                }
                FilledTonalIconButton(
                    onClick = actions::mediaNext,
                    enabled = media.canSkipNext,
                    shapes = IconButtonDefaults.shapes(),
                ) { Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next)) }
            }
        }
    }
}

/** One notification as a compact card: app icon, title, one line of text. */
@Composable
internal fun CompactNotificationCard(line: LockLine, onClick: () -> Unit) {
    val item = line.item
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                IconImage(
                    item.smallIcon,
                    size = 16.dp,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                    fallback = item.appIcon,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(line.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (line.text.isNotBlank()) {
                    Text(
                        line.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

internal const val MAX_CARD_NOTIFICATIONS = 3
