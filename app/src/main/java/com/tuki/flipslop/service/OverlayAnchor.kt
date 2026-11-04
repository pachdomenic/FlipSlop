package com.tuki.flipslop.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Since Android 15 holding SYSTEM_ALERT_WINDOW only exempts an app from the
 * background activity start limits while it shows a visible overlay window.
 * We add a 1px transparent, untouchable overlay, start the activity once it has
 * been laid out, and take the overlay down again.
 */
object OverlayAnchor {
    private const val START_DELAY_MS = 48L
    private const val REMOVE_DELAY_MS = 1500L

    fun launchWith(context: Context, start: () -> Unit) {
        val wm = context.getSystemService(WindowManager::class.java)
        val view = View(context)
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "FlipSlopAnchor"
        }

        val added = runCatching { wm.addView(view, params) }.isSuccess
        if (!added) {
            start()
            return
        }
        view.postDelayed({
            runCatching(start)
            view.postDelayed({ runCatching { wm.removeViewImmediate(view) } }, REMOVE_DELAY_MS)
        }, START_DELAY_MS)
    }
}
