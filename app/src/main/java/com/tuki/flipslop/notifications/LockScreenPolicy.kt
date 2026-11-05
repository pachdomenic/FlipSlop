package com.tuki.flipslop.notifications

import android.app.Notification

/** System lock screen notification switches (Settings > Notifications > Lock screen). */
data class LockScreenPolicy(
    val showNotifications: Boolean = true,
    val showPrivateContent: Boolean = true,
)

/** What a notification may show on the lock screen. */
data class LockLine(
    val item: CoverNotification,
    val title: String,
    val text: String,
    /** The real content (not the redacted version): the card may expand to show all of it. */
    val full: Boolean = true,
)

/**
 * Applies Android's lock screen rules: secret notifications never show,
 * private ones lose their content (falling back to the public version)
 * unless the user allows sensitive content, public ones show as is.
 */
fun CoverNotification.forLockScreen(policy: LockScreenPolicy, hiddenText: String): LockLine? {
    if (!policy.showNotifications) return null
    return when {
        visibility == Notification.VISIBILITY_SECRET -> null
        visibility == Notification.VISIBILITY_PUBLIC || policy.showPrivateContent -> LockLine(this, title, text)
        else -> LockLine(this, publicTitle ?: appLabel, publicText ?: hiddenText, full = false)
    }
}
