package com.tuki.flipslop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
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
    rows: List<@Composable (shapes: ListItemShapes) -> Unit>,
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
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, row ->
                row(ListItemDefaults.segmentedShapes(index, rows.size))
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
