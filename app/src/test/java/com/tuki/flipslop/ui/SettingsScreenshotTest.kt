package com.tuki.flipslop.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.tuki.flipslop.ui.settings.SettingsActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The settings app on the inner screen (1080x2640 @ 420dpi = 411x1006 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h1006dp-port-420dpi")
class SettingsScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun categories() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Look & style").performScrollTo()
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("screenshots/settings_main.png")
    }

    @Test
    fun style() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Look & style").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("screenshots/settings_style.png")
    }
}
