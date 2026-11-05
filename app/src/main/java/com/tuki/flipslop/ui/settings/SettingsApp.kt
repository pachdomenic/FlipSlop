package com.tuki.flipslop.ui.settings

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.service.CoverRuntime

enum class Route {
    MAIN,

    // Categories on the main screen
    ACCESS, BEHAVIOUR, LOCK, COVER, STYLE, CAMERA, DIAGNOSTICS,

    // Under "Cover"
    PAGES, APPS, WIDGETS,

    // Under "Look & style"
    CLOCK,
}

private val RouteStackSaver = listSaver<List<Route>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { names -> names.map(Route::valueOf) },
)

@Composable
fun SettingsApp(runtime: CoverRuntime, initialRoute: String?, host: SettingsHost) {
    val start = when (initialRoute) {
        SettingsActivity.ROUTE_WIDGETS -> Route.WIDGETS
        SettingsActivity.ROUTE_APPS -> Route.APPS
        SettingsActivity.ROUTE_PAGES -> Route.PAGES
        else -> Route.MAIN
    }
    // A back stack: Cover > Pages goes back to Cover, not to the main screen.
    // Deep links (from the cover) start on their screen and close on back.
    var stack by rememberSaveable(stateSaver = RouteStackSaver) { mutableStateOf(listOf(start)) }
    val navigate: (Route) -> Unit = { stack = stack + it }
    val back: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) else host.finish() }

    // Predictive back: the transition follows the back gesture (seeking), the
    // page shrinks toward the swipe edge and shows the previous one under it.
    // On the first screen the system's back-to-home preview takes over.
    val transitionState = remember { SeekableTransitionState(stack) }
    val scope = rememberCoroutineScope()
    var gesture by remember { mutableStateOf<BackGesture?>(null) }
    LaunchedEffect(stack) {
        transitionState.animateTo(stack)
        gesture = null
    }
    PredictiveBackHandler(enabled = stack.size > 1) { progress ->
        val current = stack
        val previous = current.dropLast(1)
        try {
            progress.collect { event ->
                gesture = BackGesture(fromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT)
                transitionState.seekTo(event.progress, targetState = previous)
            }
            stack = previous
        } catch (e: CancellationException) {
            scope.launch {
                transitionState.animateTo(current)
                gesture = null
            }
            throw e
        }
    }

    rememberTransition(transitionState, label = "route").AnimatedContent(
        contentKey = { it.last() },
        transitionSpec = {
            val forward = targetState.size >= initialState.size
            if (forward) {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut())
            } else {
                backTransform(gesture)
            }
        },
    ) { current ->
        // Rounded corners on the page while it leaves, like the system back preview.
        val corner by transition.animateDp(label = "corner") { if (it == EnterExitState.PostExit) 32.dp else 0.dp }
        Box(Modifier.graphicsLayer { shape = RoundedCornerShape(corner); clip = corner > 0.dp }) {
            RouteContent(current.last(), runtime, host, navigate, back)
        }
    }
}

private data class BackGesture(val fromLeft: Boolean)

private const val BACK_MS = 300
private const val BACK_FADE_MS = 90

/**
 * Back to the previous screen: the page being left stays on top, shrinks and
 * moves away from the swipe edge, and fades only at the very end; the previous
 * page waits beneath with a little parallax. Linear while it follows a gesture
 * (the system already eases the gesture progress), eased for the back button.
 */
private fun backTransform(gesture: BackGesture?): ContentTransform {
    val toRight = gesture?.fromLeft ?: true
    val easing = if (gesture != null) LinearEasing else FastOutSlowInEasing
    val move = tween<IntOffset>(BACK_MS, easing = easing)
    val scale = tween<Float>(BACK_MS, easing = easing)
    val enter = slideInHorizontally(move) { if (toRight) -it / 10 else it / 10 } + fadeIn(scale, initialAlpha = 0.5f)
    val exit = scaleOut(scale, targetScale = 0.88f) +
        slideOutHorizontally(move) { if (toRight) it / 8 else -it / 8 } +
        fadeOut(tween(BACK_FADE_MS, delayMillis = BACK_MS - BACK_FADE_MS))
    return ContentTransform(
        targetContentEnter = enter,
        initialContentExit = exit,
        targetContentZIndex = -1f,
        sizeTransform = SizeTransform(clip = false),
    )
}

@Composable
private fun RouteContent(
    route: Route,
    runtime: CoverRuntime,
    host: SettingsHost,
    navigate: (Route) -> Unit,
    back: () -> Unit,
) {
    when (route) {
        Route.MAIN -> MainSettings(runtime, host, navigate)
        Route.PAGES -> PagesSettings(runtime, onBack = back)
        Route.APPS -> AppsSettings(runtime, onBack = back)
        Route.WIDGETS -> WidgetsSettings(runtime, host, onBack = back)
        else -> CategorySettings(route, runtime, host, navigate, onBack = back)
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
