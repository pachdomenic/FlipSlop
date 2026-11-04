package com.tuki.flipslop.ui.cover

import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.data.ClockConfig
import com.tuki.flipslop.data.HomeStyle
import com.tuki.flipslop.data.NotificationStyle
import com.tuki.flipslop.data.IconShapeStyle
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.data.DefaultPages
import com.tuki.flipslop.media.MediaState
import com.tuki.flipslop.notifications.CoverAction
import com.tuki.flipslop.notifications.CoverNotification
import com.tuki.flipslop.notifications.LockScreenPolicy
import com.tuki.flipslop.toggles.ToggleStates

data class BatteryInfo(val level: Int, val charging: Boolean)

/** The strip beside the camera block and what goes into it. */
data class DockConfig(
    val cameraSide: CameraSide = CameraSide.LEFT,
    val cameraWidthDp: Int = 0,
    /** Keep the page bar out instead of hiding it behind page dots. */
    val pageBarAlways: Boolean = false,
    /** Page dots (which also bring out the page bar); off = swipe only. */
    val showPageDots: Boolean = true,
    val notifications: Boolean = true,
    val media: Boolean = true,
)

data class CoverUiState(
    val now: Long,
    val is24h: Boolean = true,
    val battery: BatteryInfo? = null,
    val locked: Boolean = false,
    /** Show the simple lock screen while [locked]. */
    val lockMinimal: Boolean = true,
    val lockNotifications: Boolean = false,
    /** The system's lock screen notification switches. */
    val lockPolicy: LockScreenPolicy = LockScreenPolicy(),
    val notifications: List<CoverNotification> = emptyList(),
    val notificationAccess: Boolean = false,
    val media: MediaState? = null,
    val pinnedApps: List<AppEntry> = emptyList(),
    val allApps: List<AppEntry> = emptyList(),
    val widgetIds: List<Int> = emptyList(),
    val toggles: ToggleStates = ToggleStates(),
    val pages: List<CoverPage> = DefaultPages,
    val bottomInsetDp: Int = 0,
    /** Padding that keeps content clear of the panel's rounded corners. */
    val edgePaddingDp: Int = 16,
    val clock: ClockConfig = ClockConfig(),
    val homeStyle: HomeStyle = HomeStyle.CARDS,
    val notificationStyle: NotificationStyle = NotificationStyle.COMPACT,
    val iconShape: IconShapeStyle = IconShapeStyle.EXPRESSIVE,
    /** Resolved: draw themed (monochrome) icons. */
    val themedIcons: Boolean = false,
    val idleAospClock: Boolean = true,
    val mediaArtBackground: Boolean = true,
    val artBlurDp: Int = 32,
    /** The system status bar is showing (it has its own battery icon). */
    val systemStatusBar: Boolean = false,
    val dock: DockConfig = DockConfig(),
    /** Draw the camera block (settings preview and screenshots). */
    val showCameraGuide: Boolean = false,
) {
    /** Nothing to tell the user: no notifications, nothing playing. */
    val isIdle: Boolean get() = notifications.isEmpty() && media == null
}

/** Everything the cover UI can ask for. Defaults do nothing (previews, tests). */
interface CoverActions {
    fun openNotification(item: CoverNotification) {}
    fun dismissNotification(key: String) {}
    fun dismissAllNotifications() {}
    fun runAction(action: CoverAction) {}
    fun reply(action: CoverAction, text: String) {}
    fun grantNotificationAccess() {}

    fun mediaPlayPause() {}
    fun mediaNext() {}
    fun mediaPrevious() {}
    fun mediaSeek(positionMs: Long) {}
    fun mediaOpen() {}

    fun launchApp(entry: AppEntry) {}
    fun togglePin(entry: AppEntry) {}

    fun addWidget() {}

    fun setTorch(on: Boolean) {}
    fun setDnd(on: Boolean) {}
    fun cycleRinger() {}
    fun setWifi(on: Boolean) {}
    fun setBluetooth(on: Boolean) {}
    fun setBrightness(level: Float) {}
    fun setAutoBrightness(on: Boolean) {}
    fun openPanel(action: String) {}

    fun openSettings() {}

    /** Bring up the bouncer; the full cover shows once unlocked. */
    fun unlock() {}
}

object NoopCoverActions : CoverActions
