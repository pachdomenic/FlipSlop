package com.tuki.flipslop.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tuki.flipslop.R
import com.tuki.flipslop.coverRuntime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Quick Settings tile: turns "show on fold" on and off, opens the cover when folded. */
class CoverTileService : TileService() {

    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val runtime = coverRuntime
        job = runtime.scope.launch {
            runtime.settings.collect { render(it.autoShowOnFold) }
        }
    }

    override fun onStopListening() {
        job?.cancel()
        job = null
        super.onStopListening()
    }

    override fun onClick() {
        val runtime = coverRuntime
        val enable = !runtime.settings.value.autoShowOnFold
        runtime.scope.launch { runtime.prefs.setAutoShowOnFold(enable) }
        if (enable && runtime.fold.snapshot.value.state.isFolded) {
            startActivityAndCollapse(runtime.launcher.pendingCoverIntent(CoverLauncher.Reason.MANUAL))
        }
    }

    private fun render(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(if (enabled) R.string.tile_on else R.string.tile_off)
        tile.updateTile()
    }
}
