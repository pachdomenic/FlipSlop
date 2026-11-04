package com.tuki.flipslop.toggles

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessCurveTest {
    @Test
    fun `ends map to the setting range`() {
        assertEquals(1, TogglesController.sliderToRaw(0f))
        assertEquals(255, TogglesController.sliderToRaw(1f))
    }

    @Test
    fun `round trip stays close`() {
        for (raw in listOf(1, 10, 64, 128, 200, 255)) {
            val back = TogglesController.sliderToRaw(TogglesController.rawToSlider(raw))
            assertEquals(raw.toDouble(), back.toDouble(), 1.0)
        }
    }
}
