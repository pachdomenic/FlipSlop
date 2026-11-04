package com.tuki.flipslop.ui.components

import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
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
import androidx.compose.ui.graphics.Color
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
 * Renders a launcher icon inside [shape]. Adaptive icons are drawn edge to
 * edge (background + foreground, no system mask) so the shape is the mask;
 * legacy icons sit centered on a tonal container.
 *
 * [themed] draws a Material You themed icon instead: the icon's monochrome
 * layer (or, when it has none, the silhouette of its foreground) tinted
 * with the colors the launcher uses for its themed icons (see ThemedIconColors).
 */
@Composable
fun ShapedAppIcon(
    drawable: Drawable,
    shape: Shape,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    themed: Boolean = false,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val colors = MaterialTheme.colorScheme
    // Same colors as the home screen's themed icons, not our own palette roles.
    val launcher = if (themed) rememberThemedIconColors() else null
    val tint = launcher?.foreground
    val bitmap = remember(drawable, px, tint) { renderIcon(drawable, px, tint) }
    val fullBleed = themed || drawable is AdaptiveIconDrawable
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(launcher?.let { Color(it.background) } ?: colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(if (fullBleed) size else size * 0.72f),
        )
    }
}

private fun renderIcon(drawable: Drawable, px: Int, themedTint: Int?): ImageBitmap {
    val side = px.coerceAtLeast(1)
    val bmp = createBitmap(side, side)
    val canvas = Canvas(bmp)
    // Adaptive layers are 108dp with a 72dp safe zone: overscan by 1/6 per side.
    val inset = side / 6
    val overscan = Rect(-inset, -inset, side + inset, side + inset)
    if (themedTint != null) {
        val adaptive = drawable as? AdaptiveIconDrawable
        val layer = adaptive?.monochrome ?: adaptive?.foreground ?: drawable
        // Draw a copy: launcher drawables are shared and a color filter would stick.
        val copy = layer.constantState?.newDrawable()?.mutate() ?: layer
        copy.bounds = if (adaptive != null) overscan else Rect(side / 5, side / 5, side - side / 5, side - side / 5)
        copy.colorFilter = PorterDuffColorFilter(themedTint, PorterDuff.Mode.SRC_IN)
        copy.draw(canvas)
    } else if (drawable is AdaptiveIconDrawable) {
        drawable.background?.let { it.bounds = overscan; it.draw(canvas) }
        drawable.foreground?.let { it.bounds = overscan; it.draw(canvas) }
    } else {
        drawable.setBounds(0, 0, side, side)
        drawable.draw(canvas)
    }
    return bmp.asImageBitmap()
}
