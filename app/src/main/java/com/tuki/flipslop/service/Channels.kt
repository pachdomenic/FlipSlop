package com.tuki.flipslop.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.tuki.flipslop.R

object Channels {
    const val STATUS = "status"
    const val COVER = "cover"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(
                    STATUS,
                    context.getString(R.string.channel_status),
                    NotificationManager.IMPORTANCE_MIN,
                ).apply {
                    description = context.getString(R.string.channel_status_desc)
                    setShowBadge(false)
                },
                NotificationChannel(
                    COVER,
                    context.getString(R.string.channel_cover),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.channel_cover_desc)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                },
            ),
        )
    }
}
