package com.tuki.flipslop.service

import android.app.ActivityOptions
import com.tuki.flipslop.core.ActivityStarts
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Display
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.cover.CoverActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brings the cover launcher up on logical display 0. Picks the strongest start
 * path the app has, then checks that the cover really appeared and falls back
 * to a (full-screen) notification if the system blocked the start.
 */
class CoverLauncher(
    private val context: Context,
    private val runtime: CoverRuntime,
) {
    enum class Reason { FOLD, WAKE, HOME, MANUAL }

    private val nm = context.getSystemService(NotificationManager::class.java)
    private var fallbackJob: Job? = null

    fun show(reason: Reason) {
        if (runtime.coverVisible.value) return
        val caps = runtime.refreshCapabilities()

        val tried = when {
            caps.backgroundStarts -> {
                startDirect(reason)
                true
            }
            caps.overlay -> {
                OverlayAnchor.launchWith(context) { startDirect(reason) }
                true
            }
            else -> false
        }

        fallbackJob?.cancel()
        fallbackJob = runtime.scope.launch {
            if (tried) delay(VERIFY_DELAY_MS)
            if (!runtime.coverVisible.value && runtime.fold.snapshot.value.state.isFolded) {
                postFallback(fullScreen = caps.fullScreenIntent)
            }
        }
    }

    /** Called when the cover is visible or the device opened. */
    fun cancelFallback() {
        fallbackJob?.cancel()
        fallbackJob = null
        nm.cancel(NOTIFICATION_ID)
    }

    fun coverIntent(reason: Reason, stay: Boolean = false): Intent =
        Intent(context, CoverActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(CoverActivity.EXTRA_REASON, reason.name)
            .putExtra(CoverActivity.EXTRA_STAY, stay)

    fun launchOptions(): ActivityOptions =
        ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY)

    fun pendingCoverIntent(reason: Reason, stay: Boolean = false): PendingIntent {
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityStarts.allowBackgroundStart,
            )
        return PendingIntent.getActivity(
            context,
            reason.ordinal,
            coverIntent(reason, stay),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            options.toBundle(),
        )
    }

    private fun startDirect(reason: Reason) {
        runCatching { context.startActivity(coverIntent(reason), launchOptions().toBundle()) }
            .onFailure { Log.w(TAG, "cover start failed", it) }
    }

    private fun postFallback(fullScreen: Boolean) {
        if (!nm.areNotificationsEnabled()) return
        val pi = pendingCoverIntent(Reason.FOLD)
        val notification = Notification.Builder(context, Channels.COVER)
            .setSmallIcon(R.drawable.ic_tile_cover)
            .setContentTitle(context.getString(R.string.fallback_title))
            .setContentText(context.getString(R.string.fallback_text))
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setTimeoutAfter(FALLBACK_TIMEOUT_MS)
            .apply { if (fullScreen) setFullScreenIntent(pi, true) }
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val TAG = "FlipSlop.Launcher"
        const val NOTIFICATION_ID = 2
        const val VERIFY_DELAY_MS = 1200L
        const val FALLBACK_TIMEOUT_MS = 60_000L
    }
}
