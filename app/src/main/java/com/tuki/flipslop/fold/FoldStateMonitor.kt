package com.tuki.flipslop.fold

import android.content.Context
import com.tuki.flipslop.core.DeviceProfile
import com.tuki.flipslop.data.CoverPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Merges every fold signal into one [StateFlow]. Priority:
 * DeviceStateManager (system builds) > display panel (+ hinge) (everyone).
 */
class FoldStateMonitor(
    context: Context,
    private val prefs: CoverPrefs,
    private val scope: CoroutineScope,
    private val profile: DeviceProfile = DeviceProfile.current,
) {
    private val deviceStates = DeviceStateSource(context)
    private val display = DisplaySource(context)
    private val hinge = HingeSource(context)
    private val classifier = CoverDisplayClassifier()

    val deviceStateAvailable: Boolean get() = deviceStates.isAvailable
    val hingeAvailable: Boolean get() = hinge.isAvailable

    private val deviceStateFlow: Flow<Int?> = deviceStates.states().map<Int, Int?> { it }.onStart { emit(null) }
    private val panelFlow: Flow<PanelSize?> = display.panels().map<PanelSize, PanelSize?> { it }.onStart { emit(null) }
    private val hingeFlow: Flow<Float?> = hinge.angles().map<Float, Float?> { it }.onStart { emit(null) }

    val snapshot: StateFlow<FoldSnapshot> =
        combine(deviceStateFlow, panelFlow, hingeFlow, prefs.knownPanels) { ds, panel, angle, known ->
            resolve(ds, panel, angle, known + profile.knownPanels)
        }
            .distinctUntilChanged()
            .onEach { learn(it.panel) }
            .stateIn(scope, SharingStarted.Eagerly, FoldSnapshot.UNKNOWN)

    private fun resolve(
        deviceState: Int?,
        panel: PanelSize?,
        hingeAngle: Float?,
        known: Set<PanelSize>,
    ): FoldSnapshot {
        val fromDeviceState = deviceState?.let { deviceStates.map(it) }
        if (fromDeviceState != null) {
            return FoldSnapshot(fromDeviceState, FoldSource.DEVICE_STATE, panel, deviceState, hingeAngle)
        }
        if (panel != null) {
            val state = classifier.classify(panel, known, hingeAngle)
            return FoldSnapshot(state, FoldSource.DISPLAY, panel, deviceState, hingeAngle)
        }
        return FoldSnapshot.UNKNOWN.copy(hingeAngle = hingeAngle, deviceState = deviceState)
    }

    private fun learn(panel: PanelSize?) {
        if (panel == null) return
        scope.launch { prefs.rememberPanel(panel.normalized()) }
    }
}
