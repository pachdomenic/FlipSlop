package com.tuki.flipslop.apps

import android.app.Activity
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.widget.Toast
import com.tuki.flipslop.R
import org.xmlpull.v1.XmlPullParser
import com.tuki.flipslop.data.CoverPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(
    val component: ComponentName,
    val user: UserHandle,
    val label: String,
    val icon: Drawable,
) {
    val key: String get() = component.flattenToString()
}

/**
 * One of an app's shortcuts ("New message", "Selfie"...). Comes from
 * ShortcutManager when FlipSlop may read shortcuts (it is the default
 * launcher), otherwise straight from the app's manifest.
 */
class AppShortcut(
    val label: String,
    val icon: Drawable?,
    internal val info: ShortcutInfo? = null,
    internal val intents: List<Intent> = emptyList(),
)

/** What the long-press menu of an app offers besides pinning. */
data class AppMenu(
    val shortcuts: List<AppShortcut> = emptyList(),
    val canUninstall: Boolean = false,
)

class AppsRepository(
    private val context: Context,
    private val prefs: CoverPrefs,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    private val _all = MutableStateFlow<List<AppEntry>>(emptyList())
    val all: StateFlow<List<AppEntry>> = _all.asStateFlow()

    val pinned: StateFlow<List<AppEntry>> =
        combine(_all, prefs.settings) { apps, settings ->
            val byKey = apps.associateBy { it.key }
            settings.pinnedApps.mapNotNull { byKey[it] }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            reload()

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            reload()
    }

    init {
        launcherApps.registerCallback(callback)
        reload()
    }

    fun reload() {
        scope.launch {
            _all.value = withContext(Dispatchers.IO) { load() }
        }
    }

    private fun load(): List<AppEntry> {
        val me = Process.myUserHandle()
        val density = context.resources.displayMetrics.densityDpi
        return launcherApps.getActivityList(null, me)
            .asSequence()
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                AppEntry(
                    component = info.componentName,
                    user = info.user,
                    label = info.label.toString(),
                    icon = info.getIcon(density),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** First run: pin the user's default dialer, messages, camera and friends. */
    suspend fun ensureDefaultPins() {
        if (prefs.settings.first().pinsInitialized) return
        val apps = withContext(Dispatchers.IO) { load() }
        if (apps.isEmpty()) return
        val byPackage = apps.groupBy { it.component.packageName }
        val pm = context.packageManager
        val wanted = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
        )
        val keys = wanted.mapNotNull { intent ->
            val pkg = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName ?: return@mapNotNull null
            byPackage[pkg]?.firstOrNull()?.key
        }.distinct()
        prefs.setPinnedApps(keys)
    }

    /** Launch on the activity's display; unlock first when the keyguard is up. */
    fun launch(activity: Activity, entry: AppEntry, bounds: Rect? = null) = whenUnlocked(activity) {
        runCatching {
            launcherApps.startMainActivity(entry.component, entry.user, bounds, activity.coverOptions())
        }
    }

    /** Shortcuts, app info and uninstall for the app's long-press menu. */
    suspend fun menu(entry: AppEntry): AppMenu = withContext(Dispatchers.IO) {
        AppMenu(
            shortcuts = runCatching { shortcuts(entry) }.getOrDefault(emptyList()),
            canUninstall = runCatching {
                val info = context.packageManager.getApplicationInfo(entry.component.packageName, 0)
                info.flags and ApplicationInfo.FLAG_SYSTEM == 0
            }.getOrDefault(false),
        )
    }

    fun launchShortcut(activity: Activity, shortcut: AppShortcut) = whenUnlocked(activity) {
        val started = runCatching {
            val info = shortcut.info
            if (info != null) {
                launcherApps.startShortcut(info, null, activity.coverOptions())
            } else {
                activity.startActivities(shortcut.intents.toTypedArray(), activity.coverOptions())
            }
        }
        if (started.isFailure) {
            Toast.makeText(activity, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(activity: Activity, entry: AppEntry) = whenUnlocked(activity) {
        runCatching {
            launcherApps.startAppDetailsActivity(entry.component, entry.user, null, activity.coverOptions())
        }
    }

    /** The system's uninstall dialog, on the cover. */
    fun uninstall(activity: Activity, entry: AppEntry) = whenUnlocked(activity) {
        val intent = Intent(
            Intent.ACTION_DELETE,
            Uri.fromParts("package", entry.component.packageName, entry.component.className),
        )
            .putExtra(Intent.EXTRA_USER, entry.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        runCatching { activity.startActivity(intent, activity.coverOptions()) }
    }

    private fun Activity.coverOptions(): Bundle =
        ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId).toBundle()

    private fun whenUnlocked(activity: Activity, block: () -> Unit) {
        val km = activity.getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    block()
                }
            })
        } else {
            block()
        }
    }

    private fun shortcuts(entry: AppEntry): List<AppShortcut> {
        val host = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)
        return if (host) hostShortcuts(entry) else manifestShortcuts(entry)
    }

    /** Static and dynamic shortcuts through LauncherApps, as the default launcher. */
    private fun hostShortcuts(entry: AppEntry): List<AppShortcut> {
        val query = LauncherApps.ShortcutQuery()
            .setPackage(entry.component.packageName)
            .setActivity(entry.component)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        val density = context.resources.displayMetrics.densityDpi
        return launcherApps.getShortcuts(query, entry.user).orEmpty()
            .filter { it.isEnabled }
            // Same order as the home screen: manifest ones first, each group by rank.
            .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
            .take(MAX_SHORTCUTS)
            .mapNotNull { info ->
                val label = (info.shortLabel ?: info.longLabel)?.toString()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                AppShortcut(label, runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull(), info)
            }
    }

    /**
     * Without shortcut access (FlipSlop is not the default launcher), read the
     * shortcuts the app declares in its manifest (android.app.shortcuts) and
     * keep the ones that open an exported activity we may start ourselves.
     */
    private fun manifestShortcuts(entry: AppEntry): List<AppShortcut> {
        val pm = context.packageManager
        val activity = pm.getActivityInfo(entry.component, PackageManager.GET_META_DATA)
        val xml = activity.metaData?.getInt(SHORTCUTS_META_DATA) ?: 0
        if (xml == 0) return emptyList()
        val res = pm.getResourcesForApplication(activity.applicationInfo)
        return res.getXml(xml).use { parseShortcuts(it, res) }
            .filter { it.intents.isNotEmpty() && canStart(it.intents.last()) }
            .take(MAX_SHORTCUTS)
    }

    private fun parseShortcuts(parser: XmlResourceParser, res: Resources): List<AppShortcut> {
        val out = mutableListOf<AppShortcut>()
        var label: String? = null
        var iconRes = 0
        var enabled = false
        var intents = mutableListOf<Intent>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "shortcut") {
                enabled = parser.getAttributeBooleanValue(ANDROID_NS, "enabled", true)
                label = parser.string(res, "shortcutShortLabel") ?: parser.string(res, "shortcutLongLabel")
                iconRes = parser.getAttributeResourceValue(ANDROID_NS, "icon", 0)
                intents = mutableListOf()
            } else if (event == XmlPullParser.START_TAG && parser.name == "intent") {
                // Reads up to the matching </intent>, extras and categories included.
                val intent = Intent.parseIntent(res, parser, parser)
                if (intent.action == null) intent.action = Intent.ACTION_VIEW
                intents += intent
            } else if (event == XmlPullParser.END_TAG && parser.name == "shortcut") {
                val text = label
                if (enabled && text != null && intents.isNotEmpty()) {
                    // As ShortcutManager does: the stack starts in a fresh task.
                    intents.first().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    val icon = iconRes.takeIf { it != 0 }?.let { runCatching { res.getDrawable(it, null) }.getOrNull() }
                    out += AppShortcut(text, icon, intents = intents)
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun XmlResourceParser.string(res: Resources, name: String): String? {
        val id = getAttributeResourceValue(ANDROID_NS, name, 0)
        val text = if (id != 0) runCatching { res.getString(id) }.getOrNull() else getAttributeValue(ANDROID_NS, name)
        return text?.takeIf { it.isNotBlank() }
    }

    private fun canStart(intent: Intent): Boolean {
        val info = context.packageManager.resolveActivity(intent, 0)?.activityInfo ?: return false
        val permission = info.permission
        return info.exported &&
            (permission == null || context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
    }

    fun togglePin(entry: AppEntry) {
        scope.launch { prefs.togglePinned(entry.key) }
    }

    private companion object {
        const val SHORTCUTS_META_DATA = "android.app.shortcuts"
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val MAX_SHORTCUTS = 4
    }
}
