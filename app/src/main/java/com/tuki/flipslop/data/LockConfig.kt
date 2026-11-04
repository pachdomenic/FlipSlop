package com.tuki.flipslop.data

/** What the lock screen is built around. */
enum class LockLayout { CARDS, BIG_CLOCK }

/** Notifications on the lock screen's main page, always within Android's lock screen rules. */
enum class LockNotifications { HIDDEN, ICONS, CARDS }

/** How the bouncer (or the fingerprint prompt) is brought up from the main page. */
enum class UnlockGesture { TAP_OR_SWIPE, SWIPE, DOUBLE_TAP }

/**
 * Fingerprint straight from the cover lock screen. Needs the privileged
 * build: the fingerprint is checked by the system's biometric prompt and
 * FlipSlop's trust agent then lets the keyguard go (see unlock/).
 */
enum class FingerprintUnlock { OFF, ON_GESTURE, IMMEDIATE }

/** Pages that may be swiped to while locked, next to the lock screen itself. */
val LockTabCandidates = listOf(CoverPage.NOTIFICATIONS, CoverPage.MEDIA, CoverPage.TOGGLES)

data class LockConfig(
    /** Our own lock screen; off shows the whole cover over the keyguard. */
    val enabled: Boolean = true,
    val layout: LockLayout = LockLayout.CARDS,
    val notifications: LockNotifications = LockNotifications.HIDDEN,
    val media: Boolean = true,
    /** Extra pages next to the lock screen, from [LockTabCandidates]. */
    val tabs: Set<CoverPage> = emptySet(),
    val gesture: UnlockGesture = UnlockGesture.TAP_OR_SWIPE,
    /** Lock icon and "how to unlock" text beside the cameras. */
    val hint: Boolean = true,
    val fingerprint: FingerprintUnlock = FingerprintUnlock.IMMEDIATE,
) {
    /** The pages the locked cover can swipe through, in the order of the unlocked ones. */
    fun pages(order: List<CoverPage>): List<CoverPage> {
        val shown = order.filter { it == CoverPage.HOME || it in tabs }
        return if (CoverPage.HOME in shown) shown else listOf(CoverPage.HOME) + shown
    }
}
