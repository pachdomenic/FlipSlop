package com.tuki.flipslop.ui.components

import android.appwidget.AppWidgetProviderInfo
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.min

/**
 * Widget picker rows, grouped by app, each with the widget's own preview.
 * Shared by the settings screen and the picker on the cover.
 */
fun LazyListScope.widgetPickerItems(
    providers: List<AppWidgetProviderInfo>,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val groups = providers.groupBy { it.provider.packageName }
    groups.forEach { (pkg, infos) ->
        item(key = "header-$pkg") { AppHeader(infos.first()) }
        items(infos, key = { it.provider.flattenToString() }) { info ->
            WidgetCard(info, onPick = { onPick(info) })
        }
    }
}

@Composable
private fun AppHeader(info: AppWidgetProviderInfo) {
    val context = LocalContext.current
    val label = remember(info) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
        }.getOrDefault(info.provider.packageName)
    }
    val density = LocalResources.current.displayMetrics.densityDpi
    val icon = remember(info) { runCatching { info.loadIcon(context, density)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull() }
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun WidgetCard(info: AppWidgetProviderInfo, onPick: () -> Unit) {
    val context = LocalContext.current
    val dm = LocalResources.current.displayMetrics
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Box {
            Column(Modifier.padding(12.dp)) {
                WidgetPreview(
                    info,
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    info.loadLabel(context.packageManager),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${(info.minWidth / dm.density).toInt()} × ${(info.minHeight / dm.density).toInt()} dp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // The preview may be a live view that eats touches; the whole card picks.
            Box(Modifier.matchParentSize().clickable(onClick = onPick))
        }
    }
}

/**
 * The widget's own preview: its preview image, else its preview layout
 * inflated from its RemoteViews (how launchers do it), else its icon.
 */
@Composable
fun WidgetPreview(info: AppWidgetProviderInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalResources.current.displayMetrics.densityDpi
    val image: ImageBitmap? = remember(info) {
        runCatching {
            info.loadPreviewImage(context, density)?.let { d ->
                val w = d.intrinsicWidth.coerceAtLeast(1)
                val h = d.intrinsicHeight.coerceAtLeast(1)
                val scale = min(1f, MAX_PREVIEW_PX / maxOf(w, h).toFloat())
                d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
            }
        }.getOrNull()
    }
    when {
        image != null -> Box(modifier, contentAlignment = Alignment.Center) {
            Image(image, contentDescription = null, contentScale = ContentScale.Fit)
        }
        info.previewLayout != 0 -> AndroidView(
            factory = { c ->
                runCatching {
                    RemoteViews(info.provider.packageName, info.previewLayout).apply(c, FrameLayout(c))
                }.getOrElse { View(c) }
            },
            modifier = modifier,
        )
        else -> {
            val icon = remember(info) {
                runCatching { info.loadIcon(context, density)?.toBitmap(192, 192)?.asImageBitmap() }.getOrNull()
            }
            Box(modifier.height(72.dp), contentAlignment = Alignment.Center) {
                icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(56.dp)) }
            }
        }
    }
}

private const val MAX_PREVIEW_PX = 720f
