package com.tuki.flipslop.ui.cover

import android.graphics.drawable.Icon
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tuki.flipslop.R
import com.tuki.flipslop.notifications.CoverNotification
import java.util.Date

/**
 * Everything an expanded notification has to say: the whole text, the
 * conversation, the inbox lines, the picture, the progress and the sub text.
 */
@Composable
internal fun NotificationDetails(item: CoverNotification, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            item.messages.isNotEmpty() -> {
                val context = LocalContext.current
                val you = stringResource(R.string.notification_you)
                item.messages.forEach { m ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                m.sender ?: you,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (m.sender == null) muted else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (m.time > 0) {
                                Text(
                                    "  " + DateFormat.getTimeFormat(context).format(Date(m.time)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = muted,
                                )
                            }
                        }
                        Text(m.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item.lines.isNotEmpty() -> item.lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium, color = muted)
            }
            else -> {
                val text = item.bigText ?: item.text
                if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, color = muted)
            }
        }
        item.picture?.let { NotificationPicture(it) }
        item.progress?.let { p ->
            if (p.indeterminate) {
                LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
            } else {
                LinearWavyProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
        }
        item.subText?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = muted) }
    }
}

@Composable
private fun NotificationPicture(icon: Icon) {
    val context = LocalContext.current
    val bitmap: ImageBitmap = remember(icon) {
        runCatching<ImageBitmap?> {
            val d = icon.loadDrawable(context) ?: return@runCatching null
            // Bounded: pictures can be camera-sized; the cover needs a few hundred px.
            val w = d.intrinsicWidth.takeIf { it > 0 } ?: PICTURE_PX
            val h = d.intrinsicHeight.takeIf { it > 0 } ?: PICTURE_PX
            val scale = minOf(1f, PICTURE_PX.toFloat() / maxOf(w, h))
            d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
        }.getOrNull()
    } ?: return
    Image(
        bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .clip(RoundedCornerShape(16.dp)),
    )
}

/** The expand chevron, as on the system's notifications: points down, flips up when open. */
@Composable
internal fun ExpandButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "expand",
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.size(28.dp),
    ) {
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.notification_collapse else R.string.notification_expand),
            modifier = Modifier.padding(4.dp).rotate(rotation),
        )
    }
}

private const val PICTURE_PX = 720
