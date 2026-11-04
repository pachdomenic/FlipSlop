package com.tuki.flipslop.ui.components

import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.graphics.shapes.RoundedPolygon
import com.tuki.flipslop.data.IconShapeStyle

/** Expressive shapes the cover cycles through for app icons. */
val IconShapes: List<RoundedPolygon> = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Cookie7Sided,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Ghostish,
)

/**
 * The mask for the [index]-th icon in a grid. Expressive cycles through
 * [IconShapes]; the others give every icon the same shape.
 */
@Composable
fun rememberIconShape(index: Int, style: IconShapeStyle = IconShapeStyle.EXPRESSIVE): Shape = when (style) {
    IconShapeStyle.EXPRESSIVE -> IconShapes[Math.floorMod(index, IconShapes.size)].toShape()
    IconShapeStyle.CIRCLE -> CircleShape
    IconShapeStyle.SQUIRCLE -> SquircleShape
    IconShapeStyle.SYSTEM -> SystemIconShape
}

private val SquircleShape = RoundedCornerShape(percent = 30)

/**
 * The icon mask the system uses (config_icon_mask, or the shape picked in
 * Wallpaper & style through its overlay), read with the public
 * AdaptiveIconDrawable.getIconMask() on a 100x100 icon and scaled to size.
 */
object SystemIconShape : Shape {
    private const val UNIT = 100

    private val mask: android.graphics.Path? by lazy {
        runCatching {
            AdaptiveIconDrawable(null, null).apply { setBounds(0, 0, UNIT, UNIT) }.iconMask
        }.getOrNull()
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val m = mask ?: return CircleShape.createOutline(size, layoutDirection, density)
        val scaled = android.graphics.Path(m).apply {
            transform(android.graphics.Matrix().apply { setScale(size.width / UNIT, size.height / UNIT) })
        }
        return Outline.Generic(scaled.asComposePath())
    }
}

/**
 * Renders a launcher icon inside an expressive shape. Adaptive icons are drawn
 * edge to edge (background + foreground, no system mask) so the shape is the
 * mask; legacy icons sit centered on a tonal container.
 */
@Composable
fun ShapedAppIcon(
    drawable: Drawable,
    shape: Shape,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val adaptive = drawable is AdaptiveIconDrawable
    val bitmap = remember(drawable, px) { renderIcon(drawable, px) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(if (adaptive) size else size * 0.72f),
        )
    }
}

private fun renderIcon(drawable: Drawable, px: Int): ImageBitmap {
    val bmp = createBitmap(px.coerceAtLeast(1), px.coerceAtLeast(1))
    val canvas = Canvas(bmp)
    if (drawable is AdaptiveIconDrawable) {
        // Adaptive layers are 108dp with a 72dp safe zone: overscan by 1/6 per side.
        val inset = px / 6
        val bounds = android.graphics.Rect(-inset, -inset, px + inset, px + inset)
        drawable.background?.let { it.bounds = bounds; it.draw(canvas) }
        drawable.foreground?.let { it.bounds = bounds; it.draw(canvas) }
    } else {
        drawable.setBounds(0, 0, px, px)
        drawable.draw(canvas)
    }
    return bmp.asImageBitmap()
}
