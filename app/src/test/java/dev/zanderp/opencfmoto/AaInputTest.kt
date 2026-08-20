package dev.zanderp.opencfmoto

import dev.zanderp.opencfmoto.aa.AaInput
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AaInputTest {

    @Test
    fun inputTimestampUsesMicroseconds() {
        assertEquals(12_345_678L, AaInput.timestampMicrosFromElapsedNanos(12_345_678_999L))
    }

    @Test
    fun regularKeyUsesPressThenRelease() {
        assertArrayEquals(booleanArrayOf(true, false), AaInput.keyTransitions(AaInput.KEY_ENTER))
    }

    @Test
    fun assistantUsesCompletePressThenRelease() {
        assertArrayEquals(booleanArrayOf(true, false), AaInput.keyTransitions(AaInput.KEY_ASSISTANT))
    }

    @Test
    fun touchscreenProfileDoesNotAdvertiseRotaryController() {
        val keys = AaInput.supportedKeycodes(touchscreen = true).toSet()

        assertFalse(keys.contains(AaInput.KEY_SCROLL_WHEEL))
        assertTrue(keys.contains(AaInput.KEY_ASSISTANT))
        assertTrue(keys.contains(AaInput.KEY_ENTER))
    }

    @Test
    fun nonTouchProfileStillAdvertisesRotaryController() {
        assertTrue(
            AaInput.supportedKeycodes(touchscreen = false).contains(AaInput.KEY_SCROLL_WHEEL)
        )
    }
}
