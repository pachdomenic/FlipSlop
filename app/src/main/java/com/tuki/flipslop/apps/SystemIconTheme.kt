package com.tuki.flipslop.apps

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import com.tuki.flipslop.core.HomeApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The "Themed icons" switch from Wallpaper & style. Android keeps no system
 * setting for it: ThemePicker stores it in the launcher, which exposes it as
 * content://<launcher>.grid_control/icon_themed (column boolean_value, 1/0),
 * guarded by <launcher>.permission.READ_SETTINGS (signatureOrSystem).
 * See Launcher3/Trebuchet GridCustomizationsProvider.
 *
 * A priv-app build holds that permission; a plain APK cannot read it and
 * [readable] stays false.
 */
class SystemIconTheme(private val context: Context) {

    private val _themed = MutableStateFlow(false)
    val themed: StateFlow<Boolean> = _themed.asStateFlow()

    /** Whether the last read reached the launcher's provider. */
    var readable: Boolean = false
        private set

    private var observed: Uri? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    fun refresh() {
        val home = HomeApp.packageName(context)
        if (home == null) {
            readable = false
            _themed.value = false
            return
        }
        val uri = "content://$home.grid_control/icon_themed".toUri()
        val value: Boolean? = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val column = c.getColumnIndex(COLUMN)
                if (column >= 0 && c.moveToFirst()) c.getInt(column) == 1 else null
            }
        }.getOrNull()
        readable = value != null
        _themed.value = value ?: false
        if (readable && observed != uri) {
            observed?.let { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
            observed = uri.takeIf {
                runCatching { context.contentResolver.registerContentObserver(uri, false, observer) }.isSuccess
            }
        }
    }

    private companion object {
        const val COLUMN = "boolean_value"
    }
}
