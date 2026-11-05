package com.tuki.flipslop.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import com.tuki.flipslop.data.CoverPrefs

/**
 * Hosts app widgets on the cover. A plain install binds through the system
 * confirmation dialog; a priv-app with BIND_APPWIDGET binds silently.
 */
class WidgetRepository(
    private val context: Context,
    private val prefs: CoverPrefs,
) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host = AppWidgetHost(context, HOST_ID)

    private var listeners = 0

    // AppWidgetService refuses every call (IllegalStateException) until the
    // user unlocks after a reboot: the cover just shows no widgets meanwhile.
    fun startListening() {
        if (listeners++ == 0) runCatching { host.startListening() }
    }

    /** The first unlock after a reboot: listening refused while locked can start now. */
    fun onUserUnlocked() {
        if (listeners > 0) runCatching { host.startListening() }
    }

    fun stopListening() {
        if (--listeners <= 0) {
            listeners = 0
            runCatching { host.stopListening() }
        }
    }

    fun providers(): List<AppWidgetProviderInfo> = runCatching {
        manager.getInstalledProvidersForProfile(Process.myUserHandle())
            .filter {
                it.widgetCategory and
                    (AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) != 0
            }
            .sortedBy { it.loadLabel(context.packageManager).lowercase() }
    }.getOrDefault(emptyList())

    fun info(id: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    fun allocate(): Int = host.allocateAppWidgetId()

    fun bindIfAllowed(id: Int, provider: ComponentName): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(id, provider) }.getOrDefault(false)

    fun bindIntent(id: Int, provider: ComponentName): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)

    fun needsConfigure(id: Int): Boolean = info(id)?.configure != null &&
        (info(id)?.widgetFeatures ?: 0) and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL == 0

    fun createView(viewContext: Context, id: Int): AppWidgetHostView? {
        val info = info(id) ?: return null
        return runCatching { host.createView(viewContext, id, info) }.getOrNull()
    }

    suspend fun commit(id: Int) = prefs.addWidget(id)

    /** Frees an id that never made it onto the cover (dialog cancelled, config aborted). */
    fun discard(id: Int) = runCatching { host.deleteAppWidgetId(id) }

    suspend fun remove(id: Int) {
        discard(id)
        prefs.removeWidget(id)
    }

    suspend fun move(id: Int, delta: Int) = prefs.moveWidget(id, delta)

    companion object {
        const val HOST_ID = 0x464C
    }
}
