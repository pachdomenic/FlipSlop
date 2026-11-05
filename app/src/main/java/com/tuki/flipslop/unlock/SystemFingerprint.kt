package com.tuki.flipslop.unlock

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG

/**
 * SystemUI's own fingerprint unlock under the cover, with no prompt.
 *
 * While an app occludes the keyguard, SystemUI stops listening to the
 * fingerprint sensor, except when the occluding app's package is in its
 * config_fingerprint_listen_on_occluding_activity_packages
 * (KeyguardUpdateMonitor). The system build adds FlipSlop to that list with a
 * static overlay (lineage/overlay/FlipSlopSystemUIOverlay). A touch on the
 * sensor then unlocks the same way it does on the system lock screen: the
 * keyguard goes away under the cover and ACTION_USER_PRESENT follows. The
 * usual Android rules still apply: the PIN after a reboot, lockout, and
 * "Touch to unlock anytime" for side sensors.
 *
 * When SystemUI listens, FlipSlop shows no biometric prompt and its trust
 * agent stays out of the way. Without the overlay, it falls back to the prompt
 * and the trust agent (see [TrustUnlock]).
 */
object SystemFingerprint {

    private const val SYSTEMUI = "com.android.systemui"
    private const val CONFIG = "config_fingerprint_listen_on_occluding_activity_packages"

    @Volatile
    private var cached: Boolean? = null

    /** SystemUI takes the fingerprint while FlipSlop covers the keyguard. Overlays change only on reboot. */
    fun active(context: Context): Boolean = cached ?: read(context)?.also { cached = it } ?: false

    /** A strong biometric is enrolled and usable. */
    fun enrolled(context: Context): Boolean =
        context.getSystemService(BiometricManager::class.java)?.canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    // SystemUI's resource is only reachable by name.
    @SuppressLint("DiscouragedApi")
    private fun read(context: Context): Boolean? = runCatching {
        val res = context.packageManager.getResourcesForApplication(SYSTEMUI)
        val id = res.getIdentifier(CONFIG, "array", SYSTEMUI)
        id != 0 && context.packageName in res.getStringArray(id)
    }.getOrNull()
}
