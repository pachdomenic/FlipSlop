package com.tuki.flipslop.ui.cover

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.RoundedCorner
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tuki.flipslop.R
import com.tuki.flipslop.apps.AppEntry
import com.tuki.flipslop.coverRuntime
import com.tuki.flipslop.fold.FoldState
import com.tuki.flipslop.notifications.CoverAction
import com.tuki.flipslop.notifications.CoverNotification
import com.tuki.flipslop.notifications.CoverNotificationListener
import com.tuki.flipslop.ui.settings.SettingsActivity
import com.tuki.flipslop.ui.theme.FlipSlopTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The cover screen launcher. Shown on logical display 0 while the device is
 * folded, over the keyguard, and closed again as soon as the device opens.
 */
class CoverActivity : ComponentActivity() {

    private val runtime by lazy { coverRuntime }
    private var locked by mutableStateOf(false)
    private var edgePadding by mutableIntStateOf(16)

    /** Opened from settings for a preview on the inner screen: do not auto-close. */
    private var stay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stay = intent.getBooleanExtra(EXTRA_STAY, false)
        enableEdgeToEdge()
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            FlipSlopTheme(darkTheme = true) {
                val state = rememberCoverUiState(runtime, locked, edgePadding)
                CoverScreen(
                    state = state,
                    actions = actions,
                    widget = { id, modifier -> HostedWidget(runtime, id, modifier) },
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                runtime.fold.snapshot.collect { snap ->
                    if (!stay && (snap.state == FoldState.OPEN || snap.state == FoldState.HALF_OPEN)) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        stay = intent.getBooleanExtra(EXTRA_STAY, false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        edgePadding = cornerPaddingDp()
    }

    override fun onStart() {
        super.onStart()
        runtime.widgets.startListening()
        runtime.toggles.start()
    }

    override fun onResume() {
        super.onResume()
        runtime.coverVisible.value = true
        runtime.launcher.cancelFallback()
        locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        runtime.refreshCapabilities()
        runtime.toggles.refresh()
    }

    override fun onPause() {
        runtime.coverVisible.value = false
        super.onPause()
    }

    override fun onStop() {
        runtime.toggles.stop()
        runtime.widgets.stopListening()
        super.onStop()
    }

    /** Inset that clears the panel's rounded corners: r * (1 - cos 45°). */
    private fun cornerPaddingDp(): Int {
        val d = resources.displayMetrics.density
        val radius = window.decorView.rootWindowInsets
            ?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        val px = radius * (1 - cos(Math.PI / 4))
        return ((px / d).roundToInt() + 8).coerceAtLeast(16)
    }

    /** Runs [block] now, or after the user unlocks on the cover. */
    private fun unlocked(block: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) {
            block()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                locked = false
                block()
            }
        })
    }

    private fun startOnCover(intent: Intent) {
        runCatching {
            startActivity(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                runtime.launcher.launchOptions().setLaunchDisplayId(display.displayId).toBundle(),
            )
        }.onFailure { Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show() }
    }

    private val actions = object : CoverActions {
        override fun openNotification(item: CoverNotification) = unlocked { runtime.notifications.open(item) }
        override fun dismissNotification(key: String) = runtime.notifications.dismiss(key)
        override fun dismissAllNotifications() = runtime.notifications.dismissAll()
        override fun runAction(action: CoverAction) = unlocked { runtime.notifications.runAction(action) }
        override fun reply(action: CoverAction, text: String) {
            runtime.notifications.reply(action, text)
        }

        override fun grantNotificationAccess() = unlocked {
            startOnCover(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    CoverNotificationListener.component(this@CoverActivity).flattenToString(),
                ),
            )
        }

        override fun mediaPlayPause() = runtime.media.playPause()
        override fun mediaNext() {
            runtime.media.next()
        }

        override fun mediaPrevious() {
            runtime.media.previous()
        }

        override fun mediaSeek(positionMs: Long) {
            runtime.media.seekTo(positionMs)
        }

        override fun mediaOpen() = unlocked { runtime.media.openApp() }

        override fun launchApp(entry: AppEntry) = runtime.apps.launch(this@CoverActivity, entry)
        override fun togglePin(entry: AppEntry) = runtime.apps.togglePin(entry)

        override fun addWidget() = unlocked {
            startOnCover(Intent(this@CoverActivity, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_WIDGETS))
        }

        override fun setTorch(on: Boolean) {
            runtime.toggles.setTorch(on)
        }

        override fun setDnd(on: Boolean) {
            if (!runtime.toggles.setDnd(on)) {
                unlocked { startOnCover(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
            }
        }

        override fun cycleRinger() = runtime.toggles.cycleRinger()

        override fun setWifi(on: Boolean) {
            if (!runtime.toggles.setWifi(on)) openPanel(Settings.Panel.ACTION_WIFI)
        }

        override fun setBluetooth(on: Boolean) {
            if (!runtime.toggles.setBluetooth(on)) unlocked { startOnCover(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        }

        override fun setBrightness(level: Float) {
            if (!runtime.toggles.setBrightness(level)) {
                // No WRITE_SETTINGS: dim or brighten just this window.
                window.attributes = window.attributes.apply { screenBrightness = level.coerceIn(0.01f, 1f) }
            }
        }

        override fun setAutoBrightness(on: Boolean) {
            runtime.toggles.setAutoBrightness(on)
        }

        override fun openPanel(action: String) = unlocked { startOnCover(Intent(action)) }

        override fun openSettings() = unlocked { startOnCover(Intent(this@CoverActivity, SettingsActivity::class.java)) }
    }

    companion object {
        const val EXTRA_REASON = "com.tuki.flipslop.extra.REASON"
        const val EXTRA_STAY = "com.tuki.flipslop.extra.STAY"
    }
}

