package com.tuki.flipslop.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.tuki.flipslop.R
import com.tuki.flipslop.coverRuntime
import com.tuki.flipslop.ui.settings.SettingsActivity

/**
 * Quiet foreground service that keeps the fold monitor alive when notification
 * access (which keeps us bound anyway) has not been granted.
 */
class CoverService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A sticky restart has a null intent and no start() behind it.
        if (intent != null && startsInFlight > 0) startsInFlight--
        val settings = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, Channels.STATUS)
            .setSmallIcon(R.drawable.ic_tile_cover)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(R.string.service_text))
            .setContentIntent(settings)
            .setOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_DEFERRED)
            .build()
        runCatching {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }.onFailure {
            Log.w(TAG, "startForeground failed", it)
            stopSelf()
            return START_NOT_STICKY
        }
        coverRuntime.start()
        if (startsInFlight == 0 && !coverRuntime.keepAliveWanted) {
            // stop() arrived while this start was in flight.
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    companion object {
        private const val TAG = "FlipSlop.Service"
        private const val NOTIFICATION_ID = 1

        /** startForegroundService calls not yet delivered to onStartCommand. Main thread only. */
        private var startsInFlight = 0

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, CoverService::class.java))
                startsInFlight++
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.i(TAG, "not allowed to start the keep-alive service right now", e)
            } catch (e: IllegalStateException) {
                Log.i(TAG, "keep-alive service start refused", e)
            }
        }

        fun stop(context: Context) {
            // Stopping before startForeground kills the whole process, and the listener
            // that connects right after every process start would make that a crash loop.
            // onStartCommand stops the service itself once it is in the foreground.
            if (startsInFlight > 0) return
            context.stopService(Intent(context, CoverService::class.java))
        }
    }
}
