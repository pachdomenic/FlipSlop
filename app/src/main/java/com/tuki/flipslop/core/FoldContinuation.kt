package com.tuki.flipslop.core

import android.content.Context
import android.provider.Settings

/** The options of the system's "Continue using apps on fold" screen. */
enum class FoldBehavior { ALWAYS, SWIPE_UP, NEVER }

object FoldContinuation {
    // Settings.System.FOLD_LOCK_BEHAVIOR: @hide, but @Readable for every app.
    private const val SETTING = "fold_lock_behavior_setting"
    private const val VALUE_ALWAYS = "stay_awake_on_fold_key"
    private const val VALUE_NEVER = "sleep_on_fold_key"

    /** The user's choice, or null on a device without this option. */
    fun behavior(context: Context): FoldBehavior? {
        if (!SystemConfig.bool("config_fold_lock_behavior")) return null
        val value = runCatching {
            Settings.System.getString(context.contentResolver, SETTING)
        }.getOrElse { return null }
        return parse(value)
    }

    /** Unset or unknown values mean swipe up, the same default as the framework's. */
    fun parse(value: String?): FoldBehavior = when (value) {
        VALUE_ALWAYS -> FoldBehavior.ALWAYS
        VALUE_NEVER -> FoldBehavior.NEVER
        else -> FoldBehavior.SWIPE_UP
    }

    /** Should folding bring up the cover, given what the system is about to do? */
    fun showsCover(
        behavior: FoldBehavior?,
        keyguardLocked: Boolean,
        foreground: String?,
        home: String?,
        self: String,
    ): Boolean = when (behavior) {
        null -> true
        FoldBehavior.NEVER -> false
        FoldBehavior.ALWAYS, FoldBehavior.SWIPE_UP ->
            keyguardLocked || foreground == null || foreground == home || foreground == self
    }
}
