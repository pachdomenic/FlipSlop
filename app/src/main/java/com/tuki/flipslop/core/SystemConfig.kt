package com.tuki.flipslop.core

import android.annotation.SuppressLint
import android.content.res.Resources

/**
 * Reads framework (`android`) config resources. These are internal resources,
 * but resolving them by name through [Resources.getSystem] is a plain resource
 * lookup, so it works for any app and picks up device overlays such as
 * FrameworksResArcfox.
 */
@SuppressLint("DiscouragedApi") // Framework config has no compile-time R ids for apps.
object SystemConfig {
    fun intArray(name: String): IntArray = runCatching {
        val res = Resources.getSystem()
        val id = res.getIdentifier(name, "array", "android")
        if (id == 0) IntArray(0) else res.getIntArray(id)
    }.getOrDefault(IntArray(0))

    fun bool(name: String, default: Boolean = false): Boolean = runCatching {
        val res = Resources.getSystem()
        val id = res.getIdentifier(name, "bool", "android")
        if (id == 0) default else res.getBoolean(id)
    }.getOrDefault(default)

    val foldedDeviceStates: Set<Int> by lazy { intArray("config_foldedDeviceStates").toSet() }
    val halfFoldedDeviceStates: Set<Int> by lazy { intArray("config_halfFoldedDeviceStates").toSet() }
    val openDeviceStates: Set<Int> by lazy { intArray("config_openDeviceStates").toSet() }

    /** True when the framework is configured as a foldable at all. */
    val isFoldable: Boolean get() = foldedDeviceStates.isNotEmpty()
}
