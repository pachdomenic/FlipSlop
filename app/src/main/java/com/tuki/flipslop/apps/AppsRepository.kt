package com.tuki.flipslop.apps

import android.app.Activity
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
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
    fun launch(activity: Activity, entry: AppEntry, bounds: Rect? = null) {
        val start = {
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(activity.display.displayId)
            runCatching {
                launcherApps.startMainActivity(entry.component, entry.user, bounds, options.toBundle())
            }
        }
        val km = activity.getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    start()
                }
            })
        } else {
            start()
        }
    }

    fun togglePin(entry: AppEntry) {
        scope.launch { prefs.togglePinned(entry.key) }
    }
}
