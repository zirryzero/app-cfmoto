package dev.zanderp.opencfmoto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashClockTest {
    @Test
    fun nameLooksLike800nkDash() {
        assertTrue(DashClock.nameLooksLikeDash("CFMOTO-800NK"))
        assertTrue(DashClock.nameLooksLikeDash("CFDL26"))
        assertFalse(DashClock.nameLooksLikeDash("WH-1000XM5"))
        assertFalse(DashClock.nameLooksLikeDash("Galaxy Watch"))
        assertFalse(DashClock.nameLooksLikeDash(null))
    }
}
