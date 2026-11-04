package com.tuki.flipslop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFB8C3FF),
    secondary = Color(0xFFC3C5DD),
    tertiary = Color(0xFFE5BAD8),
)
private val FallbackLight = lightColorScheme(
    primary = Color(0xFF4A5C92),
    secondary = Color(0xFF5A5D72),
    tertiary = Color(0xFF75546F),
)

/** Clock styles; the variable system font renders these weights natively. */
object ClockType {
    val hours = TextStyle(fontSize = 128.sp, lineHeight = 112.sp, fontWeight = FontWeight.Black, letterSpacing = (-4).sp)
    val minutes = TextStyle(fontSize = 128.sp, lineHeight = 112.sp, fontWeight = FontWeight.Light, letterSpacing = (-4).sp)
    /** AOSP lock screen proportions: regular weight, tight lines. */
    val idle = TextStyle(
        fontSize = 128.sp,
        lineHeight = 116.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-2).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val inline = TextStyle(fontSize = 96.sp, lineHeight = 96.sp, fontWeight = FontWeight.Medium, letterSpacing = (-3).sp)
}

@Composable
fun FlipSlopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dynamic = dynamicColor && !LocalInspectionMode.current
    val colors = when {
        dynamic && darkTheme -> runCatching { dynamicDarkColorScheme(context) }.getOrDefault(FallbackDark)
        dynamic -> runCatching { dynamicLightColorScheme(context) }.getOrDefault(FallbackLight)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(),
        content = content,
    )
}
