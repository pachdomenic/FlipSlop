package com.tuki.flipslop.fold

import kotlin.math.max
import kotlin.math.min

enum class FoldState {
    FOLDED,
    HALF_OPEN,
    OPEN,
    UNKNOWN;

    val isFolded: Boolean get() = this == FOLDED
}

/** Which signal decided the current [FoldState]. */
enum class FoldSource { DEVICE_STATE, DISPLAY, NONE }

/** A physical panel resolution, independent of rotation once [normalized]. */
data class PanelSize(val width: Int, val height: Int) {
    val shortSide: Int get() = min(width, height)
    val longSide: Int get() = max(width, height)
    val area: Long get() = width.toLong() * height.toLong()
    val aspect: Float get() = if (shortSide == 0) 0f else longSide.toFloat() / shortSide

    fun normalized(): PanelSize = PanelSize(shortSide, longSide)

    override fun toString(): String = "${width}x$height"

    companion object {
        fun parse(value: String): PanelSize? {
            val parts = value.split('x')
            if (parts.size != 2) return null
            val w = parts[0].toIntOrNull() ?: return null
            val h = parts[1].toIntOrNull() ?: return null
            return if (w > 0 && h > 0) PanelSize(w, h) else null
        }
    }
}

data class FoldSnapshot(
    val state: FoldState,
    val source: FoldSource,
    val panel: PanelSize? = null,
    val deviceState: Int? = null,
    val hingeAngle: Float? = null,
) {
    companion object {
        val UNKNOWN = FoldSnapshot(FoldState.UNKNOWN, FoldSource.NONE)
    }
}
