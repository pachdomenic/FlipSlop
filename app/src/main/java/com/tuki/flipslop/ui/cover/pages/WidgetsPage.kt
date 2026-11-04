package com.tuki.flipslop.ui.cover.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.cover.CoverActions
import com.tuki.flipslop.ui.cover.CoverUiState

/** One widget per page; swipe up and down between them. */
@Composable
fun WidgetsPage(
    state: CoverUiState,
    actions: CoverActions,
    modifier: Modifier = Modifier,
    widget: @Composable (id: Int, modifier: Modifier) -> Unit,
) {
    if (state.widgetIds.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Widgets,
            text = stringResource(R.string.widgets_empty),
            action = stringResource(R.string.widgets_add),
            onAction = actions::addWidget,
            modifier = modifier,
        )
        return
    }
    val pager = rememberPagerState { state.widgetIds.size }
    Column(modifier) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.page_widgets),
            style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(onClick = actions::addWidget, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.widgets_add))
        }
    }
    VerticalPager(state = pager, modifier = Modifier.weight(1f).padding(top = 8.dp), key = { state.widgetIds[it] }) { index ->
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                widget(state.widgetIds[index], Modifier.fillMaxSize())
            }
        }
    }
    }
}
