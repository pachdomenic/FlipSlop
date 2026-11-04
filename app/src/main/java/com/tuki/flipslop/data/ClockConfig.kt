package com.tuki.flipslop.data

/** System font families the clock can use (names from /system/etc/fonts.xml). */
enum class ClockFont(val deviceName: String) {
    SYSTEM("sans-serif"),
    FLEX("roboto-flex"),
    CONDENSED("sans-serif-condensed"),
    SERIF("serif"),
    MONO("monospace"),
    CURSIVE("cursive"),
    CASUAL("casual"),
}

enum class ClockColor { ACCENT, PRIMARY, TERTIARY, WHITE }

enum class HourFormat { SYSTEM, H24, H12 }

enum class DateStyle { SHORT, LONG, NUMERIC, HIDDEN }

enum class DatePosition { ABOVE, BELOW }

enum class ClockAlign { CENTER, START }

/**
 * How the clock looks. Font, weight, color and hour format apply to every
 * clock on the cover; size, seconds, date placement and alignment shape the
 * big clock and the idle clock.
 */
data class ClockConfig(
    val layout: ClockStyle = ClockStyle.STACKED,
    val font: ClockFont = ClockFont.SYSTEM,
    /** 100..900, as the font's wght axis. */
    val weight: Int = 400,
    /** Percent of the size that fills the space, 50..150. */
    val scale: Int = 100,
    val color: ClockColor = ClockColor.ACCENT,
    val seconds: Boolean = false,
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    val dateStyle: DateStyle = DateStyle.SHORT,
    val datePosition: DatePosition = DatePosition.ABOVE,
    val align: ClockAlign = ClockAlign.CENTER,
) {
    companion object {
        const val MIN_WEIGHT = 100
        const val MAX_WEIGHT = 900
        const val MIN_SCALE = 50
        const val MAX_SCALE = 150
    }
}
