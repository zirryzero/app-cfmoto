package dev.zanderp.opencfmoto

import dev.zanderp.opencfmoto.aa.AaMicrophone
import dev.zanderp.opencfmoto.aa.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AaMicrophoneTest {

    @Test
    fun mediaTimestampMatchesAndroidHeadunitMilliseconds() {
        assertEquals(
            12_345L,
            AaMicrophone.timestampMillisFromElapsedNanos(12_345_678_999L),
        )
    }

    @Test
    fun mediaFramePlacesTimestampImmediatelyBeforePcm() {
        val message = AaMicrophone.buildMicData(shortArrayOf(0x1234, -2), 2, 0x0102030405060708L)

        assertEquals(16, message.size)
        assertEquals(Channel.ID_MIC.toByte(), message.data[0])
        assertEquals(0x0b.toByte(), message.data[1])
        assertEquals(
            listOf<Byte>(1, 2, 3, 4, 5, 6, 7, 8),
            message.data.slice(4..11),
        )
        assertEquals(listOf<Byte>(0x34, 0x12, -2, -1), message.data.slice(12..15))
    }

    @Test
    fun cfmotoHandsFreeRouteIsNotTreatedAsRiderHeadset() {
        assertTrue(AaMicrophone.isBikeHandsFreeRoute("CFMOTO_BT"))
        assertFalse(AaMicrophone.isBikeHandsFreeRoute("Cardo FREECOM"))
    }

    @Test
    fun signalPeakHandlesFullNegativeSample() {
        assertEquals(32768, AaMicrophone.signalPeak(shortArrayOf(0, -15, Short.MIN_VALUE), 3))
    }
}
