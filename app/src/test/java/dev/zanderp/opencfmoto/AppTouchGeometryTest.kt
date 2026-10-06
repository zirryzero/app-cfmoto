package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppTouchGeometryTest {
    @Test
    fun scalesCaptureCoordinatesToCurrentDisplay() {
        val point = AppTouchGeometry.scalePoint(360, 640, 720, 1280, 1080, 1920)
        assertEquals(540f, point!!.first, 0.01f)
        assertEquals(960f, point.second, 0.01f)
    }

    @Test
    fun clampsEdgesInsideTargetDisplay() {
        val point = AppTouchGeometry.scalePoint(900, -50, 720, 1280, 1080, 1920)
        assertEquals(1078.5f, point!!.first, 0.01f)
        assertEquals(0f, point.second, 0.01f)
    }

    @Test
    fun rejectsUnknownDimensions() {
        assertNull(AppTouchGeometry.scalePoint(1, 1, 0, 1280, 1080, 1920))
    }
}
