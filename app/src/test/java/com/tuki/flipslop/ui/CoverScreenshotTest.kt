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
import android.graphics.drawable.Icon
import android.os.Process
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.data.CoverPage
import com.tuki.flipslop.media.MediaState
import com.tuki.flipslop.notifications.CoverAction
import com.tuki.flipslop.notifications.CoverNotification
import com.tuki.flipslop.toggles.ToggleStates
import com.tuki.flipslop.ui.cover.BatteryInfo
import com.tuki.flipslop.ui.cover.CoverScreen
import com.tuki.flipslop.ui.cover.CoverUiState
import com.tuki.flipslop.ui.cover.NoopCoverActions
import com.tuki.flipslop.ui.theme.FlipSlopTheme
import java.util.Calendar
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every cover pane at the arcfox cover configuration: 1080x1272 px at
 * 360 dpi = 480x565 dp, with the 143 dp bottom margin from the device tree.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w480dp-h565dp-port-360dpi", application = Application::class)
class CoverScreenshotTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun home() = shoot(CoverPage.HOME)
    @Test fun notifications() = shoot(CoverPage.NOTIFICATIONS)
    @Test fun media() = shoot(CoverPage.MEDIA)
    @Test fun apps() = shoot(CoverPage.APPS)
    @Test fun widgets() = shoot(CoverPage.WIDGETS)
    @Test fun toggles() = shoot(CoverPage.TOGGLES)

    @get:Rule
    val compose = createComposeRule()

    private fun shoot(page: CoverPage) {
        val state = sampleState()
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
                    CoverScreen(state = state, actions = NoopCoverActions, initialPage = page)
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("screenshots/cover_${page.name.lowercase()}.png")
    }

    private fun sampleState(): CoverUiState {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 4, 21, 37, 0) }.timeInMillis
        val pi = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val icon = context.getDrawable(android.R.drawable.sym_def_app_icon)!!
        val apps = listOf("Phone", "Messages", "Camera", "Music", "Maps", "Photos", "Chrome", "Clock")
            .mapIndexed { i, label ->
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
            bottomInsetDp = 143,
            edgePaddingDp = 24,
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
