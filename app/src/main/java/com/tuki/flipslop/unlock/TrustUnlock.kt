package com.tuki.flipslop.unlock

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How far fingerprint unlock from the cover lock screen is from working.
 * PROVIDE_TRUST_AGENT itself (privileged build or not) is in CapabilityState.
 */
enum class TrustStatus {
    /** The system has not bound the agent: off under Trust agents, or no privileged build. */
    NOT_CONNECTED,

    /** Bound, but the feature is off in FlipSlop. */
    OFF,

    /** On, waiting for an unlock with the PIN or a fingerprint on the system bouncer. */
    WAITING,

    /** The device can be unlocked by the agent after a fingerprint on the cover. */
    READY,
}

/**
 * The link between the cover and [CoverTrustAgent], which lives in the same
 * process. Android lets a trust agent unlock a locked device only through
 * "Active Unlock": the agent grants renewable trust while the device is
 * unlocked, the system turns that into "trustable" when the screen goes off,
 * and a later renewable grant with DISMISS_KEYGUARD unlocks. The system ends
 * the trustable window after 8 hours without such a grant and 24 hours after
 * the last unlock with the PIN or a biometric on the bouncer, and never
 * allows it after a reboot or lockdown; [ready] mirrors those limits so the
 * cover goes straight to the bouncer when the agent could not unlock.
 */
object TrustUnlock {

    private val _status = MutableStateFlow(TrustStatus.NOT_CONNECTED)
    val status: StateFlow<TrustStatus> = _status.asStateFlow()

    @Volatile
    internal var agent: CoverTrustAgent? = null

    /** Set right before the agent unlocks, so its onDeviceUnlocked knows the unlock was ours. */
    @Volatile
    internal var grantedAt = 0L

    internal fun publish(status: TrustStatus) {
        _status.value = status
    }

    /** True when a fingerprint on the cover can be turned into an unlock right now. */
    val ready: Boolean get() = agent?.canUnlock() == true

    /**
     * Asks the agent to let the keyguard go. Call only after the system's
     * biometric prompt accepted a strong biometric. False if it could not.
     */
    fun unlock(): Boolean {
        val a = agent ?: return false
        grantedAt = SystemClock.elapsedRealtime()
        return a.unlock()
    }
}
