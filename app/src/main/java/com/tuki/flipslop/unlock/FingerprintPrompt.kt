package com.tuki.flipslop.unlock

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import com.tuki.flipslop.R

/**
 * The system's biometric prompt on the cover, for a strong biometric only.
 * The fingerprint itself is matched by the system, never by FlipSlop.
 */
class FingerprintPrompt(private val activity: Activity) {

    private var signal: CancellationSignal? = null

    val showing: Boolean get() = signal != null

    /** A strong biometric is enrolled and usable. */
    fun available(): Boolean =
        activity.getSystemService(BiometricManager::class.java)?.canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the prompt. [onSuccess] after a match; [onUseCredential] when the
     * user picks the PIN instead or the sensor is locked out (the bouncer
     * takes over). Closing the prompt otherwise just leaves the lock screen.
     */
    fun show(onSuccess: () -> Unit, onUseCredential: () -> Unit) {
        if (signal != null) return
        val cancel = CancellationSignal()
        signal = cancel
        val executor = activity.mainExecutor
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.fingerprint_prompt_title))
            .setSubtitle(activity.getString(R.string.fingerprint_prompt_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .setNegativeButton(activity.getString(R.string.fingerprint_use_pin), executor) { _, _ ->
                signal = null
                onUseCredential()
            }
            .build()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                signal = null
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                signal = null
                when (errorCode) {
                    BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT,
                    BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT,
                    -> onUseCredential()
                    // Canceled, timed out, dismissed: stay on the lock screen.
                    else -> Unit
                }
            }
        }
        runCatching { prompt.authenticate(cancel, executor, callback) }
            .onFailure {
                signal = null
                onUseCredential()
            }
    }

    fun dismiss() {
        signal?.cancel()
        signal = null
    }
}
