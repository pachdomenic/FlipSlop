package com.tuki.flipslop.ui

import android.os.Looper
import androidx.test.core.app.ActivityScenario
import com.github.takahirom.roborazzi.captureRoboImage
import com.tuki.flipslop.ui.settings.SettingsActivity
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The settings main screen on the inner screen (1080x2640 @ 420dpi = 411x1006 dp),
 * rendered from the real SettingsActivity. The window is captured directly after
 * a bounded run of the main looper: the live cover preview keeps the looper busy,
 * so waiting for Compose to go idle would never return.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h1006dp-port-420dpi")
class SettingsScreenshotTest {

    @Test
    fun categories() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_500))
            scenario.onActivity { it.window.decorView.captureRoboImage("screenshots/settings_main.png") }
        }
    }
}
