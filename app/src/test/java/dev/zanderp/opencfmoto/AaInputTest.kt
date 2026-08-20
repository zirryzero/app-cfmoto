package dev.zanderp.opencfmoto

import dev.zanderp.opencfmoto.aa.AaInput
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AaInputTest {

    @Test
    fun inputTimestampUsesNanoseconds() {
        assertEquals(12_345_000_000L, AaInput.timestampNanosFromElapsedMillis(12_345L))
    }

    @Test
    fun regularKeyUsesPressThenRelease() {
        assertArrayEquals(booleanArrayOf(true, false), AaInput.keyTransitions(AaInput.KEY_ENTER))
    }

    @Test
    fun assistantUsesAndroidAutoVoiceSequence() {
        assertArrayEquals(booleanArrayOf(false, true), AaInput.keyTransitions(AaInput.KEY_ASSISTANT))
    }
}
