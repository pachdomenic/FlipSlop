package com.tuki.flipslop.ui.cover

import android.view.Surface
import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.apps.AppMenu
import com.tuki.flipslop.apps.AppShortcut
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.data.ClockConfig
import com.tuki.flipslop.data.HomeStyle
import com.tuki.flipslop.data.LockConfig
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

/** The screen edge the camera block ends up on once the display is rotated. */
enum class CameraEdge { BOTTOM, TOP, LEFT, RIGHT }

/**
 * Where the camera block sits on screen. [atEnd]: at the right end of the
 * bottom or top edge, or at the bottom end of the left or right edge.
 */
data class CameraPlacement(val edge: CameraEdge, val atEnd: Boolean) {
    /** The block is on the left or right edge: the strip beside it is a column. */
    val sideways: Boolean get() = edge == CameraEdge.LEFT || edge == CameraEdge.RIGHT

    companion object {
        /**
         * The block sits on the panel's bottom edge, on [side]. Surface rotations
         * turn the drawing the opposite way to the device: ROTATION_90 is the
         * device turned a quarter counter-clockwise, which brings the panel's
         * bottom edge to the right of the screen and its right end to the top.
         */
        fun of(rotation: Int, side: CameraSide): CameraPlacement {
            val right = side == CameraSide.RIGHT
            return when (rotation) {
                Surface.ROTATION_90 -> CameraPlacement(CameraEdge.RIGHT, atEnd = !right)
                Surface.ROTATION_180 -> CameraPlacement(CameraEdge.TOP, atEnd = !right)
                Surface.ROTATION_270 -> CameraPlacement(CameraEdge.LEFT, atEnd = right)
                else -> CameraPlacement(CameraEdge.BOTTOM, atEnd = right)
            }
        }
    }
}

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
    /** The lock screen shown while [locked]. */
    val lock: LockConfig = LockConfig(),
    /** A fingerprint on the cover can unlock right now (trust agent armed). */
    val fingerprintUnlock: Boolean = false,
    /** The system's lock screen notification switches. */
    val lockPolicy: LockScreenPolicy = LockScreenPolicy(),
    /** Goes up each time the cover leaves the screen (screen off): one-off slides close on it. */
    val hiddenCount: Int = 0,
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

    /**
     * The camera block on screen for a display rotation (a Surface.ROTATION_*).
     * With no bottom inset there is no block to keep clear of: natural layout.
     */
    fun cameraPlacement(rotation: Int): CameraPlacement =
        CameraPlacement.of(if (bottomInsetDp > 0) rotation else Surface.ROTATION_0, dock.cameraSide)
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

    /** What the app's long-press menu shows besides pinning. */
    suspend fun appMenu(entry: AppEntry): AppMenu = AppMenu()
    fun launchShortcut(shortcut: AppShortcut) {}
    fun openAppInfo(entry: AppEntry) {}
    fun uninstallApp(entry: AppEntry) {}

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
