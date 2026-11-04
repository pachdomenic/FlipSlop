package com.tuki.flipslop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuki.flipslop.R
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.service.CoverRuntime
import com.tuki.flipslop.ui.components.ShapedAppIcon
import com.tuki.flipslop.ui.components.rememberIconShape
import com.tuki.flipslop.ui.components.widgetPickerItems
import com.tuki.flipslop.ui.cover.icon
import com.tuki.flipslop.ui.cover.labelRes
import com.tuki.flipslop.ui.cover.resolve
import kotlinx.coroutines.launch

@Composable
fun PagesSettings(runtime: CoverRuntime, onBack: () -> Unit) {
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val enabled = settings.pages
    // Enabled pages in their order, then the disabled ones.
    val ordered = enabled + CoverPage.entries.filter { it !in enabled }

    SettingsScaffold(title = stringResource(R.string.pref_pages), onBack = onBack) {
        item(key = "hint") {
            Text(
                stringResource(R.string.pages_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        item(key = "pages") {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ordered.forEachIndexed { index, page ->
                    val on = page in enabled
                    val position = enabled.indexOf(page)
                    ListItem(
                        checked = on,
                        onCheckedChange = { checked ->
                            if (page == CoverPage.HOME) return@ListItem
                            val next = if (checked) enabled + page else enabled - page
                            scope.launch { runtime.prefs.setPages(next) }
                        },
                        shapes = ListItemDefaults.segmentedShapes(index, ordered.size),
                        leadingContent = { Icon(page.icon, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (on) {
                                    IconButton(
                                        onClick = { scope.launch { runtime.prefs.setPages(enabled.move(position, -1)) } },
                                        enabled = position > 0,
                                    ) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up)) }
                                    IconButton(
                                        onClick = { scope.launch { runtime.prefs.setPages(enabled.move(position, 1)) } },
                                        enabled = position < enabled.lastIndex,
                                    ) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down)) }
                                }
                                Switch(checked = on, onCheckedChange = null, enabled = page != CoverPage.HOME)
                            }
                        },
                    ) { Text(stringResource(page.labelRes)) }
                }
            }
        }
    }
}

private fun <T> List<T>.move(from: Int, delta: Int): List<T> {
    val to = (from + delta).coerceIn(0, lastIndex)
    if (from !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

@Composable
fun AppsSettings(runtime: CoverRuntime, onBack: () -> Unit) {
    val all by runtime.apps.all.collectAsStateWithLifecycle()
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val systemThemed by runtime.iconTheme.themed.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val pinned = settings.pinnedApps.toSet()
    val filtered = remember(all, query) {
        if (query.isBlank()) all else all.filter { it.label.contains(query, ignoreCase = true) }
    }

    SettingsScaffold(
        title = stringResource(R.string.pref_apps),
        subtitle = pluralStringResource(R.plurals.apps_selected, pinned.size, pinned.size),
        onBack = onBack,
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_apps)) },
            )
        }
        items(filtered, key = { it.key }) { app ->
            val index = filtered.indexOf(app)
            ListItem(
                checked = app.key in pinned,
                onCheckedChange = { runtime.apps.togglePin(app) },
                shapes = ListItemDefaults.segmentedShapes(index, filtered.size),
                modifier = Modifier.padding(bottom = ListItemDefaults.SegmentedGap),
                leadingContent = { ShapedAppIcon(
                        app.icon,
                        rememberIconShape(index, settings.iconShape),
                        size = 40.dp,
                        themed = settings.themedIcons.resolve(systemThemed),
                    ) },
                trailingContent = { Checkbox(checked = app.key in pinned, onCheckedChange = null) },
            ) { Text(app.label) }
        }
    }
}

@Composable
fun WidgetsSettings(runtime: CoverRuntime, host: SettingsHost, onBack: () -> Unit) {
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var picking by rememberSaveable { mutableStateOf(false) }
    val providers = remember(picking) { if (picking) runtime.widgets.providers() else emptyList() }

    SettingsScaffold(
        title = stringResource(if (picking) R.string.widgets_pick else R.string.pref_widgets),
        onBack = if (picking) ({ picking = false }) else onBack,
    ) {
        if (!picking) {
            item(key = "added") {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    val ids = settings.widgetIds
                    ids.forEachIndexed { index, id ->
                        val info = runtime.widgets.info(id)
                        ListItem(
                            onClick = {},
                            shapes = ListItemDefaults.segmentedShapes(index, ids.size + 1),
                            supportingContent = { info?.let { Text(it.provider.packageName) } },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { scope.launch { runtime.widgets.move(id, -1) } }, enabled = index > 0) {
                                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                                    }
                                    IconButton(onClick = { scope.launch { runtime.widgets.move(id, 1) } }, enabled = index < ids.lastIndex) {
                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                                    }
                                    IconButton(onClick = { scope.launch { runtime.widgets.remove(id) } }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove))
                                    }
                                }
                            },
                        ) { Text(info?.loadLabel(context.packageManager) ?: "#$id") }
                    }
                    ListItem(
                        onClick = { picking = true },
                        shapes = ListItemDefaults.segmentedShapes(ids.size, ids.size + 1),
                        leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    ) { Text(stringResource(R.string.widgets_add)) }
                }
            }
            item(key = "hint") {
                Text(
                    stringResource(R.string.widgets_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            widgetPickerItems(providers) { info ->
                picking = false
                host.addWidget(info)
            }
        }
    }
}
