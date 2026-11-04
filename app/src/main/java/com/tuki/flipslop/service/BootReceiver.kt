package com.tuki.flipslop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tuki.flipslop.coverRuntime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> context.coverRuntime.onBoot()
        }
    }
}
