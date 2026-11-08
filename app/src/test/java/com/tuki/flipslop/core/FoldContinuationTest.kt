package com.tuki.flipslop.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldContinuationTest {

    private fun keeps(foreground: String?, keyguardLocked: Boolean = false) =
        FoldContinuation.keepsApp(keyguardLocked, foreground, home = "launcher", self = "flipslop")

    @Test
    fun `an app in front stays`() {
        assertTrue(keeps("browser"))
    }

    @Test
    fun `the home screen gives way to the cover`() {
        assertFalse(keeps("launcher"))
    }

    @Test
    fun `flipslop itself gives way to the cover`() {
        assertFalse(keeps("flipslop"))
    }

    @Test
    fun `the lock screen gives way to the cover`() {
        assertFalse(keeps("browser", keyguardLocked = true))
    }

    @Test
    fun `an unknown foreground app gives way to the cover`() {
        assertFalse(keeps(null))
    }
}
