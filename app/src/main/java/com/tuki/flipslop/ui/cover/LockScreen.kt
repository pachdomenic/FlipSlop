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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.cover.pages.Digits
import com.tuki.flipslop.ui.cover.pages.MediaTimes
import com.tuki.flipslop.ui.theme.ClockType
import java.util.Date
import kotlinx.coroutines.delay

/**
 * The cover while the device is locked: the clock and the music, nothing
 * else. A tap on any empty spot or a swipe up brings up the bouncer; once
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
        Column(contentModifier) {
            LockDate(state)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LockClock(state, stacked = maxHeight >= 200.dp, available = maxHeight.value)
            }
            if (state.media != null) {
                Spacer(Modifier.height(8.dp))
                LockMedia(state, actions)
            }
        }
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

@Composable
private fun LockDate(state: CoverUiState) {
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
private fun LockClock(state: CoverUiState, stacked: Boolean, available: Float) {
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
private fun LockMedia(state: CoverUiState, actions: CoverActions) {
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
