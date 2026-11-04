package com.tuki.flipslop.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.core.DeviceProfile
import com.tuki.flipslop.fold.PanelSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.coverDataStore: DataStore<Preferences> by preferencesDataStore(name = "cover")

enum class CoverPage { HOME, NOTIFICATIONS, MEDIA, APPS, WIDGETS, TOGGLES }

/** Notifications sit left of Home, so a swipe right from the clock shows them. */
val DefaultPages = listOf(
    CoverPage.NOTIFICATIONS,
    CoverPage.HOME,
    CoverPage.MEDIA,
    CoverPage.APPS,
    CoverPage.WIDGETS,
    CoverPage.TOGGLES,
)

enum class ClockStyle { STACKED, INLINE }

data class CoverSettings(
    val autoShowOnFold: Boolean = true,
    val showOnWake: Boolean = true,
    val returnAfterHome: Boolean = true,
    val pages: List<CoverPage> = DefaultPages,
    val pinnedApps: List<String> = emptyList(),
    val pinsInitialized: Boolean = false,
    val widgetIds: List<Int> = emptyList(),
    val bottomInsetDp: Int = DeviceProfile.current.coverBottomInsetDp,
    val clockStyle: ClockStyle = ClockStyle.STACKED,
    /** AOSP lock screen style clock while there are no notifications and nothing plays. */
    val idleAospClock: Boolean = true,
    /** Album art fills the media page background and the now playing pill. */
    val mediaArtBackground: Boolean = true,
    val cameraSide: CameraSide = DeviceProfile.current.coverCameraSide,
    val cameraWidthDp: Int = DeviceProfile.current.coverCameraWidthDp,
    /** Page bar stays out; otherwise it hides behind page dots until tapped. */
    val pageBarAlways: Boolean = false,
    /** Page dots in the dock; off hides page navigation completely (swipe only). */
    val showPageDots: Boolean = true,
    /** What lives in the strip next to the camera block. */
    val dockNotifications: Boolean = true,
    val dockMedia: Boolean = true,
)

class CoverPrefs(context: Context) {
    private val store = context.applicationContext.coverDataStore

    val settings: Flow<CoverSettings> = store.data.map { p ->
        CoverSettings(
            autoShowOnFold = p[AUTO_SHOW] ?: true,
            showOnWake = p[SHOW_ON_WAKE] ?: true,
            returnAfterHome = p[RETURN_AFTER_HOME] ?: true,
            pages = p[PAGES]?.let(::decodePages) ?: DefaultPages,
            pinnedApps = p[PINNED]?.split(LIST_SEP)?.filter { it.isNotBlank() } ?: emptyList(),
            pinsInitialized = p[PINS_INIT] ?: false,
            widgetIds = p[WIDGETS]?.split(LIST_SEP)?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
            bottomInsetDp = p[BOTTOM_INSET] ?: DeviceProfile.current.coverBottomInsetDp,
            clockStyle = p[CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() }
                ?: ClockStyle.STACKED,
            idleAospClock = p[IDLE_AOSP_CLOCK] ?: true,
            mediaArtBackground = p[MEDIA_ART_BACKGROUND] ?: true,
            cameraSide = p[CAMERA_SIDE]?.let { runCatching { CameraSide.valueOf(it) }.getOrNull() }
                ?: DeviceProfile.current.coverCameraSide,
            cameraWidthDp = p[CAMERA_WIDTH] ?: DeviceProfile.current.coverCameraWidthDp,
            pageBarAlways = p[PAGE_BAR_ALWAYS] ?: false,
            showPageDots = p[SHOW_PAGE_DOTS] ?: true,
            dockNotifications = p[DOCK_NOTIFICATIONS] ?: true,
            dockMedia = p[DOCK_MEDIA] ?: true,
        )
    }.distinctUntilChanged()

    val knownPanels: Flow<Set<PanelSize>> = store.data
        .map { p -> p[KNOWN_PANELS].orEmpty().mapNotNull(PanelSize::parse).toSet() }
        .distinctUntilChanged()

    suspend fun rememberPanel(panel: PanelSize) {
        store.edit { p ->
            val current = p[KNOWN_PANELS].orEmpty()
            val value = panel.toString()
            if (value !in current) p[KNOWN_PANELS] = (current + value).toList().takeLast(MAX_PANELS).toSet()
        }
    }

    suspend fun forgetPanels() = store.edit { it.remove(KNOWN_PANELS) }

    suspend fun setAutoShowOnFold(value: Boolean) = store.edit { it[AUTO_SHOW] = value }
    suspend fun setShowOnWake(value: Boolean) = store.edit { it[SHOW_ON_WAKE] = value }
    suspend fun setReturnAfterHome(value: Boolean) = store.edit { it[RETURN_AFTER_HOME] = value }
    suspend fun setBottomInset(dp: Int) = store.edit { it[BOTTOM_INSET] = dp.coerceIn(0, MAX_INSET_DP) }
    suspend fun setClockStyle(style: ClockStyle) = store.edit { it[CLOCK_STYLE] = style.name }
    suspend fun setIdleAospClock(value: Boolean) = store.edit { it[IDLE_AOSP_CLOCK] = value }
    suspend fun setMediaArtBackground(value: Boolean) = store.edit { it[MEDIA_ART_BACKGROUND] = value }
    suspend fun setCameraSide(side: CameraSide) = store.edit { it[CAMERA_SIDE] = side.name }
    suspend fun setCameraWidth(dp: Int) = store.edit { it[CAMERA_WIDTH] = dp.coerceIn(0, MAX_CAMERA_WIDTH_DP) }
    suspend fun setPageBarAlways(value: Boolean) = store.edit { it[PAGE_BAR_ALWAYS] = value }
    suspend fun setShowPageDots(value: Boolean) = store.edit { it[SHOW_PAGE_DOTS] = value }
    suspend fun setDockNotifications(value: Boolean) = store.edit { it[DOCK_NOTIFICATIONS] = value }
    suspend fun setDockMedia(value: Boolean) = store.edit { it[DOCK_MEDIA] = value }
    suspend fun setPages(pages: List<CoverPage>) = store.edit {
        it[PAGES] = pages.distinct().ifEmpty { listOf(CoverPage.HOME) }.joinToString(LIST_SEP) { p -> p.name }
    }

    suspend fun setPinnedApps(keys: List<String>) = store.edit {
        it[PINNED] = keys.distinct().joinToString(LIST_SEP)
        it[PINS_INIT] = true
    }

    suspend fun togglePinned(key: String) = store.edit { p ->
        val current = p[PINNED]?.split(LIST_SEP)?.filter { it.isNotBlank() }.orEmpty()
        val next = if (key in current) current - key else current + key
        p[PINNED] = next.joinToString(LIST_SEP)
        p[PINS_INIT] = true
    }

    suspend fun addWidget(id: Int) = store.edit { p ->
        val current = p[WIDGETS]?.split(LIST_SEP)?.mapNotNull { it.toIntOrNull() }.orEmpty()
        if (id !in current) p[WIDGETS] = (current + id).joinToString(LIST_SEP)
    }

    suspend fun removeWidget(id: Int) = store.edit { p ->
        val current = p[WIDGETS]?.split(LIST_SEP)?.mapNotNull { it.toIntOrNull() }.orEmpty()
        p[WIDGETS] = (current - id).joinToString(LIST_SEP)
    }

    suspend fun moveWidget(id: Int, delta: Int) = store.edit { p ->
        val current = p[WIDGETS]?.split(LIST_SEP)?.mapNotNull { it.toIntOrNull() }.orEmpty().toMutableList()
        val from = current.indexOf(id)
        val to = (from + delta).coerceIn(0, current.lastIndex)
        if (from >= 0 && from != to) {
            current.add(to, current.removeAt(from))
            p[WIDGETS] = current.joinToString(LIST_SEP)
        }
    }

    private fun decodePages(raw: String): List<CoverPage> =
        raw.split(LIST_SEP).mapNotNull { runCatching { CoverPage.valueOf(it) }.getOrNull() }
            .ifEmpty { listOf(CoverPage.HOME) }

    companion object {
        const val MAX_INSET_DP = 240
        const val MAX_CAMERA_WIDTH_DP = 320
        private const val MAX_PANELS = 4
        private const val LIST_SEP = ","

        private val AUTO_SHOW = booleanPreferencesKey("auto_show_on_fold")
        private val SHOW_ON_WAKE = booleanPreferencesKey("show_on_wake")
        private val RETURN_AFTER_HOME = booleanPreferencesKey("return_after_home")
        private val PAGES = stringPreferencesKey("pages")
        private val PINNED = stringPreferencesKey("pinned_apps")
        private val PINS_INIT = booleanPreferencesKey("pins_initialized")
        private val WIDGETS = stringPreferencesKey("widget_ids")
        private val BOTTOM_INSET = intPreferencesKey("bottom_inset_dp")
        private val CLOCK_STYLE = stringPreferencesKey("clock_style")
        private val KNOWN_PANELS = stringSetPreferencesKey("known_panels")
        private val IDLE_AOSP_CLOCK = booleanPreferencesKey("idle_aosp_clock")
        private val MEDIA_ART_BACKGROUND = booleanPreferencesKey("media_art_background")
        private val CAMERA_SIDE = stringPreferencesKey("camera_side")
        private val CAMERA_WIDTH = intPreferencesKey("camera_width_dp")
        private val PAGE_BAR_ALWAYS = booleanPreferencesKey("page_bar_always")
        private val SHOW_PAGE_DOTS = booleanPreferencesKey("show_page_dots")
        private val DOCK_NOTIFICATIONS = booleanPreferencesKey("dock_notifications")
        private val DOCK_MEDIA = booleanPreferencesKey("dock_media")
    }
}
