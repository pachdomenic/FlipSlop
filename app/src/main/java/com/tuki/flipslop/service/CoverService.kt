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
        return START_STICKY
    }

    companion object {
        private const val TAG = "FlipSlop.Service"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, CoverService::class.java))
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.i(TAG, "not allowed to start the keep-alive service right now", e)
            } catch (e: IllegalStateException) {
                Log.i(TAG, "keep-alive service start refused", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CoverService::class.java))
        }
    }
}
