package com.tuki.flipslop.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldContinuationTest {

    private fun shows(
        foreground: String?,
        behavior: FoldBehavior? = FoldBehavior.ALWAYS,
        keyguardLocked: Boolean = false,
    ) = FoldContinuation.showsCover(behavior, keyguardLocked, foreground, home = "launcher", self = "flipslop")

    @Test
    fun `system values map to the three options`() {
        assertEquals(FoldBehavior.ALWAYS, FoldContinuation.parse("stay_awake_on_fold_key"))
        assertEquals(FoldBehavior.SWIPE_UP, FoldContinuation.parse("selective_stay_awake_key"))
        assertEquals(FoldBehavior.NEVER, FoldContinuation.parse("sleep_on_fold_key"))
    }

    @Test
    fun `unset and unknown values fall back to swipe up`() {
        assertEquals(FoldBehavior.SWIPE_UP, FoldContinuation.parse(null))
        assertEquals(FoldBehavior.SWIPE_UP, FoldContinuation.parse("something_else"))
    }

    @Test
    fun `an app in front stays with always and swipe up`() {
        assertFalse(shows("browser", FoldBehavior.ALWAYS))
        assertFalse(shows("browser", FoldBehavior.SWIPE_UP))
    }

    @Test
    fun `the home screen gives way to the cover`() {
        assertTrue(shows("launcher", FoldBehavior.ALWAYS))
        assertTrue(shows("launcher", FoldBehavior.SWIPE_UP))
    }

    @Test
    fun `flipslop itself gives way to the cover`() {
        assertTrue(shows("flipslop"))
    }

    @Test
    fun `the lock screen gives way to the cover`() {
        assertTrue(shows("browser", keyguardLocked = true))
    }

    @Test
    fun `an unknown foreground app gives way to the cover`() {
        assertTrue(shows(null))
    }

    @Test
    fun `never leaves the sleeping phone alone`() {
        assertFalse(shows("launcher", FoldBehavior.NEVER))
        assertFalse(shows("browser", FoldBehavior.NEVER, keyguardLocked = true))
    }

    @Test
    fun `devices without the option always show the cover`() {
        assertTrue(shows("browser", behavior = null))
    }
}
