package com.tuki.flipslop.fold

/**
 * Decides from public display information alone whether logical display 0 is
 * currently backed by the cover panel.
 *
 * On flip phones such as arcfox the display layout swaps which physical panel
 * backs display 0 (cover while folded, inner while open), so the panel
 * resolution tells us the fold state. A cover panel is close to square and is
 * smaller than the inner panel; the inner panel of a flip is tall and narrow.
 */
class CoverDisplayClassifier(
    private val maxCoverAspect: Float = 1.5f,
    private val closedHingeAngle: Float = 30f,
    private val openHingeAngle: Float = 150f,
) {
    fun classify(
        current: PanelSize,
        knownPanels: Collection<PanelSize>,
        hingeAngle: Float? = null,
    ): FoldState {
        val cur = current.normalized()
        val all = (knownPanels.map { it.normalized() } + cur).toSet()
        val largest = all.maxBy { it.area }
        val coverShaped = cur.aspect <= maxCoverAspect

        val base = when {
            !coverShaped -> FoldState.OPEN
            // Only one panel seen so far: trust the hinge if there is one.
            all.size == 1 -> when {
                hingeAngle == null -> FoldState.FOLDED
                hingeAngle < closedHingeAngle -> FoldState.FOLDED
                else -> FoldState.OPEN
            }
            cur.area < largest.area -> FoldState.FOLDED
            // Square-ish but the biggest panel: the inner screen of a book-style foldable.
            else -> FoldState.OPEN
        }

        if (base == FoldState.OPEN && hingeAngle != null &&
            hingeAngle >= closedHingeAngle && hingeAngle < openHingeAngle
        ) {
            return FoldState.HALF_OPEN
        }
        return base
    }
}
