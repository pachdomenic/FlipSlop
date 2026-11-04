package com.tuki.flipslop.media

import android.app.ActivityOptions
import com.tuki.flipslop.core.ActivityStarts
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import com.tuki.flipslop.notifications.CoverNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaState(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val positionUpdatedAt: Long,
    val speed: Float,
    val durationMs: Long,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
    val canSeek: Boolean,
) {
    /** Extrapolated playback position at [now] (elapsedRealtime). */
    fun positionAt(now: Long): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((now - positionUpdatedAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/** Active media session, read through the notification listener's grant. */
class MediaRepository(private val context: Context) {

    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<MediaState?>(null)
    val state: StateFlow<MediaState?> = _state.asStateFlow()

    private var controller: MediaController? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        onSessions(list.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() {
            bind(null)
            refreshSessions()
        }
    }

    fun onListenerConnected() {
        runCatching {
            msm.addOnActiveSessionsChangedListener(
                sessionsListener,
                CoverNotificationListener.component(context),
                handler,
            )
        }.onFailure { Log.w(TAG, "no media session access", it) }
        refreshSessions()
    }

    fun onListenerDisconnected() {
        runCatching { msm.removeOnActiveSessionsChangedListener(sessionsListener) }
        bind(null)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()
    fun seekTo(ms: Long) = controller?.transportControls?.seekTo(ms)

    /**
     * Opens the player that owns the session: its session activity when it
     * set one, otherwise the app's launcher activity (plenty of local file
     * players never set a session activity).
     */
    fun openApp(): Boolean {
        val c = controller ?: return false
        c.sessionActivity?.let { pi ->
            val options = ActivityOptions.makeBasic()
                .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
                .setPendingIntentBackgroundActivityStartMode(ActivityStarts.allowBackgroundStart)
            val sent = runCatching { pi.send(context, 0, null, null, null, null, options.toBundle()) }
                .onFailure { Log.w(TAG, "session activity failed, falling back to the app", it) }
                .isSuccess
            if (sent) return true
        }
        val launch = context.packageManager.getLaunchIntentForPackage(c.packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY)
        return runCatching { context.startActivity(launch, options.toBundle()) }
            .onFailure { Log.w(TAG, "could not open ${c.packageName}", it) }
            .isSuccess
    }

    private fun refreshSessions() {
        val list = runCatching {
            msm.getActiveSessions(CoverNotificationListener.component(context))
        }.getOrDefault(emptyList())
        onSessions(list)
    }

    private fun onSessions(list: List<MediaController>) {
        val pick = list.firstOrNull { it.playbackState?.isActive == true } ?: list.firstOrNull()
        if (pick?.sessionToken != controller?.sessionToken) bind(pick) else publish()
    }

    private fun bind(next: MediaController?) {
        controller?.unregisterCallback(controllerCallback)
        controller = next
        next?.registerCallback(controllerCallback, handler)
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        if (c == null || meta == null) {
            _state.value = null
            return
        }
        val pb = c.playbackState
        val actions = pb?.actions ?: 0L
        _state.value = MediaState(
            packageName = c.packageName,
            appLabel = label(c.packageName),
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty(),
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            isPlaying = pb?.state == PlaybackState.STATE_PLAYING,
            positionMs = pb?.position ?: 0L,
            positionUpdatedAt = pb?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = pb?.playbackSpeed ?: 1f,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
        )
    }

    private fun label(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))).toString()
    }.getOrDefault(pkg)

    private companion object {
        const val TAG = "FlipSlop.Media"
    }
}
