package com.tuki.flipslop.notifications

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockScreenPolicyTest {

    private fun item(visibility: Int, publicTitle: String? = null, publicText: String? = null) = CoverNotification(
        key = "k",
        packageName = "app",
        appLabel = "Messages",
        smallIcon = null,
        largeIcon = null,
        color = 0,
        title = "Alex",
        text = "Are we still on for tonight?",
        postTime = 0,
        isClearable = true,
        contentIntent = null,
        actions = emptyList(),
        visibility = visibility,
        publicTitle = publicTitle,
        publicText = publicText,
    )

    private val open = LockScreenPolicy(showNotifications = true, showPrivateContent = true)
    private val redacting = LockScreenPolicy(showNotifications = true, showPrivateContent = false)

    @Test
    fun `secret notifications never show`() {
        assertNull(item(Notification.VISIBILITY_SECRET).forLockScreen(open, "hidden"))
    }

    @Test
    fun `nothing shows when the system hides lock screen notifications`() {
        val off = LockScreenPolicy(showNotifications = false)
        assertNull(item(Notification.VISIBILITY_PUBLIC).forLockScreen(off, "hidden"))
    }

    @Test
    fun `private content shows when sensitive content is allowed`() {
        val line = item(Notification.VISIBILITY_PRIVATE).forLockScreen(open, "hidden")!!
        assertEquals("Alex", line.title)
        assertEquals("Are we still on for tonight?", line.text)
    }

    @Test
    fun `private content is redacted otherwise`() {
        val line = item(Notification.VISIBILITY_PRIVATE).forLockScreen(redacting, "hidden")!!
        assertEquals("Messages", line.title)
        assertEquals("hidden", line.text)
    }

    @Test
    fun `redaction uses the public version when the app gives one`() {
        val line = item(Notification.VISIBILITY_PRIVATE, "1 new message", "Tap to view")
            .forLockScreen(redacting, "hidden")!!
        assertEquals("1 new message", line.title)
        assertEquals("Tap to view", line.text)
    }

    @Test
    fun `public notifications are never redacted`() {
        val line = item(Notification.VISIBILITY_PUBLIC).forLockScreen(redacting, "hidden")!!
        assertEquals("Alex", line.title)
    }
}
