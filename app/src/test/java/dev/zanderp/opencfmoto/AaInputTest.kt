package dev.zanderp.opencfmoto

import dev.zanderp.opencfmoto.aa.AaInput
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
}
