package com.tuki.flipslop.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.tuki.flipslop.core.CameraSide
import com.tuki.flipslop.core.DeviceProfile
import com.tuki.flipslop.fold.PanelSize
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val STORE_FILE = "datastore/cover.preferences_pb"

/**
 * Settings live in device protected storage, so the cover has them right
 * after a reboot, before the first unlock (the app is directBootAware).
 * One instance per process, as DataStore requires.
 */
@Volatile
private var coverStore: DataStore<Preferences>? = null

private fun coverDataStore(app: Context): DataStore<Preferences> = coverStore ?: synchronized(CoverPrefs::class) {
    coverStore ?: PreferenceDataStoreFactory.create(
        produceFile = { File(app.createDeviceProtectedStorageContext().filesDir, STORE_FILE) },
    ).also { coverStore = it }
}

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
    /** The cover while locked. */
    val lock: LockConfig = LockConfig(),
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
    private val app = context.applicationContext
    private val store = coverDataStore(app)

    /**
     * Older versions kept the settings in credential encrypted storage, which
     * opens only after the first unlock. Once it does, take them over (they win
     * over anything written while locked) and drop the old file. Call only
     * while the user is unlocked.
     */
    suspend fun migrateFromCredentialStorage() = withContext(Dispatchers.IO) {
        val old = File(app.filesDir, STORE_FILE)
        if (!old.exists()) return@withContext
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val legacy = PreferenceDataStoreFactory.create(scope = scope, produceFile = { old })
            val values = legacy.data.first()
            store.edit { it += values }
        } finally {
            scope.cancel()
        }
        old.delete()
    }

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
            lock = LockConfig(
                enabled = p[LOCK_MINIMAL] ?: true,
                layout = p[LOCK_LAYOUT].toEnum(LockLayout.CARDS),
                // Before the lock screen settings there was only an on / off switch for cards.
                notifications = p[LOCK_NOTIFICATION_MODE].toEnum(
                    if (p[LOCK_NOTIFICATIONS] == true) LockNotifications.CARDS else LockNotifications.HIDDEN,
                ),
                media = p[LOCK_MEDIA] ?: true,
                tabs = p[LOCK_TABS].orEmpty()
                    .mapNotNull { runCatching { CoverPage.valueOf(it) }.getOrNull() }
                    .filter { it in LockTabCandidates }
                    .toSet(),
                gesture = p[LOCK_GESTURE].toEnum(UnlockGesture.TAP_OR_SWIPE),
                // The old switch only knew "beside the cameras" or "off".
                hintPosition = p[LOCK_HINT_POSITION].toEnum(
                    if (p[LOCK_HINT] == false) LockHintPosition.HIDDEN else LockHintPosition.TOP,
                ),
                stripMedia = p[LOCK_STRIP_MEDIA] ?: true,
                stripNotifications = p[LOCK_STRIP_NOTIFICATIONS] ?: true,
                fingerprint = p[LOCK_FINGERPRINT].toEnum(FingerprintUnlock.IMMEDIATE),
            ),
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
    suspend fun setLockEnabled(value: Boolean) = store.edit { it[LOCK_MINIMAL] = value }
    suspend fun setLockLayout(layout: LockLayout) = store.edit { it[LOCK_LAYOUT] = layout.name }
    suspend fun setLockNotifications(mode: LockNotifications) = store.edit { it[LOCK_NOTIFICATION_MODE] = mode.name }
    suspend fun setLockMedia(value: Boolean) = store.edit { it[LOCK_MEDIA] = value }
    suspend fun setLockTab(page: CoverPage, shown: Boolean) = store.edit { p ->
        if (page !in LockTabCandidates) return@edit
        val current = p[LOCK_TABS].orEmpty()
        p[LOCK_TABS] = if (shown) current + page.name else current - page.name
    }
    suspend fun setUnlockGesture(gesture: UnlockGesture) = store.edit { it[LOCK_GESTURE] = gesture.name }
    suspend fun setLockHintPosition(position: LockHintPosition) = store.edit { it[LOCK_HINT_POSITION] = position.name }
    suspend fun setLockStripMedia(value: Boolean) = store.edit { it[LOCK_STRIP_MEDIA] = value }
    suspend fun setLockStripNotifications(value: Boolean) = store.edit { it[LOCK_STRIP_NOTIFICATIONS] = value }
    suspend fun setFingerprintUnlock(mode: FingerprintUnlock) = store.edit { it[LOCK_FINGERPRINT] = mode.name }
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
        private val LOCK_LAYOUT = stringPreferencesKey("lock_layout")
        private val LOCK_NOTIFICATION_MODE = stringPreferencesKey("lock_notification_mode")
        private val LOCK_MEDIA = booleanPreferencesKey("lock_media")
        private val LOCK_TABS = stringSetPreferencesKey("lock_tabs")
        private val LOCK_GESTURE = stringPreferencesKey("lock_gesture")
        private val LOCK_HINT = booleanPreferencesKey("lock_hint")
        private val LOCK_HINT_POSITION = stringPreferencesKey("lock_hint_position")
        private val LOCK_STRIP_MEDIA = booleanPreferencesKey("lock_strip_media")
        private val LOCK_STRIP_NOTIFICATIONS = booleanPreferencesKey("lock_strip_notifications")
        private val LOCK_FINGERPRINT = stringPreferencesKey("lock_fingerprint")
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
