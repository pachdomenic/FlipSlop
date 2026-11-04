package com.tuki.flipslop.ui.cover

import android.text.format.DateFormat
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.tuki.flipslop.data.ClockAlign
import com.tuki.flipslop.data.ClockColor
import com.tuki.flipslop.data.ClockConfig
import com.tuki.flipslop.data.ClockStyle
import com.tuki.flipslop.data.DatePosition
import com.tuki.flipslop.data.DateStyle
import com.tuki.flipslop.data.HourFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.delay

/**
 * The clock's font: a system family by name, with the variable weight axis set
 * so any weight renders as drawn (non-variable fonts take the closest weight).
 */
@Composable
fun ClockConfig.fontFamily(): FontFamily = remember(font, weight) {
    FontFamily(
        Font(
            DeviceFontFamilyName(font.deviceName),
            FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )
}

@Composable
fun ClockConfig.textColor(): Color = when (color) {
    ClockColor.ACCENT -> MaterialTheme.colorScheme.primaryFixed
    ClockColor.PRIMARY -> MaterialTheme.colorScheme.primary
    ClockColor.TERTIARY -> MaterialTheme.colorScheme.tertiary
    ClockColor.WHITE -> Color.White
}

/** A clock text style at [size] in the configured font and weight, lines trimmed tight. */
@Composable
fun ClockConfig.style(size: TextUnit, lineHeight: TextUnit = size): TextStyle = TextStyle(
    fontFamily = fontFamily(),
    fontWeight = FontWeight(weight),
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = (-0.02).em,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

fun ClockConfig.uses24h(system24: Boolean): Boolean = when (hourFormat) {
    HourFormat.SYSTEM -> system24
    HourFormat.H24 -> true
    HourFormat.H12 -> false
}

fun ClockConfig.hours(now: Long, system24: Boolean): String =
    DateFormat.format(if (uses24h(system24)) "HH" else "h", Date(now)).toString()

fun minutes(now: Long): String = DateFormat.format("mm", Date(now)).toString()

fun seconds(now: Long): String = DateFormat.format("ss", Date(now)).toString()

/** The date in the configured style, capitalized; null when hidden. */
fun ClockConfig.date(now: Long, locale: Locale): String? {
    val skeleton = when (dateStyle) {
        DateStyle.SHORT -> "EEEdMMM"
        DateStyle.LONG -> "EEEEdMMMM"
        DateStyle.NUMERIC -> "ddMM"
        DateStyle.HIDDEN -> return null
    }
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return DateFormat.format(pattern, Date(now)).toString().replaceFirstChar { it.titlecase(locale) }
}

/** [minuteNow] normally; ticks every second when the clock shows seconds. */
@Composable
fun rememberClockNow(minuteNow: Long, withSeconds: Boolean): Long {
    var now by remember { mutableLongStateOf(minuteNow) }
    LaunchedEffect(withSeconds, minuteNow) {
        now = if (withSeconds) System.currentTimeMillis() else minuteNow
        while (withSeconds) {
            delay(1_000 - System.currentTimeMillis() % 1_000)
            now = System.currentTimeMillis()
        }
    }
    return if (withSeconds) now else minuteNow
}

/**
 * Home in big clock mode: the time as large as the page allows (times the
 * size setting), with the date above or below and optional seconds.
 */
@Composable
fun BigClock(state: CoverUiState, modifier: Modifier = Modifier) {
    val c = state.clock
    val locale = LocalConfiguration.current.locales[0]
    val now = rememberClockNow(state.now, c.seconds)
    val hours = c.hours(now, state.is24h)
    val mins = minutes(now)
    val secs = if (c.seconds) seconds(now) else null
    val date = c.date(now, locale)
    val color = c.textColor()
    val center = c.align == ClockAlign.CENTER
    val horizontal = if (center) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (center) TextAlign.Center else TextAlign.Start

    @Composable
    fun DateLine() {
        if (date != null) {
            Text(
                date,
                style = c.style(MaterialTheme.typography.headlineSmall.fontSize).copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign,
                maxLines = 1,
            )
        }
    }

    BoxWithConstraints(modifier, contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
        val density = LocalDensity.current
        val stacked = c.layout == ClockStyle.STACKED
        // Room for the date line, which travels with the digits.
        val dateRoom = if (date != null) DATE_ROOM_DP else 0f
        // Rough digit width of 0.6em; stacked lines are LINE_EM apart.
        val chars = if (stacked) 2f else 5f + (if (secs != null) 1.5f else 0f)
        val byWidth = maxWidth.value / (chars * DIGIT_WIDTH_EM)
        val lines = if (stacked) 2f * LINE_EM + 0.2f + (if (secs != null) 0.35f else 0f) else 1.1f
        val byHeight = (maxHeight.value - dateRoom) / lines
        val sizeDp = min(byWidth, byHeight) * c.scale / 100f
        val size = with(density) { Dp(sizeDp).toSp() }
        val big = c.style(size, size * LINE_EM)
        Column(Modifier.wrapContentSize(unbounded = true), horizontalAlignment = horizontal) {
            if (c.datePosition == DatePosition.ABOVE) DateLine()
            if (stacked) {
                // One two-line Text: line spacing between the lines is what lineHeight sets.
                Text("$hours\n$mins", style = big, color = color, maxLines = 2, textAlign = textAlign)
                if (secs != null) Text(secs, style = c.style(size * 0.32f), color = color.copy(alpha = 0.8f))
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$hours:$mins", style = big, color = color, maxLines = 1)
                    if (secs != null) Text(":$secs", style = c.style(size * 0.4f), color = color.copy(alpha = 0.8f))
                }
            }
            if (c.datePosition == DatePosition.BELOW) DateLine()
        }
    }
}

/** Stacked lines sit this far apart, in em: tight like a lock screen clock. */
private const val LINE_EM = 0.82f

private const val DATE_ROOM_DP = 40f

private const val DIGIT_WIDTH_EM = 0.6f
