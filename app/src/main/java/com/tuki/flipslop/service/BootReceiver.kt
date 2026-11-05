package com.tuki.flipslop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tuki.flipslop.coverRuntime

/**
 * LOCKED_BOOT_COMPLETED brings the cover up right after a reboot, before the
 * first unlock (the app is directBootAware); BOOT_COMPLETED follows once the
 * user unlocks. onBoot is safe to run for both.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> context.coverRuntime.onBoot()
        }
    }
}
