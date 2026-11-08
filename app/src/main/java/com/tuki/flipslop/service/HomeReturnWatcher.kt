package com.tuki.flipslop.service

import android.content.Context
import com.tuki.flipslop.core.ForegroundApp
import com.tuki.flipslop.core.HomeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * On the cover the navigation bar's Home goes to the regular launcher (there is
 * one HOME for both panels). While folded we watch usage events and bring the
 * cover back when the HOME app resumes. Needs usage access, which a priv-app
 * build gets for free.
 */
class HomeReturnWatcher(
    private val context: Context,
    private val runtime: CoverRuntime,
) {
    private var job: Job? = null

    fun setActive(active: Boolean) {
        if (active && job == null) {
            job = runtime.scope.launch { watch() }
        } else if (!active) {
            job?.cancel()
            job = null
        }
    }

    private suspend fun watch() {
        var since = System.currentTimeMillis()
        while (kotlin.coroutines.coroutineContext.isActive) {
            delay(POLL_MS)
            val now = System.currentTimeMillis()
            val home = HomeApp.packageName(context) ?: continue
            val homeOnTop = withContext(Dispatchers.Default) {
                ForegroundApp.lastResumed(context, since, now) == home
            }
            since = now
            if (homeOnTop) runtime.launcher.show(CoverLauncher.Reason.HOME)
        }
    }

    private companion object {
        const val POLL_MS = 1000L
    }
}
