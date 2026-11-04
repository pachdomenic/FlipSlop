package com.tuki.flipslop.widgets

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Adds a widget from an activity: allocate an id, bind it (silently when
 * privileged, through the system dialog otherwise), run the widget's
 * configure activity if it has one, then keep it.
 *
 * Create it in onCreate (it registers an activity result launcher) and
 * forward onActivityResult / onSaveInstanceState to it.
 */
class WidgetAdder(
    private val activity: ComponentActivity,
    private val widgets: WidgetRepository,
    savedState: Bundle?,
) {
    /** Widget id waiting for the bind dialog or its configure activity. */
    private var pending = savedState?.getInt(STATE_PENDING, -1) ?: -1

    private val bind = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pending
        if (id < 0) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) configureOrCommit(id) else discard()
    }

    fun add(provider: AppWidgetProviderInfo) {
        val id = widgets.allocate()
        pending = id
        if (widgets.bindIfAllowed(id, provider.provider)) {
            configureOrCommit(id)
        } else {
            try {
                bind.launch(widgets.bindIntent(id, provider.provider))
            } catch (e: ActivityNotFoundException) {
                discard()
            }
        }
    }

    /** Returns true when the result belonged to the configure activity. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        if (resultCode == Activity.RESULT_OK && pending >= 0) commit() else discard()
        return true
    }

    fun onSaveInstanceState(out: Bundle) = out.putInt(STATE_PENDING, pending)

    private fun configureOrCommit(id: Int) {
        if (!widgets.needsConfigure(id)) {
            commit()
            return
        }
        try {
            widgets.host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
        } catch (e: ActivityNotFoundException) {
            commit()
        } catch (e: SecurityException) {
            commit()
        }
    }

    private fun commit() {
        val id = pending
        pending = -1
        if (id >= 0) activity.lifecycleScope.launch { widgets.commit(id) }
    }

    private fun discard() {
        val id = pending
        pending = -1
        if (id >= 0) widgets.discard(id)
    }

    private companion object {
        const val STATE_PENDING = "flipslop_pending_widget"
        const val REQUEST_CONFIGURE = 0x57
    }
}
