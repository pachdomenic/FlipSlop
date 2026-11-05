package com.tuki.flipslop.ui.cover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuki.flipslop.R
import com.tuki.flipslop.ui.cover.pages.MediaPage

/**
 * The music slide on top of the cover, for when Music is not one of the
 * pages (or, while locked, not one of the lock screen's pages): a music
 * chip or the player card still gets you to the full controls. Swipe down,
 * Back or the button beside the cameras closes it.
 *
 * [contentModifier] places the player above the camera strip,
 * [stripModifier] the close button beside the cameras.
 */
@Composable
fun MediaSheet(
    state: CoverUiState,
    actions: CoverActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier,
    stripModifier: Modifier,
) {
    val closeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            // Keeps taps and drags from reaching the pages underneath.
            .pointerInput(Unit) { detectTapGestures() }
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged > closeThreshold) onClose() },
                ) { _, dy -> dragged += dy }
            },
    ) {
        val art = state.media?.art
        if (state.mediaArtBackground && art != null) {
            val bitmap = remember(art) { art.asImageBitmap() }
            Image(
                bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (state.artBlurDp > 0) Modifier.blur(state.artBlurDp.dp) else Modifier),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.5f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        )
        MediaPage(state, actions, contentModifier)
        Box(stripModifier, contentAlignment = Alignment.Center) {
            FilledTonalButton(onClick = onClose, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.media_sheet_close))
            }
        }
    }
}
