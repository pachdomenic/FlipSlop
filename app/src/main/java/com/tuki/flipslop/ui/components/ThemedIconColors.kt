package com.tuki.flipslop.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.tuki.flipslop.core.HomeApp

/** Background and glyph colors of a themed icon, as ARGB ints. */
data class ThemedIconColors(val background: Int, val foreground: Int)

/**
 * Themed icon colors that match the home screen: read from the launcher's
 * own color resources (Trebuchet / Launcher3), resolved with the system
 * configuration so day / night follows the system like the launcher does.
 * Falls back to the same system palette entries Launcher3 uses.
 */
object LauncherThemedColors {

    @SuppressLint("DiscouragedApi") // The launcher's resources have no R class for us.
    fun read(context: Context): ThemedIconColors {
        val home = HomeApp.packageName(context)
        if (home != null) {
            runCatching {
                val res = context.packageManager.getResourcesForApplication(home)
                val bg = res.getIdentifier(BACKGROUND, "color", home)
                val fg = res.getIdentifier(FOREGROUND, "color", home)
                if (bg != 0 && fg != 0) {
                    return ThemedIconColors(res.getColor(bg, null), res.getColor(fg, null))
                }
            }
        }
        return fallback(context)
    }

    private fun fallback(context: Context): ThemedIconColors {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) {
            ThemedIconColors(
                background = context.getColor(android.R.color.system_neutral1_800),
                foreground = context.getColor(android.R.color.system_accent1_100),
            )
        } else {
            ThemedIconColors(
                background = context.getColor(android.R.color.system_accent1_100),
                foreground = context.getColor(android.R.color.system_neutral2_700),
            )
        }
    }

    private const val BACKGROUND = "themed_icon_background_color"
    private const val FOREGROUND = "themed_icon_color"
}

/** Re-read whenever the configuration (night mode, palette) changes. */
@Composable
fun rememberThemedIconColors(): ThemedIconColors {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) { LauncherThemedColors.read(context) }
}
