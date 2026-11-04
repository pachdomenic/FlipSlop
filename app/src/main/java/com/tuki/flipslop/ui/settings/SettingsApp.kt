package com.tuki.flipslop.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.service.CoverRuntime

enum class Route { MAIN, PAGES, APPS, WIDGETS }

@Composable
fun SettingsApp(runtime: CoverRuntime, initialRoute: String?, host: SettingsHost) {
    val start = when (initialRoute) {
        SettingsActivity.ROUTE_WIDGETS -> Route.WIDGETS
        SettingsActivity.ROUTE_APPS -> Route.APPS
        SettingsActivity.ROUTE_PAGES -> Route.PAGES
        else -> Route.MAIN
    }
    var route by rememberSaveable { mutableStateOf(start) }
    // Deep links (from the cover) close the activity on back; normal navigation goes up.
    val back: () -> Unit = { if (route == Route.MAIN || route == start) host.finish() else route = Route.MAIN }
    BackHandler(enabled = route != Route.MAIN, onBack = back)

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
        },
        label = "route",
    ) { current ->
        when (current) {
            Route.MAIN -> MainSettings(runtime, host, navigate = { route = it })
            Route.PAGES -> PagesSettings(runtime, onBack = back)
            Route.APPS -> AppsSettings(runtime, onBack = back)
            Route.WIDGETS -> WidgetsSettings(runtime, host, onBack = back)
        }
    }
}

/** Shared scaffold: expressive large flexible app bar over a lazy list. */
@Composable
fun SettingsScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = subtitle?.let { { Text(it) } },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            content = content,
        )
    }
}
