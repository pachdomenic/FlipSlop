package com.tuki.flipslop.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon

data class CoverAction(
    val title: String,
    val intent: PendingIntent,
    val remoteInputs: List<RemoteInput>,
) {
    val isReply: Boolean get() = remoteInputs.any { it.allowFreeFormInput }
}

/** One message of a MessagingStyle notification. [sender] is null for the user's own messages. */
data class CoverMessage(val sender: String?, val text: String, val time: Long)

data class NotificationProgress(val value: Int, val max: Int, val indeterminate: Boolean) {
    val fraction: Float get() = if (max > 0) (value.toFloat() / max).coerceIn(0f, 1f) else 0f
}

data class CoverNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    /** Used when [smallIcon] cannot be loaded. */
    val appIcon: Drawable? = null,
    val smallIcon: Icon?,
    val largeIcon: Icon?,
    val color: Int,
    val title: String,
    val text: String,
    val postTime: Long,
    val isClearable: Boolean,
    val contentIntent: PendingIntent?,
    val actions: List<CoverAction>,
    /**
     * Lock screen visibility (Notification.VISIBILITY_*): the stricter of the
     * app's choice and the user's per-channel override.
     */
    val visibility: Int = Notification.VISIBILITY_PRIVATE,
    /** Title / text of the app's public version, shown when content is hidden. */
    val publicTitle: String? = null,
    val publicText: String? = null,

    // What the expanded notification shows (BigText, Inbox, Messaging, BigPicture styles).
    /** The whole text, when the app gave a longer one than [text]. */
    val bigText: String? = null,
    /** Sub text / summary line (account, "3 new"...). */
    val subText: String? = null,
    /** InboxStyle lines. */
    val lines: List<String> = emptyList(),
    /** MessagingStyle conversation, oldest first. */
    val messages: List<CoverMessage> = emptyList(),
    /** BigPictureStyle picture. */
    val picture: Icon? = null,
    val progress: NotificationProgress? = null,
) {
    /** Something to show beyond the title and the one line of [text]. */
    val hasDetails: Boolean
        get() = messages.size > 1 || lines.isNotEmpty() || picture != null || progress != null ||
            subText != null || (bigText != null && bigText != text)
}
