package com.tuki.flipslop.ui.settings

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.tuki.flipslop.R
import com.tuki.flipslop.coverRuntime
import com.tuki.flipslop.notifications.CoverNotificationListener
import com.tuki.flipslop.service.CoverLauncher
import com.tuki.flipslop.ui.theme.FlipSlopTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private val runtime by lazy { coverRuntime }

    /** Widget id waiting for the bind dialog or its configure activity. */
    private var pendingWidgetId = -1

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { runtime.refreshCapabilities() }

    private val bindWidget =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val id = pendingWidgetId
            if (id < 0) return@registerForActivityResult
            if (result.resultCode == Activity.RESULT_OK) configureOrCommit(id) else discardPending()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingWidgetId = savedInstanceState?.getInt(STATE_PENDING_WIDGET, -1) ?: -1
        enableEdgeToEdge()
        val initial = intent.getStringExtra(EXTRA_ROUTE)
        setContent {
            FlipSlopTheme {
                SettingsApp(
                    runtime = runtime,
                    initialRoute = initial,
                    host = host,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PENDING_WIDGET, pendingWidgetId)
    }

    override fun onStart() {
        super.onStart()
        runtime.widgets.startListening()
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a system settings page: re-read what was granted.
        runtime.refreshCapabilities()
        runtime.ensureKeepAlive()
    }

    override fun onStop() {
        runtime.widgets.stopListening()
        super.onStop()
    }

    // AppWidgetHost only offers the startActivityForResult based configure flow.
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            if (resultCode == Activity.RESULT_OK && pendingWidgetId >= 0) commitPending() else discardPending()
        }
    }

    private fun configureOrCommit(id: Int) {
        if (!runtime.widgets.needsConfigure(id)) {
            commitPending()
            return
        }
        try {
            runtime.widgets.host.startAppWidgetConfigureActivityForResult(
                this,
                id,
                0,
                REQUEST_CONFIGURE_WIDGET,
                null,
            )
        } catch (e: ActivityNotFoundException) {
            commitPending()
        } catch (e: SecurityException) {
            commitPending()
        }
    }

    private fun commitPending() {
        val id = pendingWidgetId
        pendingWidgetId = -1
        if (id >= 0) lifecycleScope.launch { runtime.widgets.commit(id) }
    }

    private fun discardPending() {
        val id = pendingWidgetId
        pendingWidgetId = -1
        if (id >= 0) runtime.widgets.discard(id)
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private val packageUri: Uri get() = Uri.fromParts("package", packageName, null)

    private val host = object : SettingsHost {
        override fun grantOverlay() = open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri))

        override fun grantNotificationAccess() = open(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                CoverNotificationListener.component(this@SettingsActivity).flattenToString(),
            ),
        )

        override fun grantFullScreenIntent() =
            open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))

        override fun grantUsageAccess() = open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri))

        override fun grantWriteSettings() = open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, packageUri))

        override fun grantPostNotifications() = notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        override fun openCoverPreview() {
            val launcher = runtime.launcher
            startActivity(
                launcher.coverIntent(CoverLauncher.Reason.MANUAL, stay = true),
                launcher.launchOptions().setLaunchDisplayId(display.displayId).toBundle(),
            )
        }

        override fun addWidget(provider: AppWidgetProviderInfo) {
            val widgets = runtime.widgets
            val id = widgets.allocate()
            pendingWidgetId = id
            if (widgets.bindIfAllowed(id, provider.provider)) {
                configureOrCommit(id)
            } else {
                try {
                    bindWidget.launch(widgets.bindIntent(id, provider.provider))
                } catch (e: ActivityNotFoundException) {
                    discardPending()
                }
            }
        }

        override fun finish() = this@SettingsActivity.finish()
    }

    companion object {
        const val EXTRA_ROUTE = "com.tuki.flipslop.extra.ROUTE"
        const val ROUTE_WIDGETS = "widgets"
        const val ROUTE_APPS = "apps"
        const val ROUTE_PAGES = "pages"
        private const val STATE_PENDING_WIDGET = "pending_widget"
        private const val REQUEST_CONFIGURE_WIDGET = 0x57
    }
}

/** What the settings UI asks of its activity. */
interface SettingsHost {
    fun grantOverlay()
    fun grantNotificationAccess()
    fun grantFullScreenIntent()
    fun grantUsageAccess()
    fun grantWriteSettings()
    fun grantPostNotifications()
    fun openCoverPreview()
    fun addWidget(provider: AppWidgetProviderInfo)
    fun finish()
}
