package com.tuki.flipslop.notifications

import android.app.ActivityOptions
import com.tuki.flipslop.core.ActivityStarts
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Display
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationsRepository(private val context: Context) {

    private val _items = MutableStateFlow<List<CoverNotification>>(emptyList())
    val items: StateFlow<List<CoverNotification>> = _items.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var listener = WeakReference<CoverNotificationListener>(null)
    private val labelCache = HashMap<String, String>()

    val activeListener: CoverNotificationListener? get() = listener.get()

    internal fun attach(service: CoverNotificationListener) {
        listener = WeakReference(service)
        _connected.value = true
    }

    internal fun detach() {
        listener = WeakReference(null)
        _connected.value = false
        _items.value = emptyList()
    }

    internal fun publish(active: Array<StatusBarNotification>, ranking: NotificationListenerService.RankingMap?) {
        val byGroup = active.groupBy { it.groupKey }
        val r = NotificationListenerService.Ranking()
        _items.value = active
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { !isMedia(it.notification) }
            .filter { sbn ->
                // Drop a group summary when its children are present.
                val summary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
                !(summary && (byGroup[sbn.groupKey]?.size ?: 0) > 1)
            }
            .filter { sbn ->
                ranking == null || !ranking.getRanking(sbn.key, r) || !r.isAmbient
            }
            .mapNotNull(::toCover)
            .sortedByDescending { it.postTime }
            .toList()
    }

    fun dismiss(key: String) {
        activeListener?.cancelNotification(key)
    }

    fun dismissAll() {
        val l = activeListener ?: return
        _items.value.filter { it.isClearable }.forEach { l.cancelNotification(it.key) }
    }

    /** Opens the notification's content on the display the cover is on. */
    fun open(item: CoverNotification): Boolean {
        val pi = item.contentIntent ?: return false
        val ok = send(pi, null)
        if (ok) dismissIfAutoCancel(item)
        return ok
    }

    fun runAction(action: CoverAction): Boolean = send(action.intent, null)

    fun reply(action: CoverAction, text: String): Boolean {
        val input = action.remoteInputs.firstOrNull { it.allowFreeFormInput } ?: return false
        val fillIn = Intent()
        val results = Bundle().apply { putCharSequence(input.resultKey, text) }
        RemoteInput.addResultsToIntent(arrayOf(input), fillIn, results)
        return send(action.intent, fillIn)
    }

    private fun send(pi: PendingIntent, fillIn: Intent?): Boolean {
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .setPendingIntentBackgroundActivityStartMode(
                ActivityStarts.allowBackgroundStart,
            )
        return runCatching {
            pi.send(context, 0, fillIn, null, null, null, options.toBundle())
        }.onFailure { Log.w(TAG, "send failed", it) }.isSuccess
    }

    private fun dismissIfAutoCancel(item: CoverNotification) {
        val sbn = activeListener?.activeNotifications?.firstOrNull { it.key == item.key } ?: return
        if (sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) dismiss(item.key)
    }

    private fun toCover(sbn: StatusBarNotification): CoverNotification? {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
        val text = lastMessage(extras)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        if (title.isNullOrBlank() && text.isNullOrBlank()) return null

        return CoverNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = label(sbn.packageName),
            smallIcon = n.smallIcon,
            largeIcon = n.getLargeIcon(),
            color = n.color,
            title = title?.toString().orEmpty(),
            text = text?.toString().orEmpty(),
            postTime = sbn.postTime,
            isClearable = sbn.isClearable,
            contentIntent = n.contentIntent,
            actions = n.actions.orEmpty().mapNotNull { a ->
                val pi = a.actionIntent ?: return@mapNotNull null
                CoverAction(a.title?.toString().orEmpty(), pi, a.remoteInputs.orEmpty().toList())
            },
        )
    }

    private fun lastMessage(extras: Bundle): CharSequence? {
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)
        val last = messages?.lastOrNull() ?: return null
        val sender = last.getCharSequence("sender")
        val body = last.getCharSequence("text") ?: return null
        return if (sender.isNullOrBlank()) body else "$sender: $body"
    }

    private fun isMedia(n: Notification): Boolean {
        val template = n.extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        return template.endsWith("MediaStyle") || n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
    }

    private fun label(pkg: String): String = labelCache.getOrPut(pkg) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0)))
                .toString()
        }.getOrDefault(pkg)
    }

    private companion object {
        const val TAG = "FlipSlop.Notifications"
    }
}
