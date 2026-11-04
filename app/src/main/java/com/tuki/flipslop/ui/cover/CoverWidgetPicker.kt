package com.tuki.flipslop.ui.cover

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.components.widgetPickerItems

/** Full-screen widget picker on the cover. */
@Composable
fun CoverWidgetPicker(
    providers: List<AppWidgetProviderInfo>,
    topInset: Dp,
    bottomInset: Dp,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(top = topInset + 12.dp, start = 16.dp, end = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.widgets_pick),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = bottomInset + 16.dp)) {
                widgetPickerItems(providers, onPick)
            }
        }
    }
}
