package com.tuki.flipslop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** A titled group of segmented (connected) list items, Android 16 settings style. */
fun LazyListScope.segmentedGroup(
    title: String?,
    key: String,
    rows: List<@Composable (shapes: Shape) -> Unit>,
) {
    if (title != null) {
        item(key = "$key-title") {
            Text(
                title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
            )
        }
    }
    item(key = key) {
        Column(verticalArrangement = Arrangement.spacedBy(SegmentedGap)) {
            rows.forEachIndexed { index, row ->
                row(segmentedShape(index, rows.size))
            }
        }
    }
}

/** Renders a 480x565dp cover layout scaled down to fit the available width. */
@Composable
fun CoverPreviewFrame(
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.62f,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val coverW = 480.dp
        val coverH = 565.dp
        val scale = (maxWidth * widthFraction) / coverW
        Box(
            Modifier
                .size(coverW * scale, coverH * scale)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .requiredSize(coverW, coverH)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) { content() }
        }
    }
}

/** Gap between the rows of a segmented group. */
val SegmentedGap = 2.dp

private val OuterCorner = 24.dp
private val InnerCorner = 4.dp

/**
 * Android 16 settings style segmented rows: the group is rounded on the
 * outside, the rows meet with small corners in between.
 */
fun segmentedShape(index: Int, count: Int): Shape {
    val top = if (index == 0) OuterCorner else InnerCorner
    val bottom = if (index == count - 1) OuterCorner else InnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** A clickable settings row in a segmented group. */
@Composable
fun SettingsItem(
    onClick: () -> Unit,
    shapes: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shapes,
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = modifier.fillMaxWidth(),
    ) {
        RowContent(enabled, leadingContent, trailingContent, supportingContent, content)
    }
}

/** A settings row that toggles; tinted while checked. */
@Composable
fun SettingsItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shape = shapes,
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceBright,
        modifier = modifier.fillMaxWidth(),
    ) {
        RowContent(enabled, leadingContent, trailingContent, supportingContent, content)
    }
}

@Composable
private fun RowContent(
    enabled: Boolean,
    leadingContent: (@Composable () -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
    supportingContent: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    ListItem(
        headlineContent = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
    )
}

private const val DISABLED_ALPHA = 0.38f
