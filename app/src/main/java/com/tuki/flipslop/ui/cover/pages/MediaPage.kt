package com.tuki.flipslop.ui.cover.pages

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.components.MorphShape
import com.tuki.flipslop.ui.cover.CoverActions
import com.tuki.flipslop.ui.cover.CoverUiState
import kotlinx.coroutines.delay

@Composable
fun MediaPage(state: CoverUiState, actions: CoverActions, modifier: Modifier = Modifier) {
    val media = state.media
    if (media == null) {
        EmptyState(
            icon = Icons.Rounded.MusicNote,
            text = stringResource(
                if (state.notificationAccess) R.string.media_empty else R.string.media_need_access,
            ),
            action = if (state.notificationAccess) null else stringResource(R.string.grant),
            onAction = actions::grantNotificationAccess,
            modifier = modifier,
        )
        return
    }

    // Square when paused, a cookie when playing.
    val morph = remember { Morph(MaterialShapes.Square, MaterialShapes.Cookie9Sided) }
    val progress by animateFloatAsState(
        targetValue = if (media.isPlaying) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "artMorph",
    )
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

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight) * 0.92f
            Box(
                Modifier
                    .size(side)
                    .clip(MorphShape(morph, progress))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = actions::mediaOpen),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(side))
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(side / 3))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            media.title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            media.artist.ifBlank { media.appLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        LinearWavyProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            amplitude = { if (media.isPlaying) 1f else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(14.dp)
                .pointerInput(media.durationMs, media.canSeek) {
                    if (media.canSeek && media.durationMs > 0) {
                        detectTapGestures { offset ->
                            actions.mediaSeek((offset.x / size.width * media.durationMs).toLong())
                        }
                    }
                },
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalIconButton(
                onClick = actions::mediaPrevious,
                enabled = media.canSkipPrevious,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
            ) { Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.previous)) }
            FilledIconButton(
                onClick = actions::mediaPlayPause,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(IconButtonDefaults.largeContainerSize()),
            ) {
                Icon(
                    if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (media.isPlaying) R.string.pause else R.string.play),
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }
            FilledTonalIconButton(
                onClick = actions::mediaNext,
                enabled = media.canSkipNext,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
            ) { Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next)) }
        }
    }
}
