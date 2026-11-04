package com.tuki.flipslop.ui.settings

import android.Manifest
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
import com.tuki.flipslop.R
import com.tuki.flipslop.coverRuntime
import com.tuki.flipslop.notifications.CoverNotificationListener
import com.tuki.flipslop.service.CoverLauncher
import com.tuki.flipslop.ui.theme.FlipSlopTheme
import com.tuki.flipslop.widgets.WidgetAdder

class SettingsActivity : ComponentActivity() {

    private val runtime by lazy { coverRuntime }

    private lateinit var widgetAdder: WidgetAdder

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { runtime.refreshCapabilities() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetAdder = WidgetAdder(this, runtime.widgets, savedInstanceState)
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
        widgetAdder.onSaveInstanceState(outState)
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
        widgetAdder.onActivityResult(requestCode, resultCode)
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

        override fun addWidget(provider: AppWidgetProviderInfo) = widgetAdder.add(provider)

        override fun finish() = this@SettingsActivity.finish()
    }

    companion object {
        const val EXTRA_ROUTE = "com.tuki.flipslop.extra.ROUTE"
        const val ROUTE_WIDGETS = "widgets"
        const val ROUTE_APPS = "apps"
        const val ROUTE_PAGES = "pages"
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
