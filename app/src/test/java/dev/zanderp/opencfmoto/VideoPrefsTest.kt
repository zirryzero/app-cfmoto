package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPrefsTest {

    @Test
    fun optimizedAreaAligns800NkPanelAndAppliesTopGuard() {
        val aligned = Cfdl26NkTouchProfile.roundCaptureDimensions(720, 712)

        assertEquals(720 to 704, aligned)
        assertEquals(720 to 682, VideoPrefs.contentArea(aligned, 22, 0, 0, 0))
    }

    @Test
    fun optimizedAreaHonorsAllUserMargins() {
        val area = VideoPrefs.contentArea(720 to 704, top = 20, bottom = 10, left = 8, right = 12)

        assertEquals(700 to 674, area)
    }

    @Test
    fun aaMarginsReflowStandardStreamToOptimizedArea() {
        val spec = AaVideoSpec(AaResolution.PORTRAIT_720x1280, dpi = 160)
        val margins = AaMargins.forAspect(spec, targetW = 720, targetH = 682)

        assertEquals(AaMargins(marginW = 0, marginH = 598), margins)
        assertEquals(720, spec.width - margins.marginW)
        assertEquals(682, spec.height - margins.marginH)
    }
}
