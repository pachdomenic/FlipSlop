package com.tuki.flipslop.notifications

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
)
