package com.tuki.flipslop.core

import android.content.Context
import android.provider.Settings

/** The system's "Continue using apps on fold" choice, and what it means for the cover. */
object FoldContinuation {
    // Settings.System.FOLD_LOCK_BEHAVIOR: @hide, but @Readable for every app.
    private const val SETTING = "fold_lock_behavior_setting"
    private const val VALUE_ALWAYS = "stay_awake_on_fold_key"

    /** True when the user picked "Always". */
    fun always(context: Context): Boolean = runCatching {
        Settings.System.getString(context.contentResolver, SETTING) == VALUE_ALWAYS
    }.getOrDefault(false)

    /** With "Always" set: should [foreground] stay on screen instead of the cover coming up? */
    fun keepsApp(keyguardLocked: Boolean, foreground: String?, home: String?, self: String): Boolean =
        !keyguardLocked && foreground != null && foreground != home && foreground != self
}
