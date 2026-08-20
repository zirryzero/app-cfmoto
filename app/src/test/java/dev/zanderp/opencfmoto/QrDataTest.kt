package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrDataTest {
    @Test
    fun parses800nkAdvancedCarbitQr() {
        val raw =
            "http://www.carbit.com.cn/downsdk/657/658/_sdk?modelid=37426&sn=Vasr&action=1" +
                "&ssid=CFMOTO4288&pwd=e278450c&uid=abc"
        val qr = QrData.parse(raw)!!

        assertEquals("CFMOTO4288", qr.ssid)
        assertEquals("e278450c", qr.pwd)
        assertEquals(QrData.MODEL_ID, qr.modelId)
        assertEquals("800NK Advanced", qr.name)
        assertTrue(qr.supportsAp)
    }

    @Test
    fun accepts800nkQrWithoutExplicitModelId() {
        val qr = QrData.parse("https://carbit.example/pair?ssid=CFMOTO800NK&pwd=secret&action=1")
        assertEquals("CFMOTO800NK", qr?.ssid)
    }

    @Test
    fun rejectsQrForAnotherDeclaredModel() {
        val raw = "https://carbit.example/pair?modelid=99999&ssid=OTHER&pwd=secret&action=1"
        assertNull(QrData.parse(raw))
        assertTrue(QrData.parseFailureHint(raw)!!.contains("800NK Advanced"))
    }

    @Test
    fun rejectsQrWithoutSoftApCredentials() {
        assertNull(QrData.parse("https://carbit.example/pair?modelid=37426&action=1"))
        assertNull(QrData.parse("CARBITDC0D3024876D"))
    }
}
