package com.tuki.flipslop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuki.flipslop.BuildConfig
import com.tuki.flipslop.R
import com.tuki.flipslop.core.CapabilityState
import com.tuki.flipslop.core.SystemConfig
import com.tuki.flipslop.core.Tier
import com.tuki.flipslop.data.ClockStyle
import com.tuki.flipslop.data.CoverPrefs
import com.tuki.flipslop.fold.FoldSnapshot
import com.tuki.flipslop.fold.FoldSource
import com.tuki.flipslop.fold.FoldState
import com.tuki.flipslop.service.CoverRuntime
import com.tuki.flipslop.ui.cover.CoverScreen
import com.tuki.flipslop.ui.cover.HostedWidget
import com.tuki.flipslop.ui.cover.NoopCoverActions
import com.tuki.flipslop.ui.cover.rememberCoverUiState
import kotlinx.coroutines.launch

@Composable
fun MainSettings(runtime: CoverRuntime, host: SettingsHost, navigate: (Route) -> Unit) {
    val caps by runtime.capabilities.collectAsStateWithLifecycle()
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val fold by runtime.fold.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val prefs = runtime.prefs
    val titleAccess = stringResource(R.string.section_access)
    val titleBehaviour = stringResource(R.string.section_behaviour)
    val titleCover = stringResource(R.string.section_cover)
    val titleDiagnostics = stringResource(R.string.section_diagnostics)

    SettingsScaffold(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(caps.tier.labelRes),
    ) {
        item(key = "hero") { Hero(runtime, caps, fold, host) }

        segmentedGroup(
            title = titleAccess,
            key = "access",
            rows = permissionRows(caps, host),
        )

        segmentedGroup(
            title = titleBehaviour,
            key = "behaviour",
            rows = listOf(
                switchRow(Icons.Rounded.PhonelinkSetup, R.string.pref_auto_show, R.string.pref_auto_show_desc, settings.autoShowOnFold) {
                    scope.launch { prefs.setAutoShowOnFold(it) }
                },
                switchRow(Icons.Rounded.NotificationsActive, R.string.pref_show_on_wake, R.string.pref_show_on_wake_desc, settings.showOnWake) {
                    scope.launch { prefs.setShowOnWake(it) }
                },
                switchRow(
                    Icons.Rounded.Home,
                    R.string.pref_return_home,
                    if (caps.usageAccess) R.string.pref_return_home_desc else R.string.pref_return_home_needs_usage,
                    settings.returnAfterHome,
                    enabled = caps.usageAccess,
                ) { scope.launch { prefs.setReturnAfterHome(it) } },
            ),
        )

        segmentedGroup(
            title = titleCover,
            key = "cover",
            rows = listOf(
                navRow(Icons.Rounded.ViewCarousel, R.string.pref_pages, R.string.pref_pages_desc) { navigate(Route.PAGES) },
                navRow(Icons.Rounded.Apps, R.string.pref_apps, R.string.pref_apps_desc) { navigate(Route.APPS) },
                navRow(Icons.Rounded.Widgets, R.string.pref_widgets, R.string.pref_widgets_desc) { navigate(Route.WIDGETS) },
                { shapes -> ClockStyleRow(settings.clockStyle, shapes) { s -> scope.launch { prefs.setClockStyle(s) } } },
                { shapes -> InsetRow(settings.bottomInsetDp, shapes) { v -> scope.launch { prefs.setBottomInset(v) } } },
            ),
        )

        segmentedGroup(
            title = titleDiagnostics,
            key = "diagnostics",
            rows = diagnosticsRows(runtime, fold, caps),
        )

        segmentedGroup(
            title = null,
            key = "about",
            rows = listOf(
                { shapes ->
                    ListItem(
                        onClick = {},
                        shapes = shapes,
                        supportingContent = { Text(stringResource(R.string.about_text)) },
                    ) { Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME)) }
                },
            ),
        )
    }
}

@Composable
private fun Hero(runtime: CoverRuntime, caps: CapabilityState, fold: FoldSnapshot, host: SettingsHost) {
    val state = rememberCoverUiState(runtime, locked = false, edgePaddingDp = 16)
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverPreviewFrame {
            CoverScreen(
                state = state,
                actions = NoopCoverActions,
                widget = { id, modifier -> HostedWidget(runtime, id, modifier) },
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TierBadge(caps.tier)
            Text(
                stringResource(fold.state.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = host::openCoverPreview, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.open_cover_now))
        }
        if (!caps.canAutoShow) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.hint_cannot_auto_show),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun TierBadge(tier: Tier) {
    val shape = when (tier) {
        Tier.BASIC -> MaterialShapes.Circle
        Tier.GRANTED -> MaterialShapes.Cookie6Sided
        Tier.PRIVILEGED -> MaterialShapes.Sunny
    }.toShape()
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = shape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) {}
            Spacer(Modifier.size(8.dp))
            Text(stringResource(tier.labelRes), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private data class Perm(
    val icon: ImageVector,
    val title: Int,
    val desc: Int,
    val granted: Boolean,
    val onGrant: () -> Unit,
)

private fun permissionRows(caps: CapabilityState, host: SettingsHost): List<@Composable (ListItemShapes) -> Unit> {
    val priv = caps.tier == Tier.PRIVILEGED
    val perms = listOf(
        Perm(Icons.Rounded.Notifications, R.string.perm_post, R.string.perm_post_desc, caps.postNotifications, host::grantPostNotifications),
        Perm(Icons.Rounded.Layers, R.string.perm_overlay, R.string.perm_overlay_desc, caps.overlay || priv, host::grantOverlay),
        Perm(Icons.Rounded.NotificationsActive, R.string.perm_listener, R.string.perm_listener_desc, caps.notificationAccess, host::grantNotificationAccess),
        Perm(Icons.Rounded.Fullscreen, R.string.perm_fsi, R.string.perm_fsi_desc, caps.fullScreenIntent || priv, host::grantFullScreenIntent),
        Perm(Icons.Rounded.QueryStats, R.string.perm_usage, R.string.perm_usage_desc, caps.usageAccess, host::grantUsageAccess),
        Perm(Icons.Rounded.BrightnessMedium, R.string.perm_write_settings, R.string.perm_write_settings_desc, caps.writeSettings, host::grantWriteSettings),
    )
    return perms.map { p ->
        { shapes: ListItemShapes ->
            ListItem(
                onClick = p.onGrant,
                shapes = shapes,
                leadingContent = { Icon(p.icon, contentDescription = null) },
                supportingContent = { Text(stringResource(p.desc)) },
                trailingContent = {
                    if (p.granted) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.granted), tint = MaterialTheme.colorScheme.primary)
                    } else {
                        FilledTonalButton(onClick = p.onGrant, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.grant)) }
                    }
                },
            ) { Text(stringResource(p.title)) }
        }
    }
}

private fun switchRow(
    icon: ImageVector,
    title: Int,
    desc: Int,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
): @Composable (ListItemShapes) -> Unit = { shapes ->
    ListItem(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(stringResource(desc)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    ) { Text(stringResource(title)) }
}

private fun navRow(icon: ImageVector, title: Int, desc: Int, onClick: () -> Unit): @Composable (ListItemShapes) -> Unit = { shapes ->
    ListItem(
        onClick = onClick,
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(stringResource(desc)) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
    ) { Text(stringResource(title)) }
}

@Composable
private fun ClockStyleRow(style: ClockStyle, shapes: ListItemShapes, onChange: (ClockStyle) -> Unit) {
    ListItem(
        onClick = {},
        shapes = shapes,
        leadingContent = { Icon(Icons.Rounded.AccessTime, contentDescription = null) },
        supportingContent = {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ToggleButton(
                    checked = style == ClockStyle.STACKED,
                    onCheckedChange = { onChange(ClockStyle.STACKED) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.clock_stacked)) }
                ToggleButton(
                    checked = style == ClockStyle.INLINE,
                    onCheckedChange = { onChange(ClockStyle.INLINE) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.clock_inline)) }
            }
        },
    ) { Text(stringResource(R.string.pref_clock)) }
}

@Composable
private fun InsetRow(value: Int, shapes: ListItemShapes, onChange: (Int) -> Unit) {
    val slider = rememberSliderState(value = value.toFloat(), trackRange = 0f..CoverPrefs.MAX_INSET_DP.toFloat())
    ListItem(
        onClick = {},
        shapes = shapes,
        supportingContent = {
            Column {
                Text(stringResource(R.string.pref_inset_desc))
                Slider(
                    state = slider,
                    onValueChange = { slider.value = it },
                    onValueChangeFinished = { onChange(slider.value.toInt()) },
                )
            }
        },
    ) { Text(stringResource(R.string.pref_inset, slider.value.toInt())) }
}

private fun diagnosticsRows(
    runtime: CoverRuntime,
    fold: FoldSnapshot,
    caps: CapabilityState,
): List<@Composable (ListItemShapes) -> Unit> {
    fun row(title: Int, value: String): @Composable (ListItemShapes) -> Unit = { shapes ->
        ListItem(onClick = {}, shapes = shapes, supportingContent = { Text(value) }) { Text(stringResource(title)) }
    }
    val states = SystemConfig.foldedDeviceStates.sorted().joinToString(",").ifEmpty { "—" }
    return listOf(
        { shapes ->
            ListItem(onClick = {}, shapes = shapes, supportingContent = { Text(stringResource(fold.source.labelRes)) }) {
                Text(stringResource(fold.state.labelRes))
            }
        },
        row(R.string.diag_panel, fold.panel?.toString() ?: "—"),
        row(R.string.diag_hinge, fold.hingeAngle?.let { "%.0f°".format(it) } ?: "—"),
        row(R.string.diag_device_state, buildString {
            append(fold.deviceState?.toString() ?: "—")
            append(" · ")
            append(if (caps.deviceStateApi) "API" else "no API")
            append(" · folded=")
            append(states)
        }),
        { shapes ->
            val scope = rememberCoroutineScope()
            val known by runtime.prefs.knownPanels.collectAsStateWithLifecycle(emptySet())
            ListItem(
                onClick = { scope.launch { runtime.prefs.forgetPanels() } },
                shapes = shapes,
                supportingContent = { Text(known.joinToString().ifEmpty { "—" }) },
                trailingContent = { Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.primary) },
            ) { Text(stringResource(R.string.diag_known_panels)) }
        },
    )
}

val Tier.labelRes: Int
    get() = when (this) {
        Tier.BASIC -> R.string.tier_basic
        Tier.GRANTED -> R.string.tier_granted
        Tier.PRIVILEGED -> R.string.tier_privileged
    }

val FoldState.labelRes: Int
    get() = when (this) {
        FoldState.FOLDED -> R.string.fold_folded
        FoldState.HALF_OPEN -> R.string.fold_half
        FoldState.OPEN -> R.string.fold_open
        FoldState.UNKNOWN -> R.string.fold_unknown
    }

val FoldSource.labelRes: Int
    get() = when (this) {
        FoldSource.DEVICE_STATE -> R.string.source_device_state
        FoldSource.DISPLAY -> R.string.source_display
        FoldSource.NONE -> R.string.source_none
    }

