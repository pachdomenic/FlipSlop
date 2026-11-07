package com.tuki.flipslop.ui.settings

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.rounded.Fingerprint
import com.tuki.flipslop.unlock.SystemFingerprint
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.data.FingerprintUnlock
import com.tuki.flipslop.unlock.TrustStatus
import com.tuki.flipslop.unlock.TrustUnlock
import com.tuki.flipslop.data.LockConfig
import com.tuki.flipslop.data.LockHintPosition
import com.tuki.flipslop.data.LockLayout
import com.tuki.flipslop.data.LockNotifications
import com.tuki.flipslop.data.LockTabCandidates
import com.tuki.flipslop.data.UnlockGesture
import com.tuki.flipslop.ui.cover.icon
import com.tuki.flipslop.ui.cover.labelRes
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewAgenda
import com.tuki.flipslop.data.ClockAlign
import com.tuki.flipslop.data.ClockColor
import com.tuki.flipslop.data.ClockConfig
import com.tuki.flipslop.data.ClockFont
import com.tuki.flipslop.data.DatePosition
import com.tuki.flipslop.data.DateStyle
import com.tuki.flipslop.data.HourFormat
import com.tuki.flipslop.ui.cover.CoverUiState
import com.tuki.flipslop.ui.cover.fontFamily
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuki.flipslop.R
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.core.CapabilityState
import com.tuki.flipslop.core.SystemConfig
import com.tuki.flipslop.core.Tier
import com.tuki.flipslop.data.ClockStyle
import com.tuki.flipslop.data.CoverPrefs
import com.tuki.flipslop.data.HomeStyle
import com.tuki.flipslop.data.IconShapeStyle
import com.tuki.flipslop.data.NotificationStyle
import com.tuki.flipslop.data.ThemedIcons
import com.tuki.flipslop.fold.FoldSnapshot
import com.tuki.flipslop.fold.FoldSource
import com.tuki.flipslop.fold.FoldState
import com.tuki.flipslop.service.CoverRuntime
import com.tuki.flipslop.ui.cover.CoverScreen
import com.tuki.flipslop.ui.cover.HostedWidget
import com.tuki.flipslop.ui.cover.NoopCoverActions
import com.tuki.flipslop.ui.cover.rememberCoverUiState
import com.tuki.flipslop.ui.components.rememberIconShape
import kotlinx.coroutines.launch

@Composable
fun MainSettings(runtime: CoverRuntime, host: SettingsHost, navigate: (Route) -> Unit) {
    val caps by runtime.capabilities.collectAsStateWithLifecycle()
    val fold by runtime.fold.snapshot.collectAsStateWithLifecycle()
    val (granted, total) = permissionCount(caps)

    SettingsScaffold(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(caps.tier.labelRes),
    ) {
        item(key = "hero") { Hero(runtime, caps, fold, host) }
        segmentedGroup(
            title = null,
            key = "categories",
            rows = rows(
                categoryRow(Route.ACCESS, Icons.Rounded.Security, summary = { stringResource(R.string.cat_access_desc, granted, total) }, navigate),
                categoryRow(Route.BEHAVIOUR, Icons.Rounded.PhonelinkSetup, R.string.cat_behaviour_desc, navigate),
                categoryRow(Route.LOCK, Icons.Rounded.Lock, R.string.cat_lock_desc, navigate),
                categoryRow(Route.COVER, Icons.Rounded.Smartphone, R.string.cat_cover_desc, navigate),
                categoryRow(Route.STYLE, Icons.Rounded.Palette, R.string.cat_style_desc, navigate),
                categoryRow(Route.CAMERA, Icons.Rounded.PhotoCamera, R.string.cat_camera_desc, navigate),
                categoryRow(Route.DIAGNOSTICS, Icons.Rounded.Info, R.string.cat_diagnostics_desc, navigate),
            ),
        )
    }
}

/** One settings category: its own screen with one group of rows. */
@Composable
fun CategorySettings(
    route: Route,
    runtime: CoverRuntime,
    host: SettingsHost,
    navigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val caps by runtime.capabilities.collectAsStateWithLifecycle()
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val fold by runtime.fold.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val prefs = runtime.prefs

    val content: List<@Composable (Shape) -> Unit> = when (route) {
        Route.ACCESS -> permissionRows(caps, host)

        Route.BEHAVIOUR -> rows(
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
        )

        Route.LOCK -> lockRows(settings.lock, caps, prefs, scope, navigate)

        Route.COVER -> rows(
            navRow(Icons.Rounded.ViewCarousel, R.string.pref_pages, R.string.pref_pages_desc) { navigate(Route.PAGES) },
            navRow(Icons.Rounded.Apps, R.string.pref_apps, R.string.pref_apps_desc) { navigate(Route.APPS) },
            navRow(Icons.Rounded.Widgets, R.string.pref_widgets, R.string.pref_widgets_desc) { navigate(Route.WIDGETS) },
            { shapes ->
                ChoiceRow(
                    title = R.string.pref_home_style,
                    icon = Icons.Rounded.Home,
                    options = HomeStyle.entries,
                    selected = settings.homeStyle,
                    label = { stringResource(it.labelRes) },
                    shapes = shapes,
                    content = { Text(stringResource(it.shortLabelRes), maxLines = 1) },
                ) { s -> scope.launch { prefs.setHomeStyle(s) } }
            },
            { shapes ->
                ChoiceRow(
                    title = R.string.pref_notification_style,
                    icon = Icons.Rounded.Notifications,
                    options = NotificationStyle.entries,
                    selected = settings.notificationStyle,
                    label = {
                        stringResource(
                            if (it == NotificationStyle.COMPACT) R.string.notification_style_compact
                            else R.string.notification_style_detailed,
                        )
                    },
                    shapes = shapes,
                ) { s -> scope.launch { prefs.setNotificationStyle(s) } }
            },
            switchRow(Icons.Rounded.SignalCellularAlt, R.string.pref_status_bar, R.string.pref_status_bar_desc, settings.showStatusBar) {
                scope.launch { prefs.setShowStatusBar(it) }
            },
        )

        Route.STYLE -> rows(
            { shapes ->
                ChoiceRow(
                    title = R.string.pref_icon_shape,
                    icon = Icons.Rounded.Category,
                    options = IconShapeStyle.entries,
                    selected = settings.iconShape,
                    label = { stringResource(it.labelRes) },
                    shapes = shapes,
                    content = { style ->
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(rememberIconShape(0, style))
                                .background(LocalContentColor.current),
                        )
                    },
                ) { s -> scope.launch { prefs.setIconShape(s) } }
            },
            { shapes ->
                val systemThemed by runtime.iconTheme.themed.collectAsStateWithLifecycle()
                ChoiceRow(
                    title = R.string.pref_themed_icons,
                    icon = Icons.Rounded.Palette,
                    options = ThemedIcons.entries,
                    selected = settings.themedIcons,
                    label = { stringResource(it.labelRes) },
                    shapes = shapes,
                    // Three buttons in a row: short labels, the full one sits in the title row.
                    content = { Text(stringResource(if (it == ThemedIcons.SYSTEM) R.string.themed_icons_system_short else it.labelRes), maxLines = 1) },
                    footer = when {
                        settings.themedIcons != ThemedIcons.SYSTEM -> null
                        !caps.systemIconTheme -> stringResource(R.string.themed_icons_system_unavailable)
                        systemThemed -> stringResource(R.string.themed_icons_system_on)
                        else -> stringResource(R.string.themed_icons_system_off)
                    },
                ) { m -> scope.launch { prefs.setThemedIcons(m) } }
            },
            navRow(Icons.Rounded.AccessTime, R.string.pref_clock, R.string.pref_clock_desc) { navigate(Route.CLOCK) },
            switchRow(Icons.Rounded.Album, R.string.pref_media_art, R.string.pref_media_art_desc, settings.mediaArtBackground) {
                scope.launch { prefs.setMediaArtBackground(it) }
            },
            { shapes ->
                SliderRow(
                    value = settings.artBlurDp,
                    max = CoverPrefs.MAX_BLUR_DP,
                    title = R.string.pref_art_blur,
                    desc = R.string.pref_art_blur_desc,
                    shapes = shapes,
                ) { v -> scope.launch { prefs.setArtBlur(v) } }
            },
            { shapes ->
                val context = LocalContext.current
                val blurSupported = remember {
                    context.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled
                }
                SliderRow(
                    value = settings.wallpaperBlurDp,
                    max = CoverPrefs.MAX_BLUR_DP,
                    title = R.string.pref_wallpaper_blur,
                    desc = if (blurSupported) R.string.pref_wallpaper_blur_desc else R.string.pref_wallpaper_blur_unsupported,
                    shapes = shapes,
                ) { v -> scope.launch { prefs.setWallpaperBlur(v) } }
            },
        )

        Route.CAMERA -> rows(
            switchRow(Icons.Rounded.ViewCarousel, R.string.pref_page_bar_always, R.string.pref_page_bar_always_desc, settings.pageBarAlways) {
                scope.launch { prefs.setPageBarAlways(it) }
            },
            switchRow(
                Icons.Rounded.MoreHoriz,
                R.string.pref_page_dots,
                R.string.pref_page_dots_desc,
                settings.showPageDots && !settings.pageBarAlways,
                enabled = !settings.pageBarAlways,
            ) { scope.launch { prefs.setShowPageDots(it) } },
            switchRow(Icons.Rounded.Notifications, R.string.pref_dock_notifications, R.string.pref_dock_notifications_desc, settings.dockNotifications) {
                scope.launch { prefs.setDockNotifications(it) }
            },
            switchRow(Icons.Rounded.MusicNote, R.string.pref_dock_media, R.string.pref_dock_media_desc, settings.dockMedia) {
                scope.launch { prefs.setDockMedia(it) }
            },
            { shapes -> CameraSideRow(settings.cameraSide, shapes) { s -> scope.launch { prefs.setCameraSide(s) } } },
            { shapes ->
                SliderRow(
                    value = settings.cameraWidthDp,
                    max = CoverPrefs.MAX_CAMERA_WIDTH_DP,
                    title = R.string.pref_camera_width,
                    desc = R.string.pref_camera_width_desc,
                    shapes = shapes,
                ) { v -> scope.launch { prefs.setCameraWidth(v) } }
            },
            { shapes ->
                SliderRow(
                    value = settings.bottomInsetDp,
                    max = CoverPrefs.MAX_INSET_DP,
                    title = R.string.pref_inset,
                    desc = R.string.pref_inset_desc,
                    shapes = shapes,
                ) { v -> scope.launch { prefs.setBottomInset(v) } }
            },
        )

        Route.CLOCK -> clockRows(settings.clock, settings.idleAospClock, prefs, scope)

        Route.DIAGNOSTICS -> diagnosticsRows(runtime, fold, caps) + rows(
            { shapes ->
                SettingsItem(
                    onClick = {},
                    shapes = shapes,
                    supportingContent = { Text(stringResource(R.string.about_text)) },
                ) {
                    // From the package, not BuildConfig: the Soong build has no BuildConfig.
                    val context = LocalContext.current
                    val version = remember {
                        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                            .getOrNull().orEmpty()
                    }
                    Text(stringResource(R.string.about_version, version))
                }
            },
        )

        else -> emptyList()
    }

    SettingsScaffold(title = stringResource(route.titleRes), onBack = onBack) {
        // The camera screen shows the cover preview, so the sliders have something to move.
        if (route == Route.CAMERA || route == Route.STYLE) {
            item(key = "preview") { CoverPreview(runtime) }
        }
        if (route == Route.LOCK) {
            item(key = "preview") { CoverPreview(runtime) { it.copy(locked = true) } }
        }
        if (route == Route.CLOCK) {
            // Show the big clock whatever Home is set to, so every change is visible.
            item(key = "preview") { CoverPreview(runtime) { it.copy(homeStyle = HomeStyle.BIG_CLOCK) } }
        }
        segmentedGroup(title = null, key = route.name, rows = content)
    }
}

private fun rows(vararg row: @Composable (Shape) -> Unit): List<@Composable (Shape) -> Unit> = row.toList()

private fun categoryRow(route: Route, icon: ImageVector, desc: Int, navigate: (Route) -> Unit) =
    categoryRow(route, icon, summary = { stringResource(desc) }, navigate)

private fun categoryRow(
    route: Route,
    icon: ImageVector,
    summary: @Composable () -> String,
    navigate: (Route) -> Unit,
): @Composable (Shape) -> Unit = { shapes ->
    SettingsItem(
        onClick = { navigate(route) },
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(summary()) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
    ) { Text(stringResource(route.titleRes)) }
}

/** How many of the user-grantable accesses are in place (privileged builds count as granted). */
private fun permissionCount(caps: CapabilityState): Pair<Int, Int> {
    val priv = caps.tier == Tier.PRIVILEGED
    val all = listOf(
        caps.postNotifications,
        caps.overlay || priv,
        caps.notificationAccess,
        caps.fullScreenIntent || priv,
        caps.usageAccess,
        caps.writeSettings,
    )
    return all.count { it } to all.size
}

val Route.titleRes: Int
    get() = when (this) {
        Route.MAIN -> R.string.app_name
        Route.ACCESS -> R.string.section_access
        Route.BEHAVIOUR -> R.string.section_behaviour
        Route.LOCK -> R.string.section_lock
        Route.COVER -> R.string.section_cover
        Route.STYLE -> R.string.section_style
        Route.CAMERA -> R.string.section_camera
        Route.DIAGNOSTICS -> R.string.section_diagnostics
        Route.PAGES -> R.string.pref_pages
        Route.APPS -> R.string.pref_apps
        Route.WIDGETS -> R.string.pref_widgets
        Route.CLOCK -> R.string.pref_clock
    }

/** Live, scaled-down cover with the camera block drawn in. */
@Composable
private fun CoverPreview(runtime: CoverRuntime, tweak: (CoverUiState) -> CoverUiState = { it }) {
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val state = tweak(rememberCoverUiState(runtime, locked = false, edgePaddingDp = 16, showCameraGuide = true))
    CoverPreviewFrame(Modifier.padding(vertical = 8.dp)) {
        CoverScreen(
            state = state,
            actions = NoopCoverActions,
            // arcfox status_bar_height_portrait; the preview has no real status bar.
            topInset = if (settings.showStatusBar) 33.dp else 0.dp,
            widget = { id, modifier -> HostedWidget(runtime, id, modifier) },
        )
    }
}

@Composable
private fun Hero(runtime: CoverRuntime, caps: CapabilityState, fold: FoldSnapshot, host: SettingsHost) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverPreview(runtime)
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

private fun permissionRows(caps: CapabilityState, host: SettingsHost): List<@Composable (Shape) -> Unit> {
    val priv = caps.tier == Tier.PRIVILEGED
    val perms = listOf(
        Perm(Icons.Rounded.Notifications, R.string.perm_post, R.string.perm_post_desc, caps.postNotifications, host::grantPostNotifications),
        Perm(Icons.Rounded.Layers, R.string.perm_overlay, R.string.perm_overlay_desc, caps.overlay || priv, host::grantOverlay),
        Perm(Icons.Rounded.NotificationsActive, R.string.perm_listener, R.string.perm_listener_desc, caps.notificationAccess, host::grantNotificationAccess),
        Perm(Icons.Rounded.Fullscreen, R.string.perm_fsi, R.string.perm_fsi_desc, caps.fullScreenIntent || priv, host::grantFullScreenIntent),
        Perm(Icons.Rounded.QueryStats, R.string.perm_usage, R.string.perm_usage_desc, caps.usageAccess, host::grantUsageAccess),
        Perm(Icons.Rounded.BrightnessMedium, R.string.perm_write_settings, R.string.perm_write_settings_desc, caps.writeSettings, host::grantWriteSettings),
    )
    // Explicit type arguments: the platform's kotlinc doesn't infer the
    // lambda as @Composable from the return type.
    return perms.map<Perm, @Composable (Shape) -> Unit> { p ->
        { shapes ->
            SettingsItem(
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
): @Composable (Shape) -> Unit = { shapes ->
    SettingsItem(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(stringResource(desc)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    ) { Text(stringResource(title)) }
}

private fun navRow(icon: ImageVector, title: Int, desc: Int, onClick: () -> Unit): @Composable (Shape) -> Unit = { shapes ->
    SettingsItem(
        onClick = onClick,
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(stringResource(desc)) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
    ) { Text(stringResource(title)) }
}

/** Everything on the Lock screen screen. */
private fun lockRows(
    lock: LockConfig,
    caps: CapabilityState,
    prefs: CoverPrefs,
    scope: CoroutineScope,
    navigate: (Route) -> Unit,
): List<@Composable (Shape) -> Unit> = rows(
    switchRow(Icons.Rounded.Lock, R.string.pref_lock_minimal, R.string.pref_lock_minimal_desc, lock.enabled) {
        scope.launch { prefs.setLockEnabled(it) }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_lock_layout,
            icon = Icons.Rounded.ViewAgenda,
            options = LockLayout.entries,
            selected = lock.layout,
            label = { stringResource(if (it == LockLayout.CARDS) R.string.lock_layout_cards else R.string.lock_layout_big) },
            shapes = shapes,
        ) { l -> scope.launch { prefs.setLockLayout(l) } }
    },
    navRow(Icons.Rounded.AccessTime, R.string.pref_clock, R.string.pref_lock_clock_desc) { navigate(Route.CLOCK) },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_lock_notifications,
            icon = Icons.Rounded.Notifications,
            options = LockNotifications.entries,
            selected = lock.notifications,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
            footer = stringResource(R.string.pref_lock_notifications_desc),
        ) { n -> scope.launch { prefs.setLockNotifications(n) } }
    },
    switchRow(Icons.Rounded.MusicNote, R.string.pref_lock_media, R.string.pref_lock_media_desc, lock.media) {
        scope.launch { prefs.setLockMedia(it) }
    },
    { shapes -> LockTabsRow(lock, shapes) { page, shown -> scope.launch { prefs.setLockTab(page, shown) } } },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_unlock_gesture,
            icon = Icons.Rounded.TouchApp,
            options = UnlockGesture.entries,
            selected = lock.gesture,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
            content = { Text(stringResource(it.shortLabelRes), maxLines = 1) },
        ) { g -> scope.launch { prefs.setUnlockGesture(g) } }
    },
    { shapes ->
        val trust by TrustUnlock.status.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val biometric = remember { SystemFingerprint.enrolled(context) }
        if (remember { SystemFingerprint.active(context) }) {
            // SystemUI takes the fingerprint under the cover: nothing to choose, no prompt.
            val row = navRow(
                icon = Icons.Rounded.Fingerprint,
                title = R.string.pref_fingerprint,
                desc = if (biometric) R.string.fingerprint_system else R.string.fingerprint_no_biometric,
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
            )
            row(shapes)
        } else ChoiceRow(
            title = R.string.pref_fingerprint,
            icon = Icons.Rounded.Fingerprint,
            options = FingerprintUnlock.entries,
            selected = lock.fingerprint,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
            content = { Text(stringResource(it.shortLabelRes), maxLines = 1) },
            footer = stringResource(
                when {
                    lock.fingerprint == FingerprintUnlock.OFF -> R.string.fingerprint_off_desc
                    !caps.trustAgent -> R.string.fingerprint_needs_system
                    !biometric -> R.string.fingerprint_no_biometric
                    !lock.enabled -> R.string.fingerprint_needs_lock_screen
                    trust == TrustStatus.NOT_CONNECTED -> R.string.fingerprint_agent_off
                    trust == TrustStatus.READY -> R.string.fingerprint_ready
                    else -> R.string.fingerprint_waiting
                },
            ),
        ) { f -> scope.launch { prefs.setFingerprintUnlock(f) } }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_lock_hint,
            icon = Icons.Rounded.Lock,
            options = LockHintPosition.entries,
            selected = lock.hintPosition,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
        ) { p -> scope.launch { prefs.setLockHintPosition(p) } }
    },
    { shapes ->
        LockStripRow(
            lock,
            shapes,
            onMedia = { scope.launch { prefs.setLockStripMedia(it) } },
            onNotifications = { scope.launch { prefs.setLockStripNotifications(it) } },
        )
    },
)

/** The pages that may be swiped to while locked, as chips. */
@Composable
private fun LockTabsRow(lock: LockConfig, shapes: Shape, onChange: (CoverPage, Boolean) -> Unit) {
    SettingsItem(
        onClick = {},
        shapes = shapes,
        leadingContent = { Icon(Icons.Rounded.ViewCarousel, contentDescription = null) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.pref_lock_tabs_desc))
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LockTabCandidates.forEach { page ->
                        val selected = page in lock.tabs
                        FilterChip(
                            selected = selected,
                            onClick = { onChange(page, !selected) },
                            label = { Text(stringResource(page.labelRes)) },
                            leadingIcon = { Icon(page.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        },
    ) { Text(stringResource(R.string.pref_lock_tabs)) }
}

private val FingerprintUnlock.labelRes: Int
    get() = when (this) {
        FingerprintUnlock.OFF -> R.string.fingerprint_mode_off
        FingerprintUnlock.ON_GESTURE -> R.string.fingerprint_mode_gesture
        FingerprintUnlock.IMMEDIATE -> R.string.fingerprint_mode_immediate
    }

private val FingerprintUnlock.shortLabelRes: Int
    get() = when (this) {
        FingerprintUnlock.OFF -> R.string.fingerprint_mode_off
        FingerprintUnlock.ON_GESTURE -> R.string.fingerprint_mode_gesture_short
        FingerprintUnlock.IMMEDIATE -> R.string.fingerprint_mode_immediate_short
    }

/** What sits beside the cameras on the lock screen, as chips. */
@Composable
private fun LockStripRow(lock: LockConfig, shapes: Shape, onMedia: (Boolean) -> Unit, onNotifications: (Boolean) -> Unit) {
    SettingsItem(
        onClick = {},
        shapes = shapes,
        leadingContent = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.pref_lock_strip_desc))
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = lock.stripMedia,
                        onClick = { onMedia(!lock.stripMedia) },
                        enabled = lock.media,
                        label = { Text(stringResource(R.string.lock_strip_media)) },
                        leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    FilterChip(
                        selected = lock.stripNotifications,
                        onClick = { onNotifications(!lock.stripNotifications) },
                        enabled = lock.notifications == LockNotifications.ICONS,
                        label = { Text(stringResource(R.string.lock_strip_notifications)) },
                        leadingIcon = { Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        },
    ) { Text(stringResource(R.string.pref_lock_strip)) }
}

private val LockHintPosition.labelRes: Int
    get() = when (this) {
        LockHintPosition.TOP -> R.string.lock_hint_top
        LockHintPosition.STRIP -> R.string.lock_hint_strip
        LockHintPosition.HIDDEN -> R.string.lock_hint_hidden
    }

private val LockNotifications.labelRes: Int
    get() = when (this) {
        LockNotifications.HIDDEN -> R.string.lock_notifications_hidden
        LockNotifications.ICONS -> R.string.lock_notifications_icons
        LockNotifications.CARDS -> R.string.lock_notifications_cards
    }

private val UnlockGesture.labelRes: Int
    get() = when (this) {
        UnlockGesture.TAP_OR_SWIPE -> R.string.unlock_gesture_tap
        UnlockGesture.SWIPE -> R.string.unlock_gesture_swipe
        UnlockGesture.DOUBLE_TAP -> R.string.unlock_gesture_double_tap
    }

private val UnlockGesture.shortLabelRes: Int
    get() = when (this) {
        UnlockGesture.TAP_OR_SWIPE -> R.string.unlock_gesture_tap_short
        UnlockGesture.SWIPE -> R.string.unlock_gesture_swipe_short
        UnlockGesture.DOUBLE_TAP -> R.string.unlock_gesture_double_tap_short
    }

/** Everything on the Clock screen. */
@Composable
private fun clockRows(
    clock: ClockConfig,
    idleAospClock: Boolean,
    prefs: CoverPrefs,
    scope: CoroutineScope,
): List<@Composable (Shape) -> Unit> = rows(
    { shapes -> FontRow(clock, shapes) { f -> scope.launch { prefs.setClockFont(f) } } },
    { shapes ->
        SliderRow(
            value = clock.weight,
            min = ClockConfig.MIN_WEIGHT,
            max = ClockConfig.MAX_WEIGHT,
            steps = (ClockConfig.MAX_WEIGHT - ClockConfig.MIN_WEIGHT) / 50 - 1,
            title = R.string.pref_clock_weight,
            desc = R.string.pref_clock_weight_desc,
            shapes = shapes,
        ) { v -> scope.launch { prefs.setClockWeight(v) } }
    },
    { shapes ->
        SliderRow(
            value = clock.scale,
            min = ClockConfig.MIN_SCALE,
            max = ClockConfig.MAX_SCALE,
            steps = (ClockConfig.MAX_SCALE - ClockConfig.MIN_SCALE) / 5 - 1,
            title = R.string.pref_clock_size,
            desc = R.string.pref_clock_size_desc,
            shapes = shapes,
        ) { v -> scope.launch { prefs.setClockScale(v) } }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_clock_layout,
            icon = Icons.Rounded.AccessTime,
            options = ClockStyle.entries,
            selected = clock.layout,
            label = { stringResource(if (it == ClockStyle.STACKED) R.string.clock_stacked else R.string.clock_inline) },
            shapes = shapes,
        ) { s -> scope.launch { prefs.setClockStyle(s) } }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_clock_align,
            icon = Icons.Rounded.FormatAlignCenter,
            options = ClockAlign.entries,
            selected = clock.align,
            label = { stringResource(if (it == ClockAlign.CENTER) R.string.clock_align_center else R.string.clock_align_start) },
            shapes = shapes,
        ) { a -> scope.launch { prefs.setClockAlign(a) } }
    },
    { shapes ->
        val colors = MaterialTheme.colorScheme
        ChoiceRow(
            title = R.string.pref_clock_color,
            icon = Icons.Rounded.Palette,
            options = ClockColor.entries,
            selected = clock.color,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
            content = { c ->
                val swatch = when (c) {
                    ClockColor.ACCENT -> colors.primaryFixed
                    ClockColor.PRIMARY -> colors.primary
                    ClockColor.TERTIARY -> colors.tertiary
                    ClockColor.WHITE -> Color.White
                }
                Box(Modifier.size(22.dp).clip(CircleShape).background(swatch))
            },
        ) { c -> scope.launch { prefs.setClockColor(c) } }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_hour_format,
            icon = Icons.Rounded.Schedule,
            options = HourFormat.entries,
            selected = clock.hourFormat,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
            content = { Text(stringResource(it.shortLabelRes), maxLines = 1) },
        ) { f -> scope.launch { prefs.setHourFormat(f) } }
    },
    switchRow(Icons.Rounded.Timer, R.string.pref_clock_seconds, R.string.pref_clock_seconds_desc, clock.seconds) {
        scope.launch { prefs.setClockSeconds(it) }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_date_style,
            icon = Icons.Rounded.CalendarToday,
            options = DateStyle.entries,
            selected = clock.dateStyle,
            label = { stringResource(it.labelRes) },
            shapes = shapes,
            content = { Text(stringResource(it.shortLabelRes), maxLines = 1) },
        ) { d -> scope.launch { prefs.setDateStyle(d) } }
    },
    { shapes ->
        ChoiceRow(
            title = R.string.pref_date_position,
            icon = Icons.Rounded.VerticalAlignTop,
            options = DatePosition.entries,
            selected = clock.datePosition,
            label = { stringResource(if (it == DatePosition.ABOVE) R.string.date_above else R.string.date_below) },
            shapes = shapes,
        ) { p -> scope.launch { prefs.setDatePosition(p) } }
    },
    switchRow(Icons.Rounded.Bedtime, R.string.pref_idle_clock, R.string.pref_idle_clock_desc, idleAospClock) {
        scope.launch { prefs.setIdleAospClock(it) }
    },
)

/** Font chips, each label set in its own font. */
@Composable
private fun FontRow(clock: ClockConfig, shapes: Shape, onChange: (ClockFont) -> Unit) {
    SettingsItem(
        onClick = {},
        shapes = shapes,
        leadingContent = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
        supportingContent = {
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ClockFont.entries.forEach { font ->
                    val family = clock.copy(font = font).fontFamily()
                    FilterChip(
                        selected = font == clock.font,
                        onClick = { onChange(font) },
                        label = { Text(stringResource(font.labelRes), fontFamily = family) },
                    )
                }
            }
        },
    ) { Text(stringResource(R.string.pref_clock_font)) }
}

private val HomeStyle.labelRes: Int
    get() = when (this) {
        HomeStyle.CARDS -> R.string.home_style_cards
        HomeStyle.DOCK -> R.string.home_style_dock
        HomeStyle.BIG_CLOCK -> R.string.home_style_big
    }

private val HomeStyle.shortLabelRes: Int
    get() = when (this) {
        HomeStyle.CARDS -> R.string.home_style_cards
        HomeStyle.DOCK -> R.string.home_style_dock_short
        HomeStyle.BIG_CLOCK -> R.string.home_style_big_short
    }

private val ClockFont.labelRes: Int
    get() = when (this) {
        ClockFont.SYSTEM -> R.string.clock_font_system
        ClockFont.FLEX -> R.string.clock_font_flex
        ClockFont.CONDENSED -> R.string.clock_font_condensed
        ClockFont.SERIF -> R.string.clock_font_serif
        ClockFont.MONO -> R.string.clock_font_mono
        ClockFont.CURSIVE -> R.string.clock_font_cursive
        ClockFont.CASUAL -> R.string.clock_font_casual
    }

private val ClockColor.labelRes: Int
    get() = when (this) {
        ClockColor.ACCENT -> R.string.clock_color_accent
        ClockColor.PRIMARY -> R.string.clock_color_primary
        ClockColor.TERTIARY -> R.string.clock_color_tertiary
        ClockColor.WHITE -> R.string.clock_color_white
    }

private val HourFormat.labelRes: Int
    get() = when (this) {
        HourFormat.SYSTEM -> R.string.hour_format_system
        HourFormat.H24 -> R.string.hour_format_24
        HourFormat.H12 -> R.string.hour_format_12
    }

private val HourFormat.shortLabelRes: Int
    get() = when (this) {
        HourFormat.SYSTEM -> R.string.hour_format_system_short
        HourFormat.H24 -> R.string.hour_format_24_short
        HourFormat.H12 -> R.string.hour_format_12_short
    }

private val DateStyle.labelRes: Int
    get() = when (this) {
        DateStyle.SHORT -> R.string.date_style_short
        DateStyle.LONG -> R.string.date_style_long
        DateStyle.NUMERIC -> R.string.date_style_numeric
        DateStyle.HIDDEN -> R.string.date_style_hidden
    }

private val DateStyle.shortLabelRes: Int
    get() = when (this) {
        DateStyle.SHORT -> R.string.date_style_short_btn
        DateStyle.LONG -> R.string.date_style_long_btn
        DateStyle.NUMERIC -> R.string.date_style_numeric_btn
        DateStyle.HIDDEN -> R.string.date_style_hidden_btn
    }

@Composable
private fun <T> ChoiceRow(
    title: Int,
    icon: ImageVector,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    shapes: Shape,
    footer: String? = null,
    content: @Composable (T) -> Unit = { Text(label(it), maxLines = 1) },
    onChange: (T) -> Unit,
) {
    SettingsItem(
        onClick = {},
        shapes = shapes,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = {
            Column {
                Text(label(selected))
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    options.forEachIndexed { i, option ->
                        val buttonShapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        val description = label(option)
                        ToggleButton(
                            checked = option == selected,
                            onCheckedChange = { onChange(option) },
                            shapes = buttonShapes,
                            modifier = Modifier.weight(1f).semantics { contentDescription = description },
                        ) { content(option) }
                    }
                }
                if (footer != null) {
                    Text(footer, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
    ) { Text(stringResource(title)) }
}

private val ThemedIcons.labelRes: Int
    get() = when (this) {
        ThemedIcons.OFF -> R.string.themed_icons_off
        ThemedIcons.ON -> R.string.themed_icons_on
        ThemedIcons.SYSTEM -> R.string.themed_icons_system
    }

private val IconShapeStyle.labelRes: Int
    get() = when (this) {
        IconShapeStyle.EXPRESSIVE -> R.string.icon_shape_expressive
        IconShapeStyle.CIRCLE -> R.string.icon_shape_circle
        IconShapeStyle.SQUIRCLE -> R.string.icon_shape_squircle
        IconShapeStyle.SYSTEM -> R.string.icon_shape_system
    }

@Composable
private fun CameraSideRow(side: CameraSide, shapes: Shape, onChange: (CameraSide) -> Unit) {
    SettingsItem(
        onClick = {},
        shapes = shapes,
        leadingContent = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
        supportingContent = {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ToggleButton(
                    checked = side == CameraSide.LEFT,
                    onCheckedChange = { onChange(CameraSide.LEFT) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.camera_left)) }
                ToggleButton(
                    checked = side == CameraSide.RIGHT,
                    onCheckedChange = { onChange(CameraSide.RIGHT) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.camera_right)) }
            }
        },
    ) { Text(stringResource(R.string.pref_camera_side)) }
}

/** A dp value with a slider; saved when the drag ends, shown live in the title. */
@Composable
private fun SliderRow(
    value: Int,
    max: Int,
    title: Int,
    desc: Int,
    shapes: Shape,
    min: Int = 0,
    steps: Int = 0,
    onChange: (Int) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    SettingsItem(
        onClick = {},
        shapes = shapes,
        supportingContent = {
            Column {
                Text(stringResource(desc))
                Slider(
                    value = local,
                    onValueChange = { local = it },
                    onValueChangeFinished = { onChange(local.roundToInt()) },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps,
                )
            }
        },
    ) { Text(stringResource(title, local.roundToInt())) }
}

private fun diagnosticsRows(
    runtime: CoverRuntime,
    fold: FoldSnapshot,
    caps: CapabilityState,
): List<@Composable (Shape) -> Unit> {
    fun row(title: Int, value: String): @Composable (Shape) -> Unit = { shapes ->
        SettingsItem(onClick = {}, shapes = shapes, supportingContent = { Text(value) }) { Text(stringResource(title)) }
    }
    val states = SystemConfig.foldedDeviceStates.sorted().joinToString(",").ifEmpty { "—" }
    return listOf(
        { shapes ->
            SettingsItem(onClick = {}, shapes = shapes, supportingContent = { Text(stringResource(fold.source.labelRes)) }) {
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
            SettingsItem(
                onClick = {},
                shapes = shapes,
                supportingContent = {
                    Text(stringResource(if (caps.allShortcuts) R.string.diag_shortcuts_all else R.string.diag_shortcuts_manifest))
                },
            ) { Text(stringResource(R.string.diag_shortcuts)) }
        },
        { shapes ->
            val scope = rememberCoroutineScope()
            val known by runtime.prefs.knownPanels.collectAsStateWithLifecycle(emptySet())
            SettingsItem(
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

