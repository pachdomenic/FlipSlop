package com.tuki.flipslop.ui.components

import android.graphics.drawable.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

/** Draws a platform [Icon] (notification icons and friends). */
@Composable
fun IconImage(
    icon: Icon?,
    size: Dp,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val bitmap: ImageBitmap? = remember(icon, px) {
        icon?.let { runCatching { it.loadDrawable(context)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull() }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size), colorFilter = colorFilter)
    }
}
