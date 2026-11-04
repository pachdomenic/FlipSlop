package com.tuki.flipslop.ui.cover.pages

import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.cover.CoverActions
import com.tuki.flipslop.ui.cover.CoverUiState

@Composable
fun TogglesPage(state: CoverUiState, actions: CoverActions, modifier: Modifier = Modifier) {
    val t = state.toggles
    Column(modifier) {
    // Brightness first: it is the one control people reach for on a dark cover.
    Brightness(state, actions)
    Spacer(Modifier.height(12.dp))
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (t.torchAvailable) {
            item { Tile(Icons.Rounded.FlashlightOn, R.string.toggle_torch, t.torch) { actions.setTorch(it) } }
        }
        item { Tile(Icons.Rounded.DoNotDisturbOn, R.string.toggle_dnd, t.dnd) { actions.setDnd(it) } }
        item {
            val (icon, label) = when (t.ringerMode) {
                AudioManager.RINGER_MODE_VIBRATE -> Icons.Rounded.Vibration to R.string.ringer_vibrate
                AudioManager.RINGER_MODE_SILENT -> Icons.Rounded.NotificationsOff to R.string.ringer_silent
                else -> Icons.AutoMirrored.Rounded.VolumeUp to R.string.ringer_normal
            }
            Tile(icon, label, t.ringerMode != AudioManager.RINGER_MODE_NORMAL) { actions.cycleRinger() }
        }
        item { Tile(Icons.Rounded.Wifi, R.string.toggle_wifi, t.wifi) { actions.setWifi(it) } }
        if (t.bluetoothAvailable) {
            item { Tile(Icons.Rounded.Bluetooth, R.string.toggle_bluetooth, t.bluetooth) { actions.setBluetooth(it) } }
        }
        item { Tile(Icons.Rounded.Public, R.string.toggle_internet, false) { actions.openPanel(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) } }
        item { Tile(Icons.AutoMirrored.Rounded.VolumeUp, R.string.toggle_volume, false) { actions.openPanel(Settings.Panel.ACTION_VOLUME) } }
        item { Tile(Icons.Rounded.Settings, R.string.toggle_settings, false) { actions.openSettings() } }
    }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        icon = { Icon(icon, contentDescription = null) },
    ) {
        Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Brightness(state: CoverUiState, actions: CoverActions) {
    val t = state.toggles
    val slider = rememberSliderState(value = t.brightness)
    // Follow outside changes (auto brightness, other apps) while the user is not dragging.
    LaunchedEffect(t.brightness) { slider.value = t.brightness }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BrightnessMedium, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Slider(
                state = slider,
                onValueChange = {
                    slider.value = it
                    actions.setBrightness(it)
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconToggleButton(
                checked = t.autoBrightness,
                onCheckedChange = { actions.setAutoBrightness(it) },
                enabled = t.canWriteSettings,
            ) {
                Icon(Icons.Rounded.BrightnessAuto, contentDescription = stringResource(R.string.auto_brightness))
            }
        }
    }
}
