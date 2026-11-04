package com.tuki.flipslop.notifications

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tuki.flipslop.coverRuntime

/**
 * Notification access unlocks the notification and media panes and DND. As a
 * bonus the system keeps this service bound, which keeps our process (and the
 * fold monitor) alive without a foreground notification.
 */
class CoverNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        val runtime = coverRuntime
        runtime.start()
        runtime.notifications.attach(this)
        refresh()
        runtime.media.onListenerConnected()
        runtime.toggles.refresh()
    }

    override fun onListenerDisconnected() {
        val runtime = coverRuntime
        runtime.notifications.detach()
        runtime.media.onListenerDisconnected()
        requestRebind(component(this))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) = refresh()

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = refresh()

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        coverRuntime.toggles.refresh()
    }

    private fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        coverRuntime.notifications.publish(active, runCatching { currentRanking }.getOrNull())
    }

    companion object {
        fun component(context: Context) = ComponentName(context, CoverNotificationListener::class.java)
    }
}
