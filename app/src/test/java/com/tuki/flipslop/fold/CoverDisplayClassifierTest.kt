package com.tuki.flipslop.fold

import com.tuki.flipslop.core.DeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fixtures come from the LineageOS arcfox tree (razr 50 Ultra):
 * cover "Outer Display" 1080x1272, inner panel 1080x2640.
 */
class CoverDisplayClassifierTest {

    private val classifier = CoverDisplayClassifier()
    private val cover = PanelSize(1080, 1272)
    private val inner = PanelSize(1080, 2640)

    @Test
    fun `arcfox cover panel is folded`() {
        assertEquals(FoldState.FOLDED, classifier.classify(cover, setOf(cover, inner)))
    }

    @Test
    fun `arcfox inner panel is open`() {
        assertEquals(FoldState.OPEN, classifier.classify(inner, setOf(cover, inner)))
    }

    @Test
    fun `rotation does not matter`() {
        assertEquals(FoldState.FOLDED, classifier.classify(PanelSize(1272, 1080), setOf(inner)))
        assertEquals(FoldState.OPEN, classifier.classify(PanelSize(2640, 1080), setOf(cover)))
    }

    @Test
    fun `first panel ever seen decides by shape`() {
        assertEquals(FoldState.FOLDED, classifier.classify(cover, emptySet()))
        assertEquals(FoldState.OPEN, classifier.classify(inner, emptySet()))
    }

    @Test
    fun `hinge breaks the tie when only a square panel is known`() {
        assertEquals(FoldState.FOLDED, classifier.classify(cover, emptySet(), hingeAngle = 2f))
        assertEquals(FoldState.OPEN, classifier.classify(cover, emptySet(), hingeAngle = 178f))
    }

    @Test
    fun `half open is reported from the hinge on the inner panel`() {
        assertEquals(FoldState.HALF_OPEN, classifier.classify(inner, setOf(cover), hingeAngle = 95f))
        assertEquals(FoldState.OPEN, classifier.classify(inner, setOf(cover), hingeAngle = 175f))
    }

    @Test
    fun `book style inner screen is not mistaken for a cover`() {
        val bookInner = PanelSize(2208, 1840)
        val bookCover = PanelSize(1080, 2092)
        assertEquals(FoldState.OPEN, classifier.classify(bookInner, setOf(bookCover, bookInner)))
        assertEquals(FoldState.OPEN, classifier.classify(bookCover, setOf(bookCover, bookInner)))
    }

    @Test
    fun `regular phone is always open`() {
        assertEquals(FoldState.OPEN, classifier.classify(PanelSize(1080, 2400), emptySet()))
    }

    @Test
    fun `arcfox profile matches the device tree`() {
        val p = DeviceProfile.forDevice("arcfox")
        assertEquals(cover, p.coverPanel)
        assertEquals(inner, p.innerPanel)
        assertEquals(143, p.coverBottomInsetDp)
        assertEquals(FoldState.FOLDED, classifier.classify(cover, p.knownPanels))
    }

    @Test
    fun `panel size round trips through prefs format`() {
        assertEquals(cover, PanelSize.parse(cover.toString()))
        assertNull(PanelSize.parse("garbage"))
        assertNull(PanelSize.parse("0x10"))
    }
}
