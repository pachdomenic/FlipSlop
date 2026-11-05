package com.tuki.flipslop.unlock

import android.app.KeyguardManager
import android.os.SystemClock
import android.service.trust.TrustAgentService
import android.util.Log
import com.tuki.flipslop.R
import com.tuki.flipslop.coverRuntime
import com.tuki.flipslop.data.FingerprintUnlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * FlipSlop's trust agent ("Active Unlock" style, see [TrustUnlock]). It never
 * decides on its own that the user is present: it only grants trust
 *
 * - while the device is already unlocked, renewable, so that it can unlock
 *   later (the system downgrades this to "trustable" when the screen goes off);
 * - when the cover asks, right after the system's biometric prompt accepted
 *   a strong biometric, to dismiss the keyguard.
 *
 * Bound by the system only when PROVIDE_TRUST_AGENT is granted (privileged
 * build) and the agent is on under Settings > Security > Trust agents, which
 * Android does by default for agents that ship with the system.
 */
class CoverTrustAgent : TrustAgentService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var enabled = false

    /** Last renewable grant the system took while unlocked; 0 when there is none. */
    private var armedAt = 0L

    /** Last unlock with the PIN, pattern or a biometric on the bouncer (refreshes the 24 h limit). */
    private var strongUnlockAt = 0L

    override fun onCreate() {
        super.onCreate()
        TrustUnlock.agent = this
        publish()
        // Not needed when SystemUI takes the fingerprint under the cover itself.
        val systemFingerprint = SystemFingerprint.active(this)
        scope.launch {
            coverRuntime.settings
                .map { it.lock.enabled && it.lock.fingerprint != FingerprintUnlock.OFF && !systemFingerprint }
                .distinctUntilChanged()
                .collect(::setEnabled)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        if (TrustUnlock.agent === this) TrustUnlock.agent = null
        TrustUnlock.publish(TrustStatus.NOT_CONNECTED)
        super.onDestroy()
    }

    private fun setEnabled(on: Boolean) {
        enabled = on
        runCatching { setManagingTrust(on) }.onFailure { Log.w(TAG, "setManagingTrust", it) }
        if (on) {
            // Turned on while unlocked: get ready for the next lock right away.
            if (!getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                strongUnlockAt = SystemClock.elapsedRealtime()
                arm()
            }
        } else {
            runCatching { revokeTrust() }
            armedAt = 0
        }
        publish()
    }

    override fun onDeviceUnlocked() {
        super.onDeviceUnlocked()
        val now = SystemClock.elapsedRealtime()
        // Our own unlock does not reset the system's 24 hour limit; any other unlock does.
        if (now - TrustUnlock.grantedAt > OWN_UNLOCK_WINDOW_MS) strongUnlockAt = now
        if (enabled) arm()
        publish()
    }

    override fun onTrustTimeout() {
        super.onTrustTimeout()
        armedAt = 0
        publish()
    }

    /** Renewable trust while unlocked; the system makes it "trustable" once the screen goes off. */
    @Suppress("DEPRECATION") // The Consumer overload adds nothing we use.
    private fun arm() {
        runCatching {
            grantTrust(getString(R.string.trust_agent_message), TRUST_DURATION_MS, FLAG_GRANT_TRUST_TEMPORARY_AND_RENEWABLE)
            armedAt = SystemClock.elapsedRealtime()
        }.onFailure { Log.w(TAG, "could not arm", it) }
    }

    internal fun canUnlock(): Boolean {
        if (!enabled || armedAt == 0L || strongUnlockAt == 0L) return false
        val now = SystemClock.elapsedRealtime()
        return now - armedAt < IDLE_LIMIT_MS && now - strongUnlockAt < HARD_LIMIT_MS
    }

    @Suppress("DEPRECATION")
    internal fun unlock(): Boolean {
        if (!canUnlock()) return false
        return runCatching {
            grantTrust(
                getString(R.string.trust_agent_message),
                TRUST_DURATION_MS,
                FLAG_GRANT_TRUST_INITIATED_BY_USER or
                    FLAG_GRANT_TRUST_DISMISS_KEYGUARD or
                    FLAG_GRANT_TRUST_TEMPORARY_AND_RENEWABLE,
            )
            armedAt = SystemClock.elapsedRealtime()
        }.onFailure { Log.w(TAG, "could not unlock", it) }.isSuccess
    }

    private fun publish() {
        TrustUnlock.publish(
            when {
                !enabled -> TrustStatus.OFF
                canUnlock() -> TrustStatus.READY
                else -> TrustStatus.WAITING
            },
        )
    }

    private companion object {
        const val TAG = "FlipSlop.Trust"
        const val HOUR_MS = 60 * 60 * 1000L

        /** Our grants expire on their own after this; each unlock renews them. */
        const val TRUST_DURATION_MS = 8 * HOUR_MS

        /** TrustManagerService drops trustable after 8 h without a renewable grant... */
        const val IDLE_LIMIT_MS = 8 * HOUR_MS - 10 * 60 * 1000L

        /** ...and 24 h after the last unlock with the PIN or a biometric on the bouncer. */
        const val HARD_LIMIT_MS = 24 * HOUR_MS - 10 * 60 * 1000L

        /** An unlock this soon after our grant was ours. */
        const val OWN_UNLOCK_WINDOW_MS = 5_000L
    }
}
