package com.tuki.flipslop.core

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** What the app is allowed to do, from plain APK up to a priv-app in the ROM. */
enum class Tier { BASIC, GRANTED, PRIVILEGED }

data class CapabilityState(
    /** SYSTEM_ALERT_WINDOW: start the cover from the background (with an overlay anchor). */
    val overlay: Boolean = false,
    /** Notification listener: notifications, media sessions, DND, keeps the process alive. */
    val notificationAccess: Boolean = false,
    /** USE_FULL_SCREEN_INTENT: show the cover over the lock screen when the screen is off. */
    val fullScreenIntent: Boolean = false,
    /** Usage access: notice the Home button on the cover and come back. */
    val usageAccess: Boolean = false,
    /** WRITE_SETTINGS: system brightness instead of window brightness. */
    val writeSettings: Boolean = false,
    val postNotifications: Boolean = false,
    /** Privileged START_ACTIVITIES_FROM_BACKGROUND. */
    val backgroundStarts: Boolean = false,
    /** Privileged BIND_APPWIDGET: add widgets without the confirmation dialog. */
    val bindWidgets: Boolean = false,
    /** Hidden DeviceStateManager reachable (system build with hidden API allowlist). */
    val deviceStateApi: Boolean = false,
    /** The launcher's themed-icons switch is readable (priv-app build). */
    val systemIconTheme: Boolean = false,
    val isSystemApp: Boolean = false,
) {
    val tier: Tier
        get() = when {
            backgroundStarts -> Tier.PRIVILEGED
            overlay || notificationAccess || fullScreenIntent -> Tier.GRANTED
            else -> Tier.BASIC
        }

    /** Can the cover come up on its own when the device folds? */
    val canAutoShow: Boolean get() = backgroundStarts || overlay || fullScreenIntent
}

object Capabilities {

    fun read(context: Context, deviceStateApi: Boolean, systemIconTheme: Boolean): CapabilityState {
        val pkg = context.packageName
        val nm = context.getSystemService(NotificationManager::class.java)
        return CapabilityState(
            overlay = Settings.canDrawOverlays(context),
            notificationAccess = pkg in NotificationManagerCompat.getEnabledListenerPackages(context),
            fullScreenIntent = runCatching { nm.canUseFullScreenIntent() }.getOrDefault(false),
            usageAccess = hasUsageAccess(context),
            writeSettings = Settings.System.canWrite(context),
            postNotifications = granted(context, Manifest.permission.POST_NOTIFICATIONS),
            backgroundStarts = granted(context, PERMISSION_BACKGROUND_STARTS),
            bindWidgets = granted(context, Manifest.permission.BIND_APPWIDGET),
            deviceStateApi = deviceStateApi,
            systemIconTheme = systemIconTheme,
            isSystemApp = (context.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
        )
    }

    private fun granted(context: Context, permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        // The replacement (checkOpNoThrow) is the same call; this keeps API 35 working.
        @Suppress("DEPRECATION")
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT -> granted(context, Manifest.permission.PACKAGE_USAGE_STATS)
            else -> false
        }
    }


    const val PERMISSION_BACKGROUND_STARTS = "android.permission.START_ACTIVITIES_FROM_BACKGROUND"
}
