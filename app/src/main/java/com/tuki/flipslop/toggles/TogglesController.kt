package com.tuki.flipslop.toggles

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.tuki.flipslop.notifications.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.roundToInt

data class ToggleStates(
    val torchAvailable: Boolean = false,
    val torch: Boolean = false,
    val dnd: Boolean = false,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
    val bluetoothAvailable: Boolean = false,
    /** 0..1 slider position. */
    val brightness: Float = 0.5f,
    val autoBrightness: Boolean = false,
    val canWriteSettings: Boolean = false,
)

/**
 * Quick toggles. Each one uses the strongest API the app may call and reports
 * `false` from its setter when the UI should open the matching system panel
 * instead.
 */
class TogglesController(
    private val context: Context,
    private val notifications: NotificationsRepository,
) {
    private val camera = context.getSystemService(CameraManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val wifi = context.getSystemService(WifiManager::class.java)
    private val bluetooth: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val torchCameraId: String? = runCatching {
        val ids = camera.cameraIdList
        ids.firstOrNull { id ->
            val c = camera.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: ids.firstOrNull { camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    }.getOrNull()

    private var torchOn = false

    private val _state = MutableStateFlow(ToggleStates())
    val state: StateFlow<ToggleStates> = _state.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                torchOn = enabled
                refresh()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refresh()
    }

    private var users = 0

    fun start() {
        if (users++ > 0) return
        runCatching { camera.registerTorchCallback(torchCallback, handler) }
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        refresh()
    }

    fun stop() {
        if (--users > 0) return
        users = 0
        runCatching { camera.unregisterTorchCallback(torchCallback) }
        runCatching { context.unregisterReceiver(receiver) }
    }

    fun refresh() {
        val cr = context.contentResolver
        val raw = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
        _state.value = ToggleStates(
            torchAvailable = torchCameraId != null,
            torch = torchOn,
            dnd = nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL,
            ringerMode = audio.ringerMode,
            wifi = runCatching { wifi.isWifiEnabled }.getOrDefault(false),
            bluetooth = runCatching { bluetooth?.isEnabled == true }.getOrDefault(false),
            bluetoothAvailable = bluetooth != null,
            brightness = rawToSlider(raw),
            autoBrightness = Settings.System.getInt(
                cr,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
            canWriteSettings = Settings.System.canWrite(context),
        )
    }

    fun setTorch(on: Boolean): Boolean {
        val id = torchCameraId ?: return false
        return runCatching { camera.setTorchMode(id, on) }.isSuccess
    }

    /** Uses the notification listener, then DND access; false means "open settings". */
    fun setDnd(on: Boolean): Boolean {
        val filter = if (on) {
            NotificationListenerService.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        }
        notifications.activeListener?.let {
            it.requestInterruptionFilter(filter)
            handler.postDelayed(::refresh, 150)
            return true
        }
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(filter)
            return true
        }
        return false
    }

    /** Normal -> vibrate -> silent (silent only with DND access) -> normal. */
    fun cycleRinger() {
        val next = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE ->
                if (nm.isNotificationPolicyAccessGranted) AudioManager.RINGER_MODE_SILENT
                else AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        runCatching { audio.ringerMode = next }
        refresh()
    }

    /** Only system apps may still toggle Wi-Fi directly. */
    @Suppress("DEPRECATION")
    fun setWifi(on: Boolean): Boolean = runCatching { wifi.setWifiEnabled(on) }.getOrDefault(false)

    /** Only system apps may still toggle Bluetooth directly. */
    @Suppress("DEPRECATION", "MissingPermission")
    fun setBluetooth(on: Boolean): Boolean = runCatching {
        val adapter = bluetooth ?: return false
        if (on) adapter.enable() else adapter.disable()
    }.getOrDefault(false)

    /** Writes system brightness when allowed; false means "use window brightness". */
    fun setBrightness(level: Float): Boolean {
        if (!Settings.System.canWrite(context)) return false
        val cr = context.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, sliderToRaw(level))
        _state.value = _state.value.copy(brightness = level, autoBrightness = false)
        return true
    }

    fun setAutoBrightness(on: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) return false
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        refresh()
        return true
    }

    companion object {
        // The slider is perceptual; the setting is roughly linear in light output.
        private const val GAMMA = 2.2f

        fun sliderToRaw(level: Float): Int = (1 + level.coerceIn(0f, 1f).pow(GAMMA) * 254).roundToInt()

        fun rawToSlider(raw: Int): Float = ((raw.coerceIn(1, 255) - 1) / 254f).pow(1 / GAMMA)
    }
}
