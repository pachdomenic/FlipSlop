package com.tuki.flipslop.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

object ForegroundApp {
    /** Package of the last activity resumed between the two times; null if none or no usage access. */
    fun lastResumed(context: Context, from: Long, to: Long): String? {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val events = runCatching { usm.queryEvents(from, to) }.getOrNull() ?: return null
        var last: String? = null
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) last = e.packageName
        }
        return last
    }
}
