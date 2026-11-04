package com.tuki.flipslop.core

import android.app.ActivityOptions
import android.os.Build

object ActivityStarts {
    /** "Let this start go through even though we are in the background." */
    val allowBackgroundStart: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            @Suppress("DEPRECATION")
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
}
