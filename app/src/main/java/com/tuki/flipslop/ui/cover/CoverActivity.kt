package com.tuki.flipslop.ui.cover

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.RoundedCorner
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
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
import com.tuki.flipslop.widgets.WidgetAdder
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The cover screen launcher. Shown on logical display 0 while the device is
 * folded, over the keyguard, and closed again as soon as the device opens.
 */
class CoverActivity : ComponentActivity() {

    private val runtime by lazy { coverRuntime }
    private var locked by mutableStateOf(false)
    private var edgePadding by mutableIntStateOf(16)
    private var pickingWidget by mutableStateOf(false)
    private lateinit var widgetAdder: WidgetAdder

    /** Opened from settings for a preview on the inner screen: do not auto-close. */
    private var stay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stay = intent.getBooleanExtra(EXTRA_STAY, false)
        widgetAdder = WidgetAdder(this, runtime.widgets, savedInstanceState)
        enableEdgeToEdge()
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        applySystemBars(showStatusBar = runtime.settings.value.showStatusBar)

        setContent {
            FlipSlopTheme(darkTheme = true) {
                val state = rememberCoverUiState(runtime, locked, edgePadding)
                val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box {
                    CoverScreen(
                        state = state,
                        actions = actions,
                        topInset = statusBar,
                        widget = { id, modifier -> HostedWidget(runtime, id, modifier) },
                    )
                    BackHandler(enabled = pickingWidget) { pickingWidget = false }
                    AnimatedVisibility(
                        visible = pickingWidget,
                        enter = fadeIn() + slideInVertically { it / 3 },
                        exit = fadeOut() + slideOutVertically { it / 3 },
                    ) {
                        val providers = remember { runtime.widgets.providers() }
                        CoverWidgetPicker(
                            providers = providers,
                            topInset = maxOf(edgePadding.dp, statusBar),
                            bottomInset = state.bottomInsetDp.dp,
                            onPick = { info ->
                                pickingWidget = false
                                unlocked { widgetAdder.add(info) }
                            },
                            onClose = { pickingWidget = false },
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                runtime.settings.map { it.showStatusBar }.distinctUntilChanged().collect(::applySystemBars)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                runtime.settings.map { it.wallpaperBlurDp }.distinctUntilChanged().collect(::applyWallpaperBlur)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        widgetAdder.onSaveInstanceState(outState)
    }

    // AppWidgetHost only offers the startActivityForResult based configure flow.
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgetAdder.onActivityResult(requestCode, resultCode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        stay = intent.getBooleanExtra(EXTRA_STAY, false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        edgePadding = cornerPaddingDp()
    }

    /** The keyguard comes and goes while we stay on screen; keep [locked] in step. */
    private val keyguardReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> locked = false
                else -> {
                    refreshLocked()
                    // The keyguard may engage a moment after the screen goes off.
                    window.decorView.postDelayed(::refreshLocked, KEYGUARD_SETTLE_MS)
                }
            }
        }
    }

    private fun refreshLocked() {
        locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            keyguardReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            RECEIVER_NOT_EXPORTED,
        )
        refreshLocked()
        runtime.widgets.startListening()
        runtime.toggles.start()
    }

    override fun onResume() {
        super.onResume()
        runtime.coverVisible.value = true
        runtime.launcher.cancelFallback()
        refreshLocked()
        runtime.refreshCapabilities()
        runtime.toggles.refresh()
    }

    override fun onPause() {
        runtime.coverVisible.value = false
        super.onPause()
    }

    override fun onStop() {
        runCatching { unregisterReceiver(keyguardReceiver) }
        runtime.toggles.stop()
        runtime.widgets.stopListening()
        super.onStop()
    }

    /**
     * Blurs the wallpaper showing through the window (window background
     * blur, Android 12+). SurfaceFlinger only draws it while cross-window
     * blur is enabled: supported by the device and not in battery saver.
     */
    private fun applyWallpaperBlur(dp: Int) {
        val px = (dp * resources.displayMetrics.density).roundToInt()
        runCatching { window.setBackgroundBlurRadius(px) }
    }

    /** The navigation bar never shows on the cover; the status bar is a setting. */
    private fun applySystemBars(showStatusBar: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            hide(WindowInsetsCompat.Type.navigationBars())
            if (showStatusBar) show(WindowInsetsCompat.Type.statusBars())
            else hide(WindowInsetsCompat.Type.statusBars())
        }
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

        override fun mediaOpen() = unlocked {
            if (!runtime.media.openApp()) {
                Toast.makeText(this@CoverActivity, R.string.cannot_open, Toast.LENGTH_SHORT).show()
            }
        }

        override fun launchApp(entry: AppEntry) = runtime.apps.launch(this@CoverActivity, entry)
        override fun togglePin(entry: AppEntry) = runtime.apps.togglePin(entry)

        override fun addWidget() {
            pickingWidget = true
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

        override fun unlock() = unlocked { }
    }

    companion object {
        const val EXTRA_REASON = "com.tuki.flipslop.extra.REASON"
        const val EXTRA_STAY = "com.tuki.flipslop.extra.STAY"
        private const val KEYGUARD_SETTLE_MS = 1_000L
    }
}

