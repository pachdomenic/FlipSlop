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

/** Home page: lock screen style cards, the expressive clock with the camera dock, or just a big clock. */
enum class HomeStyle { CARDS, DOCK, BIG_CLOCK }

/** Notifications page: compact cards like the lock screen, or full cards with actions and reply. */
enum class NotificationStyle { COMPACT, DETAILED }

/** Mask for app icons on the cover. SYSTEM follows the icon shape chosen in Wallpaper & style. */
enum class IconShapeStyle { EXPRESSIVE, CIRCLE, SQUIRCLE, SYSTEM }

/** Monochrome icons tinted with the wallpaper colors. SYSTEM follows Wallpaper & style. */
enum class ThemedIcons { OFF, ON, SYSTEM }

data class CoverSettings(
    val autoShowOnFold: Boolean = true,
    val showOnWake: Boolean = true,
    val returnAfterHome: Boolean = true,
    val pages: List<CoverPage> = DefaultPages,
    val pinnedApps: List<String> = emptyList(),
    val pinsInitialized: Boolean = false,
    val widgetIds: List<Int> = emptyList(),
    val bottomInsetDp: Int = DeviceProfile.current.coverBottomInsetDp,
    val clock: ClockConfig = ClockConfig(),
    val homeStyle: HomeStyle = HomeStyle.CARDS,
    val notificationStyle: NotificationStyle = NotificationStyle.COMPACT,
    val iconShape: IconShapeStyle = IconShapeStyle.EXPRESSIVE,
    val themedIcons: ThemedIcons = ThemedIcons.SYSTEM,
    /** AOSP lock screen style clock while there are no notifications and nothing plays. */
    val idleAospClock: Boolean = true,
    /** Album art fills the media page background and the now playing pill. */
    val mediaArtBackground: Boolean = true,
    /** Blur of the wallpaper behind the cover, in dp (0 = sharp). */
    val wallpaperBlurDp: Int = 0,
    /** Blur of the album art background, in dp. */
    val artBlurDp: Int = 32,
    /** System status bar on the cover (the navigation bar always stays hidden). */
    val showStatusBar: Boolean = true,
    /** While locked: only the clock and music; tap or swipe up to unlock. */
    val lockMinimal: Boolean = true,
    /** Notifications on the simple lock screen too (off by default). */
    val lockNotifications: Boolean = false,
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
            clock = ClockConfig(
                layout = p[CLOCK_STYLE].toEnum(ClockStyle.STACKED),
                font = p[CLOCK_FONT].toEnum(ClockFont.SYSTEM),
                weight = p[CLOCK_WEIGHT] ?: 400,
                scale = p[CLOCK_SCALE] ?: 100,
                color = p[CLOCK_COLOR].toEnum(ClockColor.ACCENT),
                seconds = p[CLOCK_SECONDS] ?: false,
                hourFormat = p[CLOCK_HOUR_FORMAT].toEnum(HourFormat.SYSTEM),
                dateStyle = p[CLOCK_DATE_STYLE].toEnum(DateStyle.SHORT),
                datePosition = p[CLOCK_DATE_POSITION].toEnum(DatePosition.ABOVE),
                align = p[CLOCK_ALIGN].toEnum(ClockAlign.CENTER),
            ),
            homeStyle = p[HOME_STYLE]?.let { runCatching { HomeStyle.valueOf(it) }.getOrNull() } ?: HomeStyle.CARDS,
            notificationStyle = p[NOTIFICATION_STYLE]?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() }
                ?: NotificationStyle.COMPACT,
            iconShape = p[ICON_SHAPE]?.let { runCatching { IconShapeStyle.valueOf(it) }.getOrNull() }
                ?: IconShapeStyle.EXPRESSIVE,
            themedIcons = p[THEMED_ICONS]?.let { runCatching { ThemedIcons.valueOf(it) }.getOrNull() }
                ?: ThemedIcons.SYSTEM,
            idleAospClock = p[IDLE_AOSP_CLOCK] ?: true,
            mediaArtBackground = p[MEDIA_ART_BACKGROUND] ?: true,
            wallpaperBlurDp = p[WALLPAPER_BLUR] ?: 0,
            artBlurDp = p[ART_BLUR] ?: 32,
            showStatusBar = p[SHOW_STATUS_BAR] ?: true,
            lockMinimal = p[LOCK_MINIMAL] ?: true,
            lockNotifications = p[LOCK_NOTIFICATIONS] ?: false,
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
    suspend fun setClockFont(font: ClockFont) = store.edit { it[CLOCK_FONT] = font.name }
    suspend fun setClockWeight(weight: Int) = store.edit {
        it[CLOCK_WEIGHT] = weight.coerceIn(ClockConfig.MIN_WEIGHT, ClockConfig.MAX_WEIGHT)
    }
    suspend fun setClockScale(percent: Int) = store.edit {
        it[CLOCK_SCALE] = percent.coerceIn(ClockConfig.MIN_SCALE, ClockConfig.MAX_SCALE)
    }
    suspend fun setClockColor(color: ClockColor) = store.edit { it[CLOCK_COLOR] = color.name }
    suspend fun setClockSeconds(value: Boolean) = store.edit { it[CLOCK_SECONDS] = value }
    suspend fun setHourFormat(format: HourFormat) = store.edit { it[CLOCK_HOUR_FORMAT] = format.name }
    suspend fun setDateStyle(style: DateStyle) = store.edit { it[CLOCK_DATE_STYLE] = style.name }
    suspend fun setDatePosition(position: DatePosition) = store.edit { it[CLOCK_DATE_POSITION] = position.name }
    suspend fun setClockAlign(align: ClockAlign) = store.edit { it[CLOCK_ALIGN] = align.name }
    suspend fun setHomeStyle(style: HomeStyle) = store.edit { it[HOME_STYLE] = style.name }
    suspend fun setNotificationStyle(style: NotificationStyle) = store.edit { it[NOTIFICATION_STYLE] = style.name }
    suspend fun setIconShape(style: IconShapeStyle) = store.edit { it[ICON_SHAPE] = style.name }
    suspend fun setThemedIcons(mode: ThemedIcons) = store.edit { it[THEMED_ICONS] = mode.name }
    suspend fun setIdleAospClock(value: Boolean) = store.edit { it[IDLE_AOSP_CLOCK] = value }
    suspend fun setMediaArtBackground(value: Boolean) = store.edit { it[MEDIA_ART_BACKGROUND] = value }
    suspend fun setWallpaperBlur(dp: Int) = store.edit { it[WALLPAPER_BLUR] = dp.coerceIn(0, MAX_BLUR_DP) }
    suspend fun setArtBlur(dp: Int) = store.edit { it[ART_BLUR] = dp.coerceIn(0, MAX_BLUR_DP) }
    suspend fun setShowStatusBar(value: Boolean) = store.edit { it[SHOW_STATUS_BAR] = value }
    suspend fun setLockMinimal(value: Boolean) = store.edit { it[LOCK_MINIMAL] = value }
    suspend fun setLockNotifications(value: Boolean) = store.edit { it[LOCK_NOTIFICATIONS] = value }
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
        const val MAX_BLUR_DP = 64
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
        private val CLOCK_FONT = stringPreferencesKey("clock_font")
        private val CLOCK_WEIGHT = intPreferencesKey("clock_weight")
        private val CLOCK_SCALE = intPreferencesKey("clock_scale")
        private val CLOCK_COLOR = stringPreferencesKey("clock_color")
        private val CLOCK_SECONDS = booleanPreferencesKey("clock_seconds")
        private val CLOCK_HOUR_FORMAT = stringPreferencesKey("clock_hour_format")
        private val CLOCK_DATE_STYLE = stringPreferencesKey("clock_date_style")
        private val CLOCK_DATE_POSITION = stringPreferencesKey("clock_date_position")
        private val CLOCK_ALIGN = stringPreferencesKey("clock_align")
        private val HOME_STYLE = stringPreferencesKey("home_style")
        private val NOTIFICATION_STYLE = stringPreferencesKey("notification_style")
        private val ICON_SHAPE = stringPreferencesKey("icon_shape")
        private val THEMED_ICONS = stringPreferencesKey("themed_icons")
        private val KNOWN_PANELS = stringSetPreferencesKey("known_panels")
        private val IDLE_AOSP_CLOCK = booleanPreferencesKey("idle_aosp_clock")
        private val MEDIA_ART_BACKGROUND = booleanPreferencesKey("media_art_background")
        private val WALLPAPER_BLUR = intPreferencesKey("wallpaper_blur_dp")
        private val ART_BLUR = intPreferencesKey("art_blur_dp")
        private val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        private val LOCK_MINIMAL = booleanPreferencesKey("lock_minimal")
        private val LOCK_NOTIFICATIONS = booleanPreferencesKey("lock_notifications")
        private val CAMERA_SIDE = stringPreferencesKey("camera_side")
        private val CAMERA_WIDTH = intPreferencesKey("camera_width_dp")
        private val PAGE_BAR_ALWAYS = booleanPreferencesKey("page_bar_always")
        private val SHOW_PAGE_DOTS = booleanPreferencesKey("show_page_dots")
        private val DOCK_NOTIFICATIONS = booleanPreferencesKey("dock_notifications")
        private val DOCK_MEDIA = booleanPreferencesKey("dock_media")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
