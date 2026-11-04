package com.tuki.flipslop.fold

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tuki.flipslop.core.SystemConfig
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Listens to `DeviceStateManager`, the most precise fold signal there is. The
 * class is a hidden/system API, so we reach it through reflection. That only
 * works when hidden API access is allowed for us: platform-signed builds, or a
 * system app listed in a `<hidden-api-whitelisted-app>` sysconfig entry (see
 * lineage/). Everywhere else [isAvailable] is false and the public display
 * signal takes over.
 */
@SuppressLint("PrivateApi") // On purpose, see above; failures fall back to DisplaySource.
class DeviceStateSource(private val context: Context) {

    private val callbackClass: Class<*>? = runCatching {
        Class.forName("android.hardware.devicestate.DeviceStateManager\$DeviceStateCallback")
    }.getOrNull()

    private val manager: Any? = runCatching {
        context.getSystemService(DEVICE_STATE_SERVICE)
    }.getOrNull()

    private val registerMethod: Method? = resolve("registerCallback", Executor::class.java)
    private val unregisterMethod: Method? = resolve("unregisterCallback")

    /** True when the framework config describes fold states and we can register a callback. */
    val isAvailable: Boolean
        get() = registerMethod != null && unregisterMethod != null && SystemConfig.isFoldable

    fun map(state: Int): FoldState? = when (state) {
        in SystemConfig.foldedDeviceStates -> FoldState.FOLDED
        in SystemConfig.halfFoldedDeviceStates -> FoldState.HALF_OPEN
        in SystemConfig.openDeviceStates -> FoldState.OPEN
        else -> null
    }

    /** Emits device state identifiers, or nothing at all when unavailable. */
    fun states(): Flow<Int> = callbackFlow {
        val cbClass = callbackClass
        val mgr = manager
        val register = registerMethod
        val unregister = unregisterMethod
        if (!isAvailable || cbClass == null || mgr == null || register == null || unregister == null) {
            awaitClose { }
            return@callbackFlow
        }

        val handler = InvocationHandler { proxy, method, args ->
            when (method.name) {
                "onDeviceStateChanged", "onStateChanged" -> {
                    args?.firstOrNull()?.let { identifierOf(it) }?.let { trySend(it) }
                    null
                }
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "toString" -> "FlipSlopDeviceStateCallback"
                else -> null
            }
        }
        val callback = Proxy.newProxyInstance(cbClass.classLoader, arrayOf(cbClass), handler)

        // Never let a reflection surprise crash the binder callback thread.
        val main = Handler(Looper.getMainLooper())
        val executor = Executor { command ->
            main.post {
                try {
                    command.run()
                } catch (t: Throwable) {
                    Log.w(TAG, "device state callback failed", t)
                }
            }
        }

        val registered = runCatching { register.invoke(mgr, executor, callback) }
            .onFailure { Log.w(TAG, "registerCallback failed, falling back to display signal", it) }
            .isSuccess
        awaitClose {
            if (registered) runCatching { unregister.invoke(mgr, callback) }
        }
    }

    private fun resolve(name: String, vararg leading: Class<*>): Method? {
        val mgr = manager ?: return null
        val cb = callbackClass ?: return null
        return runCatching { mgr.javaClass.getMethod(name, *leading, cb) }.getOrNull()
    }

    private fun identifierOf(value: Any): Int? = when (value) {
        is Int -> value
        else -> runCatching { value.javaClass.getMethod("getIdentifier").invoke(value) as Int }.getOrNull()
    }

    private companion object {
        const val TAG = "FlipSlop.DeviceState"
        const val DEVICE_STATE_SERVICE = "device_state"
    }
}
