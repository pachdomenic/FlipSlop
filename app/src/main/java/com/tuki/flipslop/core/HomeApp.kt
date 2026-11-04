package com.tuki.flipslop.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** The user's default HOME app (Trebuchet on LineageOS). */
object HomeApp {
    fun packageName(context: Context): String? = context.packageManager
        .resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
}
