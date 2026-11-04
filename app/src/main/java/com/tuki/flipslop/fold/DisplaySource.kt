package com.tuki.flipslop.fold

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Public fold signal: the physical panel currently backing logical display 0.
 * Works for any app, no permission needed.
 */
class DisplaySource(private val context: Context) {

    fun panels(): Flow<PanelSize> = callbackFlow {
        val dm = context.getSystemService(DisplayManager::class.java)

        fun publish() {
            val mode = dm.getDisplay(Display.DEFAULT_DISPLAY)?.mode ?: return
            trySend(PanelSize(mode.physicalWidth, mode.physicalHeight))
        }

        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) publish()
            }
        }
        dm.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        publish()
        awaitClose { dm.unregisterDisplayListener(listener) }
    }.distinctUntilChanged()
}
