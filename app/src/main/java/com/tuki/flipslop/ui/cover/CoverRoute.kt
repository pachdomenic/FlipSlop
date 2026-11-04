package com.tuki.flipslop.ui.cover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import android.util.SizeF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuki.flipslop.data.ThemedIcons
import com.tuki.flipslop.service.CoverRuntime

/** Collects every runtime flow into one [CoverUiState]. */
@Composable
fun rememberCoverUiState(
    runtime: CoverRuntime,
    locked: Boolean,
    edgePaddingDp: Int,
    showCameraGuide: Boolean = false,
): CoverUiState {
    val context = LocalContext.current
    val settings by runtime.settings.collectAsStateWithLifecycle()
    val notifications by runtime.notifications.items.collectAsStateWithLifecycle()
    val connected by runtime.notifications.connected.collectAsStateWithLifecycle()
    val media by runtime.media.state.collectAsStateWithLifecycle()
    val pinned by runtime.apps.pinned.collectAsStateWithLifecycle()
    val all by runtime.apps.all.collectAsStateWithLifecycle()
    val toggles by runtime.toggles.state.collectAsStateWithLifecycle()
    val systemThemed by runtime.iconTheme.themed.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Re-read the system's lock screen switches each time the device locks.
    val lockPolicy = remember(locked) { runtime.notifications.lockScreenPolicy() }
    var battery by remember { mutableStateOf<BatteryInfo?>(null) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    battery = intent.toBattery()
                } else {
                    now = System.currentTimeMillis()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)?.let { battery = it.toBattery() }
        now = System.currentTimeMillis()
        onDispose { context.unregisterReceiver(receiver) }
    }

    return CoverUiState(
        now = now,
        is24h = DateFormat.is24HourFormat(context),
        battery = battery,
        locked = locked,
        lockMinimal = settings.lockMinimal,
        lockNotifications = settings.lockNotifications,
        lockPolicy = lockPolicy,
        notifications = notifications,
        notificationAccess = connected,
        media = media,
        pinnedApps = pinned,
        allApps = all,
        widgetIds = settings.widgetIds,
        toggles = toggles,
        pages = settings.pages,
        bottomInsetDp = settings.bottomInsetDp,
        edgePaddingDp = edgePaddingDp,
        clock = settings.clock,
        homeStyle = settings.homeStyle,
        notificationStyle = settings.notificationStyle,
        iconShape = settings.iconShape,
        themedIcons = settings.themedIcons.resolve(systemThemed),
        idleAospClock = settings.idleAospClock,
        mediaArtBackground = settings.mediaArtBackground,
        artBlurDp = settings.artBlurDp,
        systemStatusBar = settings.showStatusBar,
        dock = DockConfig(
            cameraSide = settings.cameraSide,
            cameraWidthDp = settings.cameraWidthDp,
            pageBarAlways = settings.pageBarAlways,
            showPageDots = settings.showPageDots,
            notifications = settings.dockNotifications,
            media = settings.dockMedia,
        ),
        showCameraGuide = showCameraGuide,
    )
}

private fun Intent.toBattery(): BatteryInfo? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (level < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(level * 100 / scale, charging)
}

/** Hosts one app widget sized to the space the cover gives it. */
@Composable
fun HostedWidget(runtime: CoverRuntime, id: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            runtime.widgets.createView(ctx, id) ?: android.view.View(ctx)
        },
        update = { view ->
            val host = view as? android.appwidget.AppWidgetHostView ?: return@AndroidView
            view.post {
                val d = view.resources.displayMetrics.density
                if (view.width > 0 && view.height > 0) {
                    host.updateAppWidgetSize(
                        android.os.Bundle(),
                        listOf(SizeF(view.width / d, view.height / d)),
                    )
                }
            }
        },
        modifier = modifier,
    )
}

/** OFF / ON as chosen; SYSTEM takes the launcher's switch (false when unreadable). */
fun ThemedIcons.resolve(systemThemed: Boolean): Boolean = when (this) {
    ThemedIcons.OFF -> false
    ThemedIcons.ON -> true
    ThemedIcons.SYSTEM -> systemThemed
}
