package com.tuki.flipslop.service

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.tuki.flipslop.apps.AppsRepository
import com.tuki.flipslop.apps.SystemIconTheme
import com.tuki.flipslop.core.CapabilityState
import com.tuki.flipslop.core.Capabilities
import com.tuki.flipslop.data.CoverPrefs
import com.tuki.flipslop.data.CoverSettings
import com.tuki.flipslop.fold.FoldState
import com.tuki.flipslop.fold.FoldStateMonitor
import com.tuki.flipslop.media.MediaRepository
import com.tuki.flipslop.notifications.NotificationsRepository
import com.tuki.flipslop.toggles.TogglesController
import com.tuki.flipslop.widgets.WidgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Process-wide state. Lives as long as the process, which is kept alive either
 * by the system binding our notification listener or by [CoverService].
 */
class CoverRuntime(private val app: Application) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs = CoverPrefs(app)
    val settings: StateFlow<CoverSettings> =
        prefs.settings.stateIn(scope, SharingStarted.Eagerly, CoverSettings())

    val fold = FoldStateMonitor(app, prefs, scope)
    val notifications = NotificationsRepository(app)
    val media = MediaRepository(app)
    val apps = AppsRepository(app, prefs, scope)
    val widgets = WidgetRepository(app, prefs)
    val iconTheme = SystemIconTheme(app)
    val toggles = TogglesController(app, notifications)
    val launcher = CoverLauncher(app, this)
    private val homeWatcher = HomeReturnWatcher(app, this)

    private val _capabilities = MutableStateFlow(CapabilityState())
    val capabilities: StateFlow<CapabilityState> = _capabilities.asStateFlow()

    /** True while [com.tuki.flipslop.ui.cover.CoverActivity] is resumed. */
    val coverVisible = MutableStateFlow(false)

    private val powerManager = app.getSystemService(PowerManager::class.java)
    val interactive = MutableStateFlow(powerManager.isInteractive)

    private var started = false

    fun start() {
        if (started) return
        started = true
        Channels.ensure(app)
        refreshCapabilities()
        registerScreenReceiver()

        scope.launch {
            fold.snapshot.map { it.state }.distinctUntilChanged().collect(::onFoldStateChanged)
        }

        scope.launch {
            combine(
                fold.snapshot.map { it.state.isFolded },
                interactive,
                settings.map { it.returnAfterHome },
                capabilities.map { it.usageAccess },
                coverVisible,
            ) { folded, on, wanted, usage, visible -> folded && on && wanted && usage && !visible }
                .distinctUntilChanged()
                .collect(homeWatcher::setActive)
        }

        scope.launch {
            combine(notifications.connected, settings.map { it.autoShowOnFold }) { _, _ -> }
                .collect { ensureKeepAlive() }
        }

        scope.launch { apps.ensureDefaultPins() }
    }

    /** BOOT_COMPLETED: the one moment a foreground service start is always allowed. */
    fun onBoot() {
        start()
        scope.launch {
            delay(BOOT_SETTLE_MS)
            ensureKeepAlive()
        }
    }

    fun refreshCapabilities(): CapabilityState {
        iconTheme.refresh()
        val caps = Capabilities.read(app, fold.deviceStateAvailable, iconTheme.readable)
        _capabilities.value = caps
        return caps
    }

    /**
     * Keep the process alive for the fold monitor. A bound notification listener
     * already does that; otherwise run a quiet foreground service.
     */
    fun ensureKeepAlive() {
        val wanted = settings.value.autoShowOnFold
        if (wanted && !notifications.connected.value) {
            CoverService.start(app)
        } else {
            CoverService.stop(app)
        }
    }

    private suspend fun onFoldStateChanged(state: FoldState) {
        when (state) {
            FoldState.FOLDED -> {
                val s = prefs.settings.first()
                if (s.autoShowOnFold && interactive.value) {
                    launcher.show(CoverLauncher.Reason.FOLD)
                }
            }
            FoldState.OPEN, FoldState.HALF_OPEN -> launcher.cancelFallback()
            FoldState.UNKNOWN -> Unit
        }
    }

    private fun registerScreenReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        interactive.value = true
                        if (fold.snapshot.value.state.isFolded &&
                            settings.value.showOnWake &&
                            !coverVisible.value
                        ) {
                            launcher.show(CoverLauncher.Reason.WAKE)
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> interactive.value = false
                    Intent.ACTION_USER_PRESENT -> refreshCapabilities()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private companion object {
        const val BOOT_SETTLE_MS = 500L
    }
}
