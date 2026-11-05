package com.tuki.flipslop.ui

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Process
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.core.DeviceProfile
import com.tuki.flipslop.data.ClockAlign
import com.tuki.flipslop.data.ClockColor
import com.tuki.flipslop.data.ClockConfig
import com.tuki.flipslop.data.ClockFont
import com.tuki.flipslop.data.ClockStyle
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.data.DatePosition
import com.tuki.flipslop.data.DateStyle
import com.tuki.flipslop.data.HourFormat
import com.tuki.flipslop.data.HomeStyle
import com.tuki.flipslop.data.NotificationStyle
import com.tuki.flipslop.data.IconShapeStyle
import com.tuki.flipslop.data.LockConfig
import com.tuki.flipslop.data.LockLayout
import com.tuki.flipslop.data.LockNotifications
import com.tuki.flipslop.data.UnlockGesture
import com.tuki.flipslop.media.MediaState
import com.tuki.flipslop.notifications.CoverAction
import com.tuki.flipslop.notifications.CoverNotification
import com.tuki.flipslop.toggles.ToggleStates
import com.tuki.flipslop.ui.cover.BatteryInfo
import com.tuki.flipslop.ui.cover.CoverScreen
import com.tuki.flipslop.ui.cover.CoverUiState
import com.tuki.flipslop.ui.cover.DockConfig
import com.tuki.flipslop.ui.cover.NoopCoverActions
import com.tuki.flipslop.ui.theme.FlipSlopTheme
import java.util.Calendar
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every cover pane at the arcfox cover configuration: 1080x1272 px at
 * 360 dpi = 480x565 dp, with the camera block from the arcfox profile drawn in.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w480dp-h565dp-port-360dpi", application = Application::class)
class CoverScreenshotTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val arcfox = DeviceProfile.ARCFOX

    @Test fun home() = shoot(CoverPage.HOME)
    @Test fun homeIdle() = shoot(CoverPage.HOME, idle = true, name = "home_idle")
    @Test fun homeBigClock() = shoot(CoverPage.HOME, name = "home_big") { it.copy(homeStyle = HomeStyle.BIG_CLOCK) }
    @Test fun homeBigClockCustom() = shoot(CoverPage.HOME, name = "home_big_custom") {
        it.copy(
            homeStyle = HomeStyle.BIG_CLOCK,
            clock = ClockConfig(
                layout = ClockStyle.INLINE,
                font = ClockFont.SERIF,
                weight = 300,
                color = ClockColor.WHITE,
                dateStyle = DateStyle.LONG,
                datePosition = DatePosition.BELOW,
                align = ClockAlign.START,
            ),
        )
    }
    @Test fun homeBigClockMono() = shoot(CoverPage.HOME, name = "home_big_mono") {
        it.copy(
            homeStyle = HomeStyle.BIG_CLOCK,
            clock = ClockConfig(font = ClockFont.MONO, weight = 800, color = ClockColor.TERTIARY, hourFormat = HourFormat.H12),
        )
    }
    @Test fun homeDock() = shoot(CoverPage.HOME, name = "home_dock") { it.copy(homeStyle = HomeStyle.DOCK) }
    @Test fun homeClassicDock() = shoot(CoverPage.HOME, docked = false, name = "home_classic") {
        it.copy(homeStyle = HomeStyle.DOCK)
    }
    @Test fun notificationsDetailed() = shoot(CoverPage.NOTIFICATIONS, name = "notifications_detailed") {
        it.copy(notificationStyle = NotificationStyle.DETAILED)
    }
    @Test fun locked() = shoot(CoverPage.HOME, name = "locked") { it.copy(locked = true) }
    @Test fun lockedNotifications() = shoot(CoverPage.HOME, name = "locked_notifications") {
        it.copy(locked = true, lock = LockConfig(notifications = LockNotifications.CARDS), media = null)
    }
    @Test fun lockedBigClock() = shoot(CoverPage.HOME, name = "locked_big") {
        it.copy(
            locked = true,
            lock = LockConfig(
                layout = LockLayout.BIG_CLOCK,
                notifications = LockNotifications.ICONS,
                tabs = setOf(CoverPage.NOTIFICATIONS, CoverPage.MEDIA),
                gesture = UnlockGesture.DOUBLE_TAP,
            ),
            fingerprintUnlock = true,
        )
    }
    // No Music page: the chip opens the music slide over the cover, unlocked and locked.
    @Test fun mediaSheet() = shoot(CoverPage.HOME, name = "media_sheet", tap = "Hinge Angle") {
        it.copy(homeStyle = HomeStyle.DOCK, pages = it.pages - CoverPage.MEDIA)
    }
    @Test fun lockedMediaSheet() = shoot(CoverPage.HOME, name = "locked_media_sheet", tap = "Hinge Angle") {
        it.copy(locked = true, lock = LockConfig(layout = LockLayout.BIG_CLOCK))
    }
    @Test fun lockedIdle() = shoot(CoverPage.HOME, name = "locked_idle") { it.copy(locked = true, media = null) }
    @Test fun appsCircle() = shoot(CoverPage.APPS, name = "apps_circle") { it.copy(iconShape = IconShapeStyle.CIRCLE) }
    @Test fun appsThemed() = shoot(CoverPage.APPS, name = "apps_themed") { it.copy(themedIcons = true) }
    @Test fun notifications() = shoot(CoverPage.NOTIFICATIONS)
    @Test fun media() = shoot(CoverPage.MEDIA)
    @Test fun apps() = shoot(CoverPage.APPS)
    @Test fun widgets() = shoot(CoverPage.WIDGETS)
    @Test fun toggles() = shoot(CoverPage.TOGGLES)

    @get:Rule
    val compose = createComposeRule()

    private fun shoot(
        page: CoverPage,
        idle: Boolean = false,
        docked: Boolean = true,
        name: String = page.name.lowercase(),
        /** Text of something to tap before the shot. */
        tap: String? = null,
        tweak: (CoverUiState) -> CoverUiState = { it },
    ) {
        val base = tweak(sampleState())
        val state = base.copy(
            notifications = if (idle) emptyList() else base.notifications,
            media = if (idle) null else base.media,
            locked = base.locked,
            dock = base.dock.copy(pageBarAlways = !docked, notifications = docked, media = docked),
        )
        // The clock, wavy progress and media ticker never go idle: drive time by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipSlopTheme(darkTheme = true, dynamicColor = false) {
                // Stand-in for the wallpaper the real window shows through.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF3B4A7A), Color(0xFF7A3B6B), Color(0xFF1E2B3A)))),
                ) {
                    // 33dp: arcfox status_bar_height_portrait. Robolectric draws no status bar.
                    CoverScreen(state = state, actions = NoopCoverActions, initialPage = page, topInset = 33.dp)
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        if (tap != null) {
            compose.onNodeWithText(tap).performClick()
            // With the clock driven by hand, state written by the click needs a nudge to recompose.
            Snapshot.sendApplyNotifications()
            compose.mainClock.advanceTimeBy(1_500)
        }
        compose.onRoot().captureRoboImage("screenshots/cover_$name.png")
    }

    private fun sampleState(): CoverUiState {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 4, 21, 37, 0) }.timeInMillis
        val pi = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val legacy = context.getDrawable(android.R.drawable.sym_def_app_icon)!!
        // Adaptive icons with a monochrome layer, for themed icons.
        fun adaptive(glyph: Int, color: Long) = AdaptiveIconDrawable(
            ColorDrawable(color.toInt()),
            context.getDrawable(glyph),
            context.getDrawable(glyph),
        )
        val glyphs = listOf(
            android.R.drawable.ic_menu_call to 0xFF2E7D32,
            android.R.drawable.ic_dialog_email to 0xFF1565C0,
            android.R.drawable.ic_menu_camera to 0xFF424242,
            android.R.drawable.ic_media_play to 0xFFC62828,
            android.R.drawable.ic_dialog_map to 0xFF00838F,
            android.R.drawable.ic_menu_gallery to 0xFFEF6C00,
        )
        val apps = listOf("Phone", "Messages", "Camera", "Music", "Maps", "Photos", "Chrome", "Clock")
            .mapIndexed { i, label ->
                val icon = glyphs.getOrNull(i)?.let { (g, c) -> adaptive(g, c) } ?: legacy
                AppEntry(ComponentName("app.$i", "app.$i.Main"), Process.myUserHandle(), label, icon)
            }
        return CoverUiState(
            now = now,
            is24h = true,
            battery = BatteryInfo(76, charging = true),
            notifications = listOf(
                notification("1", "Messages", "Alex", "Are we still on for tonight?", now - 4 * 60_000, reply = true, pi = pi),
                notification("2", "Gmail", "LineageOS Gerrit", "arcfox: overlay: add cover panel cutout", now - 35 * 60_000, pi = pi),
                notification("3", "Calendar", "Standup", "In 15 minutes", now - 90 * 60_000, pi = pi),
            ),
            notificationAccess = true,
            media = MediaState(
                packageName = "music",
                appLabel = "Music",
                title = "Hinge Angle",
                artist = "The Flip Phones",
                art = art(),
                isPlaying = true,
                positionMs = 84_000,
                positionUpdatedAt = SystemClock.elapsedRealtime(),
                speed = 0f,
                durationMs = 213_000,
                canSkipPrevious = true,
                canSkipNext = true,
                canSeek = true,
            ),
            pinnedApps = apps.take(6),
            allApps = apps,
            toggles = ToggleStates(torchAvailable = true, torch = false, dnd = true, wifi = true, bluetooth = true, bluetoothAvailable = true, brightness = 0.6f, canWriteSettings = true),
            bottomInsetDp = arcfox.coverBottomInsetDp,
            edgePaddingDp = 24,
            dock = DockConfig(cameraSide = arcfox.coverCameraSide, cameraWidthDp = arcfox.coverCameraWidthDp),
            showCameraGuide = true,
            systemStatusBar = true,
        )
    }

    private fun notification(
        key: String,
        app: String,
        title: String,
        text: String,
        time: Long,
        pi: PendingIntent,
        reply: Boolean = false,
    ) = CoverNotification(
        key = key,
        packageName = app.lowercase(),
        appLabel = app,
        smallIcon = Icon.createWithResource(context, android.R.drawable.ic_dialog_email),
        largeIcon = null,
        color = 0,
        title = title,
        text = text,
        postTime = time,
        isClearable = true,
        contentIntent = pi,
        actions = if (reply) {
            listOf(
                CoverAction("Reply", pi, listOf(android.app.RemoteInput.Builder("text").setLabel("Reply").build())),
                CoverAction("Mark as read", pi, emptyList()),
            )
        } else {
            emptyList()
        },
    )

    private fun art(): Bitmap {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 256f, 256f, 0xFFFFB86B.toInt(), 0xFF6B5BFF.toInt(), Shader.TileMode.CLAMP)
        }
        Canvas(bmp).drawRect(0f, 0f, 256f, 256f, paint)
        return bmp
    }
}
